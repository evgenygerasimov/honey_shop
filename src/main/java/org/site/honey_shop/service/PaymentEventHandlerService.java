package org.site.honey_shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.*;
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

    @Transactional
    public void handlePaymentSucceeded(Map<String, Object> paymentData) {
        Map<String, Object> paymentObject = extractObjectMap(paymentData);
        Map<String, Object> metadata = extractMetaData(paymentObject);

        if (metadata != null) {
            String sessionId = (String) metadata.get("sessionId");
            paymentCashService.savePaymentSuccess(sessionId, true);
            log.info("Save payment success flag for sessionId={}", sessionId);
        } else {
            log.warn("Metadata is null, skipping payment success update in handlePaymentSucceeded()");
            throw new NullPointerException("Metadata is null, skipping payment success update in handlePaymentSucceeded()");
        }

        extractOrderUuid(paymentData);
        log.info("Find order by uuid={} from payment data for handlePaymentSucceeded.", extractOrderUuid(paymentData));
        Order order = orderService.findById(extractOrderUuid(paymentData));

        log.info("Order with uuid={} found, updating order status and payment status.", extractOrderUuid(paymentData));
        order.setOrderStatus(OrderStatus.PAID);
        log.info("Update order status successfully.");

        log.info("Find payment for order by id={}", extractOrderUuid(paymentData));
        Payment payment = paymentService.findById(order.getPayment().getPaymentId());
        log.info("Payment with id={} found, update payment status.", payment.getPaymentId());

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        log.info("Update payment status successfully for payment id={}.", payment.getPaymentId());

        paymentService.update(payment);
        orderService.update(order);
        log.info("Order and payment status updated successfully.");


        for (OrderItem item : order.getOrderItems()) {
            log.info("Update stock for product with name={} after payment success.", item.getProduct().getName());
            productService.updateStockForReduction(item.getProduct(), item.getQuantity());
            log.info("New stock={} for product with name={} .", item.getProduct().getStockQuantity(), item.getProduct().getName());
        }
    }

    @Transactional
    public void handlePaymentCanceled(Map<String, Object> paymentData) {
        log.info("Find order by uuid={} from payment data for handlePaymentCanceled.", extractOrderUuid(paymentData));
        Order order = orderService.findById(extractOrderUuid(paymentData));

        log.info("Order with uuid={} found, updating order status and payment status.", extractOrderUuid(paymentData));
        order.setOrderStatus(OrderStatus.CANCELLED);
        log.info("Update order status successfully.");

        log.info("Find payment for order by id={}", extractOrderUuid(paymentData));
        Payment payment = paymentService.findById(order.getPayment().getPaymentId());
        log.info("Payment with id={} found, update payment status.", payment.getPaymentId());

        payment.setPaymentStatus(PaymentStatus.FAILED);
        log.info("Update status successfully for payment with id={}.", payment.getPaymentId());

        paymentService.update(payment);
        orderService.update(order);
        log.info("Order and payment status updated successfully.");

    }

    @Transactional
    public void handleRefundSucceeded(Map<String, Object> paymentData) {
        log.info("Find order by uuid={} from payment data for handleRefundSucceeded.", extractOrderUuid(paymentData));
        Order order = orderService.findById(extractOrderUuid(paymentData));

        log.info("Order with uuid={} found, updating order status and payment status.", extractOrderUuid(paymentData));
        order.setOrderStatus(OrderStatus.CANCELLED);
        log.info("Update order status successfully.");
        log.info("Find payment for order by id={}", extractOrderUuid(paymentData));
        Payment payment = paymentService.findById(order.getPayment().getPaymentId());

        log.info("Payment with id={} found, update payment status.", payment.getPaymentId());
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        log.info("Update payment status successfully.");
        paymentService.update(payment);
        orderService.update(order);
        log.info("Order and payment status updated successfully.");

        for (OrderItem item : order.getOrderItems()) {
            log.info("Update stock for product with name={} after refund success.", item.getProduct().getName());
            productService.updateStockForAddition(item.getProduct(), item.getQuantity());
            log.info("New stock={} for product with name={} .", item.getProduct().getStockQuantity(), item.getProduct().getName());
        }

    }

    private static Map<String, Object> extractMetaData(Map<String, Object> paymentObject) {
        return (Map<String, Object>) paymentObject.get("metadata");

    }

    private static Map<String, Object> extractObjectMap(Map<String, Object> paymentData) {
        return (Map<String, Object>) paymentData.get("object");
    }

    private UUID extractOrderUuid(Map<String, Object> paymentData) {
        UUID orderUuid;
        Map<String, Object> paymentObject = extractObjectMap(paymentData);
        String description = (String) paymentObject.get("description");
        if (description != null && description.contains("#")) {
            log.info("Order UUID found in payment data.");
            String orderId = description.substring(description.indexOf("#") + 1).trim();
            orderUuid = UUID.fromString(orderId);
        } else {
            log.error("Order UUID from paymentData is null or empty.");
            throw new NullPointerException("Order UUID is null, skipping order uuid extraction");
        }
        return orderUuid;
    }
}
