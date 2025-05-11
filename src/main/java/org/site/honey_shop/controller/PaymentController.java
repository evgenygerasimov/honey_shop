package org.site.honey_shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.service.OrderService;
import org.site.honey_shop.service.PaymentEventHandlerFacade;
import org.site.honey_shop.service.PaymentService;
import org.site.honey_shop.service.PaymentCashService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentEventHandlerFacade paymentEventHandlerFacade;
    private final OrderService orderService;
    private final PaymentCashService paymentCashService;
    private final HttpSession session;

    @GetMapping("/new")
    public String newPaymentPage(@RequestParam("order_id") UUID orderId) {
        Order order = orderService.findById(orderId);
        String confirmationUrl = paymentService.createConfirmationUrl(order);
        return "redirect:" + confirmationUrl;
    }

    @PostMapping("/notify")
    public ResponseEntity<String> handlePaymentNotification(@RequestBody String payload) {
        paymentEventHandlerFacade.eventHandler(payload);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/clear-cart")
    public ResponseEntity<Void> clearCart() {
        paymentCashService.setPaymentSuccess  (session.getId(), false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-payment-status")
    public ResponseEntity<Map<String, Boolean>> checkPaymentStatus() {
        boolean paymentSuccess = paymentCashService.getPaymentSuccess(session.getId());
        Map<String, Boolean> response = new HashMap<>();
        response.put("paymentSuccess", paymentSuccess);
        return ResponseEntity.ok(response);
    }
}

