package org.site.honey_shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CdekCacheServiceTest {

    @InjectMocks
    private CdekCacheService cdekCacheService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ExecutorService executor;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cdekCacheService.setBaseUrl("http://example.com/api");
        cdekCacheService.setCdekCacheKeys("cdek::keys");
        cdekCacheService.setMaxRetries(3);
        cdekCacheService.setRetryDelayMs(100);
        cdekCacheService.setSleepBetweenAttemptsMs(100);
        cdekCacheService.setTimeoutMinutes(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void testGetOfficesWithCaching_fromCache() {
        Map<String, String> params = Map.of("action", "offices");
        String cacheKey = "cdek::action=offices";

        when(valueOperations.get(cacheKey)).thenReturn("cached-data");

        String result = cdekCacheService.getOfficesWithCaching(params);

        assertEquals("cached-data", result);
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void testGetOfficesWithCaching_noCache() {
        Map<String, String> params = Map.of("action", "offices");
        String cacheKey = "cdek::action=offices";
        String url = "http://example.com/api?action=offices";

        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(restTemplate.getForObject(url, String.class)).thenReturn("fresh-data");

        String result = cdekCacheService.getOfficesWithCaching(params);

        assertEquals("fresh-data", result);
        verify(valueOperations).set(cacheKey, "fresh-data");
    }

    @Test
    void testUpdateCache() {
        Map<String, String> params = Map.of("action", "offices");
        String cacheKey = "cdek::action=offices";
        String url = "http://example.com/api?action=offices";

        when(restTemplate.getForObject(url, String.class)).thenReturn("updated-data");

        String result = cdekCacheService.updateCache(params, cacheKey);

        assertEquals("updated-data", result);
        verify(valueOperations).set(cacheKey, "updated-data");
        verify(listOperations).rightPush("cdek::keys", cacheKey);
    }

    @Test
    void testFetchCdekData_validResponse() {
        String url = "http://example.com/api";
        when(restTemplate.getForObject(url, String.class)).thenReturn("valid-response");

        String result = cdekCacheService.fetchCdekData(url);

        assertEquals("valid-response", result);
    }

    @Test
    void testFetchCdekData_invalidResponse() {
        String url = "http://example.com/api";
        when(restTemplate.getForObject(url, String.class)).thenReturn("<b>Fatal error</b>");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cdekCacheService.fetchCdekData(url));

        assertEquals("Error while getting data from CDEK API", ex.getMessage());
    }

    @Test
    void testBuildCacheKey() {
        Map<String, String> params = new HashMap<>();
        params.put("b", "2");
        params.put("a", "1");

        String key = cdekCacheService.buildCacheKey(params);
        assertEquals("cdek::a=1&b=2", key);
    }

    @Test
    void testScheduledRefreshWithTimeout_success() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> mockFuture = mock(Future.class);

        when(executor.submit(any(Runnable.class))).thenReturn((Future) mockFuture);
        when(mockFuture.get(anyLong(), any())).thenReturn(null);

        cdekCacheService.scheduledRefreshWithTimeout();

        verify(mockFuture).get(anyLong(), any());
    }


    @Test
    void testScheduledRefreshWithTimeout_timeout() throws Exception {
        Future<Object> mockFuture = mock(Future.class);
        when(executor.submit(any(Runnable.class))).thenReturn((Future) mockFuture);
        doThrow(new TimeoutException()).when(mockFuture).get(anyLong(), any());

        cdekCacheService.scheduledRefreshWithTimeout();

        verify(mockFuture, times(3)).cancel(true);
    }

}
