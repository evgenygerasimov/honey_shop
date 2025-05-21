package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.entity.OrderStatus;
import org.site.honey_shop.entity.Payment;
import org.site.honey_shop.entity.PaymentStatus;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.kafka.OrderEventPublisher;
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void testFindOrderDTOById_Success() {
        Order order = Order.builder().orderId(orderId).build();
        OrderDTO orderDTO = new OrderDTO();
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
        Order order = Order.builder().orderId(orderId).build();
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
    void testFindAll_ShouldReturnSortedDTOs() {
        Order order1 = Order.builder().createDate(LocalDateTime.now()).build();
        Order order2 = Order.builder().createDate(LocalDateTime.now().plusDays(1)).build();
        OrderDTO dto1 = new OrderDTO(); dto1.setCreateDate(order1.getCreateDate());
        OrderDTO dto2 = new OrderDTO(); dto2.setCreateDate(order2.getCreateDate());

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(shopMapper.toDto(order1)).thenReturn(dto1);
        when(shopMapper.toDto(order2)).thenReturn(dto2);

        List<OrderDTO> result = orderService.findAll();

        assertThat(result).containsExactly(dto2, dto1);
    }

    @Test
    void testSave_Success() {
        Order order = Order.builder()
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

        String orderItemsJson = "[{\"order\":{},\"product\":{\"productId\":\"2304ce98-5fe5-46ee-abf1-c8e7eba4328a\"},\"quantity\":1,\"pricePerUnit\":3000}]";
        Order savedOrder = Order.builder().orderId(orderId).build();
        when(orderRepository.save(any())).thenReturn(savedOrder);

        Order result = orderService.save(order, orderItemsJson);

        assertThat(result.getOrderId()).isEqualTo(orderId);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderItems()).hasSize(1);
    }

    @Test
    void testSave_InvalidJson() {
        Order order = Order.builder().build();
        String badJson = "invalid-json";

        assertThatThrownBy(() -> orderService.save(order, badJson))
                .isInstanceOf(OrderCreateException.class)
                .hasMessageContaining("Ошибка при разборе JSON");
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
    void testUpdateOrderPaymentStatus_Success() {
        Order existing = Order.builder().orderId(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenReturn(existing);

        Order updated = orderService.updateOrderPaymentStatus(existing, PaymentStatus.SUCCESS);

        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
