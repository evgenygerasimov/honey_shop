package org.site.honey_shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.service.PaymentCashService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCashServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PaymentCashService paymentCashService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testSavePaymentSuccess() {
        String sessionId = "abc123";
        boolean success = true;
        String expectedKey = "paymentSuccess:" + sessionId;

        paymentCashService.savePaymentSuccess(sessionId, success);

        verify(valueOperations).set(eq(expectedKey), eq(String.valueOf(success)), eq(Duration.ofDays(7)));
    }

    @Test
    void testGetPaymentSuccess_returnsTrue() {
        String sessionId = "abc123";
        String expectedKey = "paymentSuccess:" + sessionId;

        when(valueOperations.get(expectedKey)).thenReturn("true");

        boolean result = paymentCashService.getPaymentSuccess(sessionId);

        verify(valueOperations).get(expectedKey);
        assert result;
    }

    @Test
    void testGetPaymentSuccess_returnsFalse() {
        String sessionId = "abc123";
        String expectedKey = "paymentSuccess:" + sessionId;

        when(valueOperations.get(expectedKey)).thenReturn("false");

        boolean result = paymentCashService.getPaymentSuccess(sessionId);

        verify(valueOperations).get(expectedKey);
        assert !result;
    }

    @Test
    void testGetPaymentSuccess_nullValue() {
        String sessionId = "abc123";
        String expectedKey = "paymentSuccess:" + sessionId;

        when(valueOperations.get(expectedKey)).thenReturn(null);

        boolean result = paymentCashService.getPaymentSuccess(sessionId);

        verify(valueOperations).get(expectedKey);
        assert !result;
    }

    @Test
    void testSetPaymentSuccess() {
        String sessionId = "xyz789";
        boolean success = false;
        String expectedKey = "paymentSuccess:" + sessionId;

        paymentCashService.setPaymentSuccess(sessionId, success);

        verify(valueOperations).set(eq(expectedKey), eq(String.valueOf(success)));
    }
}
