package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.service.PaymentCashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentCashServiceIT extends TestContainerConfig {

    @Autowired
    private PaymentCashService paymentCashService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SESSION_ID = "session123";

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testSavePaymentSuccess_successTrue_valueStoredWithExpiration() {
        paymentCashService.savePaymentSuccess(SESSION_ID, true);

        String value = redisTemplate.opsForValue().get("paymentSuccess:" + SESSION_ID);
        assertThat(value).isEqualTo("true");

        Boolean hasKey = redisTemplate.hasKey("paymentSuccess:" + SESSION_ID);
        assertThat(hasKey).isTrue();
    }

    @Test
    void testSavePaymentSuccess_successFalse_valueStoredWithExpiration() {
        paymentCashService.savePaymentSuccess(SESSION_ID, false);

        String value = redisTemplate.opsForValue().get("paymentSuccess:" + SESSION_ID);
        assertThat(value).isEqualTo("false");

        Boolean hasKey = redisTemplate.hasKey("paymentSuccess:" + SESSION_ID);
        assertThat(hasKey).isTrue();
    }

    @Test
    void testGetPaymentSuccess_valueExists_returnsCorrectBoolean() {
        redisTemplate.opsForValue().set("paymentSuccess:" + SESSION_ID, "true");

        boolean result = paymentCashService.getPaymentSuccess(SESSION_ID);

        assertThat(result).isTrue();
    }

    @Test
    void testGetPaymentSuccess_valueDoesNotExist_returnsFalse() {
        boolean result = paymentCashService.getPaymentSuccess("nonexistent");

        assertThat(result).isFalse();
    }

    @Test
    void testSetPaymentSuccess_overwritesValueWithoutExpiration() {
        paymentCashService.setPaymentSuccess(SESSION_ID, true);
        String first = redisTemplate.opsForValue().get("paymentSuccess:" + SESSION_ID);
        assertThat(first).isEqualTo("true");

        paymentCashService.setPaymentSuccess(SESSION_ID, false);
        String second = redisTemplate.opsForValue().get("paymentSuccess:" + SESSION_ID);
        assertThat(second).isEqualTo("false");
    }
}
