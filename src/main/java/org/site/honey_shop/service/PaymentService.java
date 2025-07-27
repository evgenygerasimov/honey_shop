package org.site.honey_shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.repository.PaymentRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${myapp.shop.id}")
    private String SHOP_ID;
    @Value("${myapp.shop.yookassa.api.key}")
    private String API_KEY;
    @Value("${myapp.shop.yookassa.confirmation.url}")
    private String CONFIRMATION_URL;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final HttpSession session;

    public Payment findById(UUID id) {
        log.info("Get payment by id: {}", id);
        return paymentRepository.findById(id).orElseThrow(()
                -> new RuntimeException("Payment not found"));
    }

    public void save(Payment payment) {
        payment = Payment.builder()
                .amount(payment.getAmount())
                .paymentStatus(PaymentStatus.SUCCESS)
                .order(payment.getOrder())
                .build();
        log.info("Payment successfully saved.");
        paymentRepository.save(payment);
    }

    public void update(Payment payment) {
        Payment exisitingPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow(()
                -> new RuntimeException("Payment not found"));
        exisitingPayment.setPaymentStatus(payment.getPaymentStatus());
        log.info("Payment with id: {}", exisitingPayment.getPaymentId());
        paymentRepository.save(exisitingPayment);
    }

    public String createConfirmationUrl(Order order) {
        log.info("Create confirmation url for order: {}", order.getOrderId());
        String idempotenceKey = UUID.randomUUID().toString();

        Map<String, Object> receipt = getReceipt(order);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", Map.of("value", order.getTotalOrderAmount().toString(), "currency", "RUB"));
        requestBody.put("description", "Оплата заказа #" + order.getOrderId());
        requestBody.put("capture", true);
        requestBody.put("confirmation", Map.of("type", "redirect", "return_url", "https://localhost:8443/"));
        requestBody.put("metadata", Map.of("sessionId", session.getId()));
        requestBody.put("receipt", receipt);
        log.info("Request body constructed: {}", requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Host", "api.yookassa.ru");
        headers.set("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((SHOP_ID + ":" + API_KEY).getBytes()));
        headers.set("Idempotence-Key", idempotenceKey);
        log.info("Request headers constructed: {}", headers);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(CONFIRMATION_URL, HttpMethod.POST, requestEntity, String.class);
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> confirmation = (Map<String, Object>) responseMap.get("confirmation");

            if (confirmation != null) {
                log.info("Confirmation url: {} for order: {} created successfully.", confirmation.get("confirmation_url"), order.getOrderId());
                return (String) confirmation.get("confirmation_url");
            } else {
                log.error("Confirmation url not found in response for order: {}. Response: {}", order.getOrderId(), response.getBody());
                throw new RuntimeException("Confirmation url not found in response!");
            }
        } catch (Exception e) {
            log.error("Confirmation url not found in response for order: {}.", order.getOrderId(), e);
            throw new RuntimeException("Error while parsing confirmation_url", e);
        }
    }

    private Map<String, Object> getReceipt(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            Map<String, Object> productItem = Map.of(
                    "description", item.getProduct().getName(),
                    "quantity", item.getQuantity(),
                    "amount", Map.of(
                            "value", item.getPricePerUnit()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                                    .setScale(2, RoundingMode.HALF_UP)
                                    .toPlainString(),
                            "currency", "RUB"
                    ),
                    "vat_code", 1,
                    "payment_subject", "commodity",
                    "payment_mode", "full_prepayment"
            );
            items.add(productItem);
        }

        BigDecimal deliveryPrice = order.getDeliveryAmount(); // = 330.00 например
        if (deliveryPrice != null && deliveryPrice.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> deliveryItem = Map.of(
                    "description", "Доставка",
                    "quantity", 1,
                    "amount", Map.of(
                            "value", deliveryPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            "currency", "RUB"
                    ),
                    "vat_code", 1,
                    "payment_subject", "service",
                    "payment_mode", "full_prepayment"
            );
            items.add(deliveryItem);
        }

        Map<String, Object> receipt = new HashMap<>();
        Map<String, Object> customer = Map.of("email", order.getCustomerEmail());
        receipt.put("customer", customer);
        receipt.put("items", items);
        return receipt;
    }
}
