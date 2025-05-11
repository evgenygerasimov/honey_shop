package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.service.OrderService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;
    @Mock
    private OrderService orderService;
    @Mock
    private UserService userService;
    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    void testOrderDetails() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        Product product = new Product();
        product.setProductId(UUID.randomUUID());
        item.setProduct(product);

        OrderDTO orderDTO = mock(OrderDTO.class);
        when(orderDTO.getOrderItems()).thenReturn(List.of(item));

        when(orderService.findOrderDTOById(orderId)).thenReturn(orderDTO);

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("order-details"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("productsMap"));
    }

    @Test
    void testOrdersList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.findByUsername(anyString())).thenReturn(new UserResponseDTO(userId,
                        "testuser",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Role.ROLE_SUPER_ADMIN,
                        true,
                        null
                )
        );

        when(orderService.findAll()).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password")
        );

        mockMvc.perform(get("/orders").principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("all-orders"))
                .andExpect(model().attribute("userId", userId))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void testCreateOrder_WithErrors() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("orderItemsData", "") // обязательно передаём параметр
                )
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"));
    }

    @Test
    void testCreateOrder_WithException() throws Exception {
        Order order = Order.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Сергеевич")
                .customerEmail("ivan@example.com")
                .customerPhone("+7 (123) 456-78-90")
                .deliveryAddress("г. Москва, ул. Пушкина, д. 1")
                .deliveryAmount(BigDecimal.valueOf(100.00))
                .productAmount(BigDecimal.valueOf(400.00))
                .totalOrderAmount(BigDecimal.valueOf(500.00))
                .deliveryType("Курьер") // обязательно, так как поле nullable = false
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        String itemsJson = "[]";

        when(orderService.save(any(), any())).thenThrow(new OrderCreateException("Ошибка создания"));

        mockMvc.perform(post("/orders")
                        .param("orderItemsData", itemsJson)
                        .flashAttr("order", order))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"));
    }



    @Test
    void testUpdateOrderStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        when(orderService.findById(id)).thenReturn(order);

        mockMvc.perform(post("/orders/update-order-status/" + id)
                        .param("orderStatus", OrderStatus.PENDING.name()))
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).updateOrderStatus(order, OrderStatus.PENDING);
    }

    @Test
    void testUpdatePaymentStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        when(orderService.findById(id)).thenReturn(order);

        mockMvc.perform(post("/orders/update-payment-status/" + id)
                        .param("orderPaymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).updateOrderPaymentStatus(order, PaymentStatus.SUCCESS);
    }

    @Test
    void testDeleteOrder() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/orders/delete/" + id))
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).delete(id);
    }
}
