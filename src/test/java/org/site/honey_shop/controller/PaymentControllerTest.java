package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.site.honey_shop.controller.PaymentController;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.service.OrderService;
import org.site.honey_shop.service.PaymentCashService;
import org.site.honey_shop.service.PaymentEventHandlerFacade;
import org.site.honey_shop.service.PaymentService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentEventHandlerFacade paymentEventHandlerFacade;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentCashService paymentCashService;
    @Mock
    private MockHttpSession session;
    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(paymentController).build();
    }

    @Test
    void testNewPaymentPage_redirectsToConfirmationUrl() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order mockOrder = new Order();
        String confirmationUrl = "https://example.com/confirm";

        when(orderService.findById(orderId)).thenReturn(mockOrder);
        when(paymentService.createConfirmationUrl(mockOrder)).thenReturn(confirmationUrl);

        mockMvc.perform(get("/payments/new").param("order_id", orderId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(confirmationUrl));
    }

    @Test
    void testHandlePaymentNotification_returnsOk() throws Exception {
        String payload = "{\"event\": \"payment_success\"}";

        mockMvc.perform(post("/payments/notify")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(paymentEventHandlerFacade).eventHandler(payload);
    }

    @Test
    void testClearCart_returnsOk() throws Exception {
        mockMvc.perform(post("/payments/clear-cart").session(session))
                .andExpect(status().isOk());

        verify(paymentCashService).setPaymentSuccess(session.getId(), false);
    }

    @Test
    void testCheckPaymentStatus_returnsPaymentSuccess() throws Exception {
        when(paymentCashService.getPaymentSuccess(session.getId())).thenReturn(true);

        mockMvc.perform(get("/payments/check-payment-status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentSuccess").value(true));
    }
}
