package org.site.honey_shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.kafka.OrderInfoEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandlerService {

    private final PaymentCashService paymentCashService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PaymentService paymentService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderInfoEventPublisher orderInfoEventPublisher;

    @Transactional
    public void handlePaymentSucceeded(Map<String, Object> paymentData) {
        Map<String, Object> paymentObject = extractObjectMap(paymentData);
        Map<String, Object> metadata = extractMetaData(paymentObject);

        if (metadata != null) {
            String sessionId = (String) metadata.get("sessionId");
            paymentCashService.savePaymentSuccess(sessionId, true);
            log.info("Payment success flag saved for sessionId={}", sessionId);
        } else {
            log.warn("Metadata is null, skipping payment success update");
            throw new NullPointerException("Payment metadata is missing");
        }

        UUID orderUuid = extractOrderUuid(paymentData);
        log.info("Handling payment success for order {}", orderUuid);

        Order order = orderService.findById(orderUuid);
        order.setOrderStatus(OrderStatus.PAID);
        log.info("Order {} marked as PAID", orderUuid);

        Payment payment = paymentService.findById(order.getPayment().getPaymentId());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        log.info("Payment {} marked as SUCCESS", payment.getPaymentId());

        paymentService.update(payment);
        orderService.update(order);

        orderInfoEventPublisher.publishOrderInfoEvent(order);
        orderEventPublisher.publishOrderCreatedEvent(" на сумму " + order.getTotalOrderAmount() + " руб. был успешно оплачен!");

        log.info("Order and payment status(Success) updated successfully for order {}", orderUuid);
    }

    @Transactional
    public void handlePaymentCanceled(Map<String, Object> paymentData) {
        UUID orderUuid = extractOrderUuid(paymentData);
        log.info("Handling payment cancellation for order {}", orderUuid);

        Order order = orderService.findById(orderUuid);
        order.setOrderStatus(OrderStatus.CANCELLED);
        log.info("Order {} marked as CANCELLED", orderUuid);

        Payment payment = paymentService.findById(order.getPayment().getPaymentId());
        payment.setPaymentStatus(PaymentStatus.FAILED);
        log.info("Payment {} marked as FAILED", payment.getPaymentId());

        paymentService.update(payment);
        orderService.update(order);

        orderEventPublisher.publishOrderCreatedEvent(" на сумму " + order.getTotalOrderAmount() + " руб. был отменен!");

        log.info("Order and payment status(Cancelled) updated successfully for order {}", orderUuid);
    }

    @Transactional
    public void handleRefundSucceeded(Map<String, Object> paymentData) {
        UUID orderUuid = extractOrderUuid(paymentData);
        log.info("Handling refund success for order {}", orderUuid);

        Order order = orderService.findById(orderUuid);
        order.setOrderStatus(OrderStatus.CANCELLED);
        log.info("Order {} marked as CANCELLED (after refund)", orderUuid);

        Payment payment = paymentService.findById(order.getPayment().getPaymentId());
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        log.info("Payment {} marked as REFUNDED", payment.getPaymentId());

        paymentService.update(payment);
        orderService.update(order);

        for (OrderItem item : order.getOrderItems()) {
            productService.updateStockForAddition(item.getProduct(), item.getQuantity());
            log.info("Stock restored: +{} units for product '{}'", item.getQuantity(), item.getProduct().getName());
        }

        log.info("Refund processed and stock restored for order {}", orderUuid);
    }

    private static Map<String, Object> extractMetaData(Map<String, Object> paymentObject) {
        return (Map<String, Object>) paymentObject.get("metadata");
    }

    private static Map<String, Object> extractObjectMap(Map<String, Object> paymentData) {
        return (Map<String, Object>) paymentData.get("object");
    }

    private UUID extractOrderUuid(Map<String, Object> paymentData) {
        Map<String, Object> paymentObject = extractObjectMap(paymentData);
        String description = (String) paymentObject.get("description");
        if (description != null && description.contains("#")) {
            String orderId = description.substring(description.indexOf("#") + 1).trim();
            return UUID.fromString(orderId);
        } else {
            log.error("Missing or invalid order UUID in payment data description");
            throw new NullPointerException("Order UUID is missing from payment data");
        }
    }
}
