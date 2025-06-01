package org.site.honey_shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentEventHandlerFacade {

    private final PaymentEventHandlerService paymentEventHandlerService;
    private final ObjectMapper objectMapper;

    public void eventHandler(String payload) {
        try {
            log.info("Received payload: {}", payload);
            Map<String, Object> paymentData = objectMapper.readValue(payload, Map.class);
            String event = (String) paymentData.get("event");
            JsonNode root = objectMapper.readTree(payload);

            switch (event) {
                case "payment.succeeded":
                    log.info("Retrieved information about succeeded payment for order: {}",
                            root.path("object").path("description").asText());
                    paymentEventHandlerService.handlePaymentSucceeded(paymentData);
                    break;
                case "payment.canceled":
                    log.info("Retrieved information about canceled payment for order: {}",
                            root.path("object").path("description").asText());
                    paymentEventHandlerService.handlePaymentCanceled(paymentData);
                    break;
                case "refund.succeeded":
                    log.info("Retrieved information about refund succeeded payment for order: {}",
                            root.path("object").path("description").asText());
                    paymentEventHandlerService.handleRefundSucceeded(paymentData);
                    break;
                default:
                    log.error("Unknown payment event {}", event);
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported event");
                    return;
            }
            log.info("Notification processed successfully");
            ResponseEntity.ok("Notification received");
        } catch (Exception e) {
            log.error("Error processing payment", e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment");
        }
    }
}
