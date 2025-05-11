package org.site.honey_shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.entity.Payment;
import org.site.honey_shop.entity.PaymentStatus;
import org.site.honey_shop.repository.PaymentRepository;
import org.site.honey_shop.service.PaymentService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "SHOP_ID", "test_shop_id");
        ReflectionTestUtils.setField(paymentService, "API_KEY", "test_api_key");
        ReflectionTestUtils.setField(paymentService, "CONFIRMATION_URL", "https://api.yookassa.ru/v3/payments");
    }

    @Test
    void testFindById_Success() {
        UUID id = UUID.randomUUID();
        Payment payment = Payment.builder().paymentId(id).build();

        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

        Payment result = paymentService.findById(id);

        assertThat(result).isEqualTo(payment);
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.findById(id));
    }

    @Test
    void testSave_Success() {
        Order order = Order.builder().orderId(UUID.randomUUID()).build();
        Payment payment = Payment.builder()
                .amount(BigDecimal.valueOf(500))
                .order(order)
                .build();

        paymentService.save(payment);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getAmount()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(saved.getOrder()).isEqualTo(order);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void testUpdate_Success() {
        UUID id = UUID.randomUUID();
        Payment existing = Payment.builder()
                .paymentId(id)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment incoming = Payment.builder()
                .paymentId(id)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById(id)).thenReturn(Optional.of(existing));

        paymentService.update(incoming);

        assertThat(existing.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(existing);
    }

    @Test
    void testCreateConfirmationUrl_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .orderId(orderId)
                .totalOrderAmount(BigDecimal.valueOf(1000))
                .build();

        when(session.getId()).thenReturn("session-123");

        Map<String, Object> confirmationMap = Map.of("confirmation_url", "https://return.url");
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("confirmation", confirmationMap);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{ \"confirmation\": { \"confirmation_url\": \"https://return.url\" } }"));

        when(objectMapper.readValue(anyString(), eq(Map.class)))
                .thenReturn(responseMap);

        String url = paymentService.createConfirmationUrl(order);

        assertThat(url).isEqualTo("https://return.url");
    }

    @Test
    void testCreateConfirmationUrl_NoConfirmationInResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .orderId(orderId)
                .totalOrderAmount(BigDecimal.valueOf(1000))
                .build();

        when(session.getId()).thenReturn("session-123");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("someOtherField", "value");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{ \"someOtherField\": \"value\" }"));

        when(objectMapper.readValue(anyString(), eq(Map.class)))
                .thenReturn(responseMap);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> paymentService.createConfirmationUrl(order));
        assertThat(exception.getMessage()).contains("Error while parsing confirmation_url");
    }
}