package org.site.honey_shop.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentCashService {

    private final StringRedisTemplate redisTemplate;

    private static final String PAYMENT_SUCCESS_KEY = "paymentSuccess";
    private static final Duration EXPIRATION_TIME = Duration.ofDays(7);

    public void savePaymentSuccess(String sessionId, boolean success) {
        log.info("Saving payment flag={} for session id:{}  ",  success, sessionId);
        redisTemplate.opsForValue().set(PAYMENT_SUCCESS_KEY + ":" + sessionId, String.valueOf(success), EXPIRATION_TIME);
        log.info("Payment flag={} saved for session id:{}  ", success, sessionId);
    }

    public boolean getPaymentSuccess(String sessionId) {
        String result = redisTemplate.opsForValue().get(PAYMENT_SUCCESS_KEY + ":" + sessionId);
        return Boolean.parseBoolean(result);
    }

    public void setPaymentSuccess(String sessionId, boolean success) {
        log.info("Setting payment flag={} for session id:{}", success, sessionId);
        redisTemplate.opsForValue().set(PAYMENT_SUCCESS_KEY + ":" + sessionId, String.valueOf(success));
        log.info("Payment flag={} set for session id:{}", success, sessionId);
    }
}