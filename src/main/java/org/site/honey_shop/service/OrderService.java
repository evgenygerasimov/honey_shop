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
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopMapper shopMapper;

    public OrderDTO findOrderDTOById(UUID orderId) {
        log.info("Find orderDTO: {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        return shopMapper.toDto(order);
    }

    public Order findById(UUID orderId){
        log.info("Find order: {}", orderId);
        return orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
    }

    public List<OrderDTO> findAll() {
        List<OrderDTO> listOrderDTO = new ArrayList<>();
        for (Order order : orderRepository.findAll()) {
            listOrderDTO.add(shopMapper.toDto(order));
        }
        listOrderDTO.sort((o1, o2) -> o2.getCreateDate().compareTo(o1.getCreateDate()));
        log.info("Find all orders sorted by createDate descending");
        return listOrderDTO;
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
            orderItem.setOrder(order);
        }

        order.setOrderItems(orderItems);
        log.info("Attempt to save order: {}", order.getOrderId());
        order = orderRepository.save(order);
        log.info("Order saved: {}", order.getOrderId());
        return order;
    }

    public Order update(Order order) {
        Order existingOrder = orderRepository.findById(order.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        existingOrder.setOrderStatus(order.getOrderStatus());
        existingOrder.setPaymentStatus(order.getPayment().getPaymentStatus());
        log.info("Attempt to update order: {}", existingOrder);
        return orderRepository.save(existingOrder);
    }

    public void delete(UUID orderId) {
        log.info("Attempt to delete order: {}", orderId);
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
