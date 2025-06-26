package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.kafka.OrderInfoEventPublisher;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentEventHandlerServiceIT extends TestContainerConfig {

    @Autowired
    private PaymentEventHandlerService paymentEventHandlerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @MockitoBean
    private OrderInfoEventPublisher orderInfoEventPublisher;

    private Order order;
    private Product product;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CategoryRepository categoryRepository;

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

        order = createSampleOrder();
        order = orderService.save(order, buildOrderItemsJson(product.getProductId(), 2, new BigDecimal("100.00")));
    }

    @Test
    void handlePaymentSucceeded_updatesOrderAndPaymentStatusAndPublishesEvents() {
        Map<String, Object> paymentData = createPaymentData(order.getOrderId(), "session123");

        paymentEventHandlerService.handlePaymentSucceeded(paymentData);

        Order updatedOrder = orderService.findById(order.getOrderId());
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

        Payment payment = paymentService.findById(updatedOrder.getPayment().getPaymentId());
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(orderEventPublisher).publishOrderCreatedEvent(contains("успешно оплачен"));
        verify(orderInfoEventPublisher).publishOrderInfoEvent(any(Order.class));
    }

    @Test
    void handlePaymentSucceeded_withMissingMetadata_throwsException() {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("object", new HashMap<>()); // пустой object без metadata

        assertThatThrownBy(() -> paymentEventHandlerService.handlePaymentSucceeded(paymentData))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Payment metadata is missing");
    }

    @Test
    void handlePaymentCanceled_updatesOrderAndPaymentStatusAndPublishesEvent() {
        Map<String, Object> paymentData = createPaymentData(order.getOrderId(), null);

        paymentEventHandlerService.handlePaymentCanceled(paymentData);

        Order updatedOrder = orderService.findById(order.getOrderId());
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Payment payment = paymentService.findById(updatedOrder.getPayment().getPaymentId());
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(orderEventPublisher).publishOrderCreatedEvent(contains("был отменен"));
    }

    @Test
    @Transactional
    void handleRefundSucceeded_updatesStatusesAndRestoresStock() {
        Map<String, Object> paymentData = createPaymentData(order.getOrderId(), null);

        productService.updateStockForReduction(product, 2);

        paymentEventHandlerService.handleRefundSucceeded(paymentData);

        Order updatedOrder = orderService.findById(order.getOrderId());
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Payment payment = paymentService.findById(updatedOrder.getPayment().getPaymentId());
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

        Product updatedProduct = productService.getProductById(product.getProductId());
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(product.getStockQuantity());
    }

    @Test
    void handlePaymentSucceeded_withInvalidDescription_throwsException() {
        Map<String, Object> paymentData = new HashMap<>();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("description", "invalid_description_without_uuid");
        objectMap.put("metadata", Map.of("sessionId", "sess1"));
        paymentData.put("object", objectMap);

        assertThatThrownBy(() -> paymentEventHandlerService.handlePaymentSucceeded(paymentData))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Order UUID is missing");
    }

    private Order createSampleOrder() {
        return Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(200))
                .deliveryAmount(BigDecimal.ZERO)
                .totalOrderAmount(BigDecimal.valueOf(200))
                .deliveryType("PVZ")
                .build();
    }

    private String buildOrderItemsJson(UUID productId, int quantity, BigDecimal price) {
        return "[{\"order\":{},\"product\":{\"productId\":\"" + productId + "\"},\"quantity\":" + quantity + ",\"pricePerUnit\":" + price + "}]";
    }

    private Map<String, Object> createPaymentData(UUID orderId, String sessionId) {
        Map<String, Object> metadata = sessionId != null ? Map.of("sessionId", sessionId) : null;
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("description", "Оплата заказа #" + orderId);
        if (metadata != null) {
            objectMap.put("metadata", metadata);
        }
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("object", objectMap);
        return paymentData;
    }
}
