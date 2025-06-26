package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.OrderService;
import org.site.honey_shop.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIT extends TestContainerConfig {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @MockitoBean
    private PaymentService paymentService;

    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        MockMultipartFile image = new MockMultipartFile(
                "image", "honey.jpg", "image/jpeg", "some-image-data".getBytes());

        Category category = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        Category savedCategory = categoryService.saveCategoryWithImage(category, image);

         product = Product.builder()

                .name("Продукт 1")
                .description("Описание продукта")
                .shortDescription("Кратко")
                .price(new BigDecimal("10.00"))
                .length(10.0)
                .width(5.0)
                .height(3.0)
                .weight(0.5)
                .images(List.of())
                .category(savedCategory)
                .stockQuantity(100)
                .showInShowcase(true)
                .showcaseOrder(1)
                .build();

        productRepository.save(product);
    }

    @Test
    void testSaveOrder_withValidOrderAndItems_savesSuccessfully() {
        Order order = createSampleOrder();
        String stringUUID = product.getProductId().toString();

        String json = "[{\"order\":{},\"product\":{\"productId\":\"" + stringUUID + "\"},\"quantity\":1,\"pricePerUnit\":10.99}]";

        Order saved = orderService.save(order, json);

        assertThat(saved.getOrderId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getPayment()).isNotNull();
        assertThat(saved.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderEventPublisher).publishOrderCreatedEvent(any());
    }

    @Test
    void testSaveOrder_withInvalidJson_throwsException() {
        Order order = createSampleOrder();
        String invalidJson = "not a json";

        assertThatThrownBy(() -> orderService.save(order, invalidJson))
                .isInstanceOf(OrderCreateException.class)
                .hasMessageContaining("Ошибка при разборе JSON");
    }

    @Test
    void testSaveOrder_withNonExistentProduct_throwsException() {
        Order order = createSampleOrder();
        String jsonWithInvalidProduct = "[{\"order\":{},\"product\":{\"productId\":\"00000000-0000-0000-0000-000000000000\"},\"quantity\":1,\"pricePerUnit\":10.99}]";

        assertThatThrownBy(() -> orderService.save(order, jsonWithInvalidProduct))
                .isInstanceOf(OrderCreateException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @Transactional
    void testFindOrderDTOById_returnsCorrectData() {
        Order saved = createAndPersistSimpleOrder();

        OrderDTO dto = orderService.findOrderDTOById(saved.getOrderId());

        assertThat(dto.getOrderId()).isEqualTo(saved.getOrderId());
        assertThat(dto.getFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(dto.getOrderItems()).hasSize(1);
    }

    @Test
    void testFindOrderDTOById_withInvalidId_throwsException() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.findOrderDTOById(randomId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional
    void testFindById_returnsOrder() {
        Order saved = createAndPersistSimpleOrder();

        Order found = orderService.findById(saved.getOrderId());

        assertThat(found).isNotNull();
        assertThat(found.getOrderId()).isEqualTo(saved.getOrderId());
    }

    @Test
    void testFindById_withInvalidId_throwsException() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.findById(randomId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional
    void testFindAll_returnsPage() {
        createAndPersistSimpleOrder();

        Page<OrderDTO> page = orderService.findAll(PageRequest.of(0, 10));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    void testUpdateOrderStatus_updatesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        Order updated = orderService.updateOrderStatus(saved, OrderStatus.SHIPPED);

        assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void testUpdateOrderStatus_withInvalidOrder_throwsException() {
        Order order = createSampleOrder();
        order.setOrderId(UUID.randomUUID());

        assertThatThrownBy(() -> orderService.updateOrderStatus(order, OrderStatus.SHIPPED))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional
    void testUpdateOrderPaymentStatus_updatesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        Order updated = orderService.updateOrderPaymentStatus(saved, PaymentStatus.SUCCESS);

        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdateOrderPaymentStatus_withInvalidOrder_throwsException() {
        Order order = createSampleOrder();
        order.setOrderId(UUID.randomUUID());

        assertThatThrownBy(() -> orderService.updateOrderPaymentStatus(order, PaymentStatus.SUCCESS))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional
    void testDeleteOrder_deletesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        orderService.delete(saved.getOrderId());

        assertThat(orderRepository.findById(saved.getOrderId())).isEmpty();
    }

    @Test
    @Transactional
    void testUpdate_updatesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        saved.setOrderStatus(OrderStatus.SHIPPED);
        saved.getPayment().setPaymentStatus(PaymentStatus.SUCCESS);

        Order updated = orderService.update(saved);

        assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdate_withInvalidOrder_throwsException() {
        Order order = createSampleOrder();
        order.setOrderId(UUID.randomUUID());

        assertThatThrownBy(() -> orderService.update(order))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Transactional
    void testCancelExpiredOrders_cancelsAndRestoresStock() {
        Order expiredOrder = createAndPersistSimpleOrder();
        expiredOrder.setOrderStatus(OrderStatus.PENDING);
        expiredOrder.setCreateDate(LocalDateTime.now().minusMinutes(11));
        orderRepository.save(expiredOrder);

        when(paymentService.findById(any())).thenAnswer(invocation -> {
            Payment payment = expiredOrder.getPayment();
            payment.setPaymentStatus(PaymentStatus.PENDING);
            return payment;
        });
        doAnswer(invocation -> null).when(paymentService).update(any());

        orderService.cancelExpiredOrders();

        Order cancelledOrder = orderRepository.findById(expiredOrder.getOrderId()).orElseThrow();
        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(cancelledOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    private Order createSampleOrder() {
        return Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(10.99))
                .deliveryAmount(BigDecimal.ZERO)
                .totalOrderAmount(BigDecimal.valueOf(10.99))
                .personalDataConsent(true)
                .deliveryType("PVZ")
                .build();
    }

    private Order createAndPersistSimpleOrder() {
        Order order = createSampleOrder();
        String stringUUID = product.getProductId().toString();

        String json = "[{\"order\":{},\"product\":{\"productId\":\"" + stringUUID + "\"},\"quantity\":1,\"pricePerUnit\":10.99}]";

        return orderService.save(order, json);
    }
}
