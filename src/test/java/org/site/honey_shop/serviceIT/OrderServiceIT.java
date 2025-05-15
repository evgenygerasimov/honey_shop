package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.repository.OrderRepository;
import org.site.honey_shop.service.OrderEventPublisher;
import org.site.honey_shop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderServiceIT extends TestContainerConfig {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void testSaveOrder_withValidOrderAndItems_savesSuccessfully() {
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
                .deliveryType("PVZ")
                .build();

        String json = "[{\"order\":{},\"product\":{\"productId\":\"2304ce98-5fe5-46ee-abf1-c8e7eba4328a\"},\"quantity\":1,\"pricePerUnit\":10.99}]";

        Order saved = orderService.save(order, json);

        assertThat(saved.getOrderId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getPayment()).isNotNull();
        assertThat(saved.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void testFindOrderDTOById_returnsCorrectData() {
        Order saved = createAndPersistSimpleOrder();

        OrderDTO dto = orderService.findOrderDTOById(saved.getOrderId());

        assertThat(dto.getOrderId()).isEqualTo(saved.getOrderId());
        assertThat(dto.getFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(dto.getOrderItems()).hasSize(1);
    }

    @Test
    void testUpdateOrderStatus_updatesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        Order updated = orderService.updateOrderStatus(saved, OrderStatus.SHIPPED);

        assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void testUpdateOrderPaymentStatus_updatesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        Order updated = orderService.updateOrderPaymentStatus(saved, PaymentStatus.SUCCESS);

        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testDeleteOrder_deletesSuccessfully() {
        Order saved = createAndPersistSimpleOrder();

        orderService.delete(saved.getOrderId());

        assertThat(orderRepository.findById(saved.getOrderId())).isEmpty();
    }

    private Order createAndPersistSimpleOrder() {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .customerEmail("ivan@example.com")
                .deliveryAddress("ул. Ленина, 1")
                .customerPhone("+79991234567")
                .productAmount(BigDecimal.valueOf(9.99))
                .deliveryAmount(BigDecimal.ZERO)
                .totalOrderAmount(BigDecimal.valueOf(9.99))
                .deliveryType("PVZ")
                .build();

        String json = "[{\"order\":{},\"product\":{\"productId\":\"2304ce98-5fe5-46ee-abf1-c8e7eba4328a\"},\"quantity\":1,\"pricePerUnit\":9.99}]";

        return orderService.save(order, json);
    }
}
