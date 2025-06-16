package org.site.honey_shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopMapper shopMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final PaymentService paymentService;

    public OrderDTO findOrderDTOById(UUID orderId) {
        log.info("Find orderDTO: {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        return shopMapper.toDto(order);
    }

    public Order findById(UUID orderId) {
        log.info("Find order: {}", orderId);
        return orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
    }

    public Page<OrderDTO> findAll(Pageable pageable) {

        log.info("Find all orders sorted by createDate descending");
        return orderRepository.findAll(pageable).map(shopMapper::toDto);
    }

    @Transactional
    public Order save(Order order, String orderItemsJson) {
        order = Order.builder()
                .firstName(order.getFirstName())
                .lastName(order.getLastName())
                .middleName(order.getMiddleName())
                .customerEmail(order.getCustomerEmail())
                .deliveryAddress(order.getDeliveryAddress())
                .customerPhone(order.getCustomerPhone())
                .productAmount(order.getProductAmount())
                .deliveryAmount(order.getDeliveryAmount())
                .totalOrderAmount(order.getTotalOrderAmount())
                .deliveryType(order.getDeliveryType())
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .payment(order.getPayment())
                .build();
        Payment payment = Payment.builder()
                .amount(order.getTotalOrderAmount())
                .paymentStatus(PaymentStatus.PENDING)
                .order(order)
                .build();
        order.setPayment(payment);

        ObjectMapper objectMapper = new ObjectMapper();
        List<OrderItem> orderItems;

        try {
            log.info("Attempt to parse order items json: {}", orderItemsJson);
            orderItems = objectMapper.readValue(orderItemsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Error while parsing JSON: {}", e.getMessage());
            throw new OrderCreateException("Ошибка при разборе JSON с товарами");
        }

        for (OrderItem orderItem : orderItems) {
            UUID productId = orderItem.getProduct().getProductId();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new OrderCreateException("Товар с id " + productId + " не найден"));

            productService.updateStockForReduction(product, orderItem.getQuantity());
            productRepository.save(product);

            orderItem.setProduct(product);
            orderItem.setOrder(order);
        }

        order.setOrderItems(orderItems);
        order = orderRepository.save(order);
        orderEventPublisher.publishOrderCreatedEvent(" на сумму " + order.getTotalOrderAmount() + " руб.");
        log.info("Order saved: {}", order.getOrderId());
        return order;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(10);
        List<Order> expiredOrders = orderRepository.findByOrderStatusAndCreateDateBefore(OrderStatus.PENDING, expirationThreshold);

        for (Order order : expiredOrders) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            Payment payment = paymentService.findById(order.getPayment().getPaymentId());
            payment.setPaymentStatus(PaymentStatus.FAILED);

            for (OrderItem item : order.getOrderItems()) {
                Product product = productRepository.findById(item.getProduct().getProductId())
                        .orElseThrow(() -> new IllegalStateException("Product not found"));

                productService.updateStockForAddition(product, item.getQuantity());
            }

            paymentService.update(payment);
            log.info("Order cancelled: {}", order.getOrderId());
            update(order);
        }
    }


    @Transactional
    public Order update(Order order) {
        Order existingOrder = orderRepository.findById(order.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        existingOrder.setOrderStatus(order.getOrderStatus());
        existingOrder.setPaymentStatus(order.getPayment().getPaymentStatus());
        log.info("Attempt to update order: {}", existingOrder);
        return orderRepository.save(existingOrder);
    }

    public void delete(UUID orderId) {
        orderRepository.deleteById(orderId);
        log.info("Order deleted: {}", orderId);
    }

    public Order updateOrderStatus(Order order, OrderStatus orderStatus) {
        Order existingOrder = orderRepository.findById(order.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        existingOrder.setOrderStatus(orderStatus);
        log.info("Attempt to update order status for order: {}", existingOrder);
        return orderRepository.save(existingOrder);
    }

    public Order updateOrderPaymentStatus(Order order, PaymentStatus orderPaymentStatus) {
        Order existingOrder = orderRepository.findById(order.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        existingOrder.setPaymentStatus(orderPaymentStatus);
        log.info("Attempt to update order payment status for order: {}", existingOrder);
        return orderRepository.save(existingOrder);
    }
}
