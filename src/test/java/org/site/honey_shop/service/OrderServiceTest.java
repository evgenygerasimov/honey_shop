package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private final UUID orderId = UUID.randomUUID();

    private Order order;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .orderId(orderId)
                .firstName("Ivan")
                .lastName("Ivanov")
                .customerPhone("123")
                .customerEmail("test@test.com")
                .deliveryAmount(BigDecimal.TEN)
                .productAmount(BigDecimal.ONE)
                .totalOrderAmount(BigDecimal.valueOf(11))
                .deliveryType("PVZ")
                .payment(Payment.builder().build())
                .build();

        orderDTO = new OrderDTO();
    }

    @Test
    void testFindOrderDTOById_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(shopMapper.toDto(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.findOrderDTOById(orderId);

        assertThat(result).isEqualTo(orderDTO);
        verify(orderRepository).findById(orderId);
        verify(shopMapper).toDto(order);
    }

    @Test
    void testFindOrderDTOById_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findOrderDTOById(orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void testFindById_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.findById(orderId);

        assertThat(result).isEqualTo(order);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void testFindById_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void testFindAll_ReturnsPageOfDTOs() {
        Pageable pageable = mock(Pageable.class);

        @SuppressWarnings("unchecked")
        Page<Order> orderPage = mock(Page.class);

        Page<OrderDTO> emptyDtoPage = Page.empty();

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderPage.map(any(Function.class))).thenReturn(emptyDtoPage);

        Page<OrderDTO> result = orderService.findAll(pageable);

        assertThat(result).isEmpty();
        verify(orderRepository).findAll(pageable);
        verify(orderPage).map(any(Function.class));
    }


    @Test
    void testSave_Success() {
        // Подготовим JSON с одним товаром
        UUID productId = UUID.randomUUID();

        String orderItemsJson = "[{\"order\":null,\"product\":{\"productId\":\"" + productId + "\"},\"quantity\":1,\"pricePerUnit\":3000}]";

        // Мок продукта
        Product product = Product.builder().productId(productId).build();

        // Мок поведения репозитория продукта и сервиса для обновления остатков
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(productService).updateStockForReduction(product, 1);
        when(productRepository.save(product)).thenReturn(product);

        // Мок сохранения заказа
        Order savedOrder = Order.builder().orderId(orderId).build();
        when(orderRepository.save(any())).thenReturn(savedOrder);

        // Вызов метода
        Order result = orderService.save(order, orderItemsJson);

        // Проверки
        assertThat(result.getOrderId()).isEqualTo(orderId);

        verify(productRepository).findById(productId);
        verify(productService).updateStockForReduction(product, 1);
        verify(productRepository).save(product);
        verify(orderRepository).save(orderCaptor.capture());
        verify(orderEventPublisher).publishOrderCreatedEvent(anyString());

        // Проверяем, что внутри orderCaptor передан заказ с одним orderItem
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getOrderItems()).hasSize(1);
        assertThat(capturedOrder.getOrderItems().get(0).getProduct().getProductId()).isEqualTo(productId);
    }

    @Test
    void testSave_InvalidJson() {
        String badJson = "invalid-json";

        assertThatThrownBy(() -> orderService.save(order, badJson))
                .isInstanceOf(OrderCreateException.class)
                .hasMessageContaining("Ошибка при разборе JSON");
    }

    @Test
    void testSave_ProductNotFound_Throws() {
        UUID productId = UUID.randomUUID();
        String json = "[{\"order\":null,\"product\":{\"productId\":\"" + productId + "\"},\"quantity\":1,\"pricePerUnit\":3000}]";

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.save(order, json))
                .isInstanceOf(OrderCreateException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void testUpdate_Success() {
        Order oldOrder = Order.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.PENDING)
                .payment(Payment.builder().paymentStatus(PaymentStatus.PENDING).build())
                .build();

        Order newOrder = Order.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.PAID)
                .payment(Payment.builder().paymentStatus(PaymentStatus.SUCCESS).build())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(oldOrder));
        when(orderRepository.save(any())).thenReturn(newOrder);

        Order result = orderService.update(newOrder);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdate_OrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        Order someOrder = Order.builder().orderId(orderId).build();

        assertThatThrownBy(() -> orderService.update(someOrder))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void testDelete() {
        orderService.delete(orderId);

        verify(orderRepository).deleteById(orderId);
    }

    @Test
    void testUpdateOrderStatus_Success() {
        Order existing = Order.builder().orderId(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenReturn(existing);

        Order updated = orderService.updateOrderStatus(existing, OrderStatus.SHIPPED);

        assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void testUpdateOrderStatus_OrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        Order order = Order.builder().orderId(orderId).build();

        assertThatThrownBy(() -> orderService.updateOrderStatus(order, OrderStatus.SHIPPED))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void testUpdateOrderPaymentStatus_Success() {
        Order existing = Order.builder().orderId(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenReturn(existing);

        Order updated = orderService.updateOrderPaymentStatus(existing, PaymentStatus.SUCCESS);

        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdateOrderPaymentStatus_OrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        Order order = Order.builder().orderId(orderId).build();

        assertThatThrownBy(() -> orderService.updateOrderPaymentStatus(order, PaymentStatus.SUCCESS))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void testCancelExpiredOrders() {
        Order order1 = Order.builder()
                .orderId(orderId)
                .orderStatus(OrderStatus.PENDING)
                .createDate(LocalDateTime.now().minusMinutes(11))
                .payment(Payment.builder().paymentId(UUID.randomUUID()).paymentStatus(PaymentStatus.PENDING).build())
                .orderItems(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .product(Product.builder().productId(UUID.randomUUID()).build())
                .quantity(2)
                .build();

        order1.getOrderItems().add(item);

        when(orderRepository.findByOrderStatusAndCreateDateBefore(
                eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(order1));

        when(orderRepository.findById(order1.getOrderId())).thenReturn(Optional.of(order1));

        when(paymentService.findById(order1.getPayment().getPaymentId())).thenReturn(order1.getPayment());
        doNothing().when(paymentService).update(order1.getPayment());

        when(productRepository.findById(item.getProduct().getProductId())).thenReturn(Optional.of(item.getProduct()));
        doNothing().when(productService).updateStockForAddition(item.getProduct(), item.getQuantity());

        when(orderRepository.save(any())).thenReturn(order1);

        orderService.cancelExpiredOrders();

        assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order1.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(paymentService).update(order1.getPayment());
        verify(productService).updateStockForAddition(item.getProduct(), item.getQuantity());
        verify(orderRepository).save(order1);
    }

}
