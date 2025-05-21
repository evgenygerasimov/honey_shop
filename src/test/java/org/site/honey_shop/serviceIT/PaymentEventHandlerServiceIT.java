package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.kafka.OrderInfoEventPublisher;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.PaymentRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentEventHandlerServiceIT extends TestContainerConfig {

    @Autowired
    private PaymentEventHandlerService paymentEventHandlerService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @MockitoBean
    private OrderInfoEventPublisher orderInfoEventPublisher;

    @BeforeEach
    void clearDb() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        paymentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testHandlePaymentSucceeded_success() {
        Product product = createSampleProduct();
        productRepository.save(product);
        Order order = createSampleOrder();
        OrderItem orderItem = createSampleOrderItem(order, product, 1);
        order.setOrderItems(List.of(orderItem));
        orderRepository.save(order);

        Payment payment = createSamplePayment(order);
        paymentRepository.save(payment);

        order.setPayment(payment);
        orderRepository.save(order);

        Map<String, Object> paymentData = createPaymentData("payment.succeeded", order.getOrderId(), true);

        paymentEventHandlerService.handlePaymentSucceeded(paymentData);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);

        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isLessThan(product.getStockQuantity());
    }


    @Test
    void testHandlePaymentCanceled_success() {
        Product product = createSampleProduct();
        productRepository.save(product);
        Order order = createSampleOrder();
        OrderItem orderItem = createSampleOrderItem(order, product, 1);
        order.setOrderItems(List.of(orderItem));
        orderRepository.save(order);

        Payment payment = createSamplePayment(order);
        paymentRepository.save(payment);

        order.setPayment(payment);
        orderRepository.save(order);

        Map<String, Object> paymentData = buildPaymentData(order, "payment.canceled");


        paymentEventHandlerService.handlePaymentCanceled(paymentData);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void testHandleRefundSucceeded_success() {
        Product product = createSampleProduct();
        productRepository.save(product);
        Order order = createSampleOrder();
        OrderItem orderItem = createSampleOrderItem(order, product, 1);
        order.setOrderItems(List.of(orderItem));
        orderRepository.save(order);

        Payment payment = createSamplePayment(order);
        paymentRepository.save(payment);

        order.setPayment(payment);
        orderRepository.save(order);

        Map<String, Object> paymentData = buildPaymentData(order, "refunded");

        paymentEventHandlerService.handleRefundSucceeded(paymentData);

        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isGreaterThan(product.getStockQuantity());
    }

    private Map<String, Object> buildPaymentData(Order order, String status) {
        return buildPaymentData(order.getOrderId(), status);
    }

    private Map<String, Object> buildPaymentData(UUID orderId, String status) {
        Map<String, Object> paymentData = new HashMap<>();
        Map<String, Object> paymentObject = new HashMap<>();
        paymentObject.put("description", "# " + orderId.toString());
        paymentData.put("object", paymentObject);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", UUID.randomUUID().toString());
        paymentData.put("metadata", metadata);

        return paymentData;

    }

    private OrderItem createSampleOrderItem(Order order, Product product, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setOrder(order);
        orderItem.setPricePerUnit(product.getPrice());
        return orderItem;
    }

    private Order createSampleOrder() {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(10.99))
                .deliveryAmount(BigDecimal.valueOf(0))
                .totalOrderAmount(BigDecimal.valueOf(10.99))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryType("PVZ")
                .build();
        return order;
    }

    private Payment createSamplePayment(Order order) {
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .amount(new BigDecimal("10.99"))
                .paymentStatus(PaymentStatus.PENDING)
                .build());
        return payment;
    }

    private Product createSampleProduct() {
        Category honeyCategory = categoryService.save("Мёд");
        Product product = Product.builder()
                .name("Продукт 1")
                .description("Описание продукта")
                .shortDescription("Кратко")
                .price(new BigDecimal("10.99"))
                .length(10.0)
                .width(5.0)
                .height(3.0)
                .weight(0.5)
                .images(new java.util.ArrayList<>())
                .stockQuantity(100)
                .category(honeyCategory)
                .build();
        return product;
    }

    private Map<String, Object> createPaymentData(java.lang.String event, UUID orderId, boolean withMetadata) {
        Map<java.lang.String, java.lang.Object> paymentObject = new HashMap<>();
        paymentObject.put("description", "Order #" + orderId);

        if (withMetadata) {
            Map<java.lang.String, java.lang.Object> metadata = new HashMap<>();
            metadata.put("sessionId", "abc-123");
            paymentObject.put("metadata", metadata);
        }

        Map<java.lang.String, java.lang.Object> paymentData = new HashMap<>();
        paymentData.put("event", event);
        paymentData.put("object", paymentObject);
        return paymentData;
    }
}
