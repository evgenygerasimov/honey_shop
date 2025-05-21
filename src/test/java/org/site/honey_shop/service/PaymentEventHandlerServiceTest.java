package org.site.honey_shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.kafka.OrderInfoEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerServiceTest {

    @Mock
    private PaymentCashService paymentCashService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private OrderInfoEventPublisher orderInfoEventPublisher;

    @InjectMocks
    private PaymentEventHandlerService service;

    private UUID orderUuid;
    private Order order;
    private Payment payment;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderUuid = UUID.randomUUID();
        product = new Product();
        product.setName("Test Product");
        product.setStockQuantity(10);

        orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);

        payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());

        order = new Order();
        order.setOrderId(orderUuid);
        order.setPayment(payment);
        order.setOrderItems(List.of(orderItem));
    }

    private Map<String, Object> createPaymentData(String event, UUID orderId, boolean withMetadata) {
        Map<String, Object> paymentObject = new HashMap<>();
        paymentObject.put("description", "Order #" + orderId);

        if (withMetadata) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sessionId", "abc-123");
            paymentObject.put("metadata", metadata);
        }

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("event", event);
        paymentData.put("object", paymentObject);
        return paymentData;
    }

    @Test
    void testHandlePaymentSucceeded_successfulFlow() {
        Map<String, Object> paymentData = createPaymentData("payment.succeeded", orderUuid, true);

        when(orderService.findById(orderUuid)).thenReturn(order);
        when(paymentService.findById(payment.getPaymentId())).thenReturn(payment);

        service.handlePaymentSucceeded(paymentData);

        verify(paymentCashService).savePaymentSuccess("abc-123", true);
        verify(orderService).findById(orderUuid);
        verify(paymentService).findById(payment.getPaymentId());
        verify(paymentService).update(payment);
        verify(orderService).update(order);
        verify(productService).updateStockForReduction(product, 2);

        assertEquals(OrderStatus.PAID, order.getOrderStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
    }

    @Test
    void testHandlePaymentSucceeded_missingMetadata_shouldThrow() {
        Map<String, Object> paymentData = createPaymentData("payment.succeeded", orderUuid, false);

        Exception ex = assertThrows(NullPointerException.class, () -> service.handlePaymentSucceeded(paymentData));
        assertTrue(ex.getMessage().contains("Metadata is null"));
    }


    @Test
    void testHandlePaymentCanceled_successfulFlow() {
        Map<String, Object> paymentData = createPaymentData("payment.canceled", orderUuid, false);

        when(orderService.findById(orderUuid)).thenReturn(order);
        when(paymentService.findById(payment.getPaymentId())).thenReturn(payment);

        service.handlePaymentCanceled(paymentData);

        verify(orderService).update(order);
        verify(paymentService).update(payment);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
    }


    @Test
    void testHandleRefundSucceeded_successfulFlow() {
        Map<String, Object> paymentData = createPaymentData("refund.succeeded", orderUuid, false);

        when(orderService.findById(orderUuid)).thenReturn(order);
        when(paymentService.findById(payment.getPaymentId())).thenReturn(payment);

        service.handleRefundSucceeded(paymentData);

        verify(paymentService).update(payment);
        verify(orderService).update(order);
        verify(productService).updateStockForAddition(product, 2);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());
    }
}
