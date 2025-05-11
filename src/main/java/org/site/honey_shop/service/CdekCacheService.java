package org.site.honey_shop.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
public class CdekCacheService {

    @Value("${cdek.cache.keys-list}")
    private String cdekCacheKeys;
    @Value("${cdek.cache.base-url}")
    private String baseUrl;
    @Value("${cdek.cache.max-retries}")
    private int maxRetries;
    @Value("${cdek.cache.retry-delay-ms}")
    private int retryDelayMs;
    @Value("${cdek.cache.sleep-between-attempts-ms}")
    private int sleepBetweenAttemptsMs;
    @Value("${cdek.cache.timeout-minutes}")
    private long timeoutMinutes;

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ExecutorService executor;
//            =
//            new ThreadPoolExecutor(
//            1, 1,
//            0L, TimeUnit.MILLISECONDS,
//            new LinkedBlockingQueue<>(),
//            Executors.defaultThreadFactory(),
//            new ThreadPoolExecutor.AbortPolicy()
//    );

    public String getOfficesWithCaching(Map<String, String> params) {
        String cacheKey = buildCacheKey(params);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        return cached != null ? cached : updateCache(params, cacheKey);
    }

    public String updateCache(Map<String, String> params, String cacheKey) {
        String url = buildUrl(params);
        log.info("Requesting CDEK API: {}", url);

        String response = fetchCdekData(url);
        redisTemplate.opsForValue().set(cacheKey, response);
        redisTemplate.opsForList().rightPush(cdekCacheKeys, cacheKey);

        return response;
    }

    public String buildCacheKey(Map<String, String> params) {
        return "cdek::" + params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private String buildUrl(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        params.forEach(builder::queryParam);
        return builder.toUriString();
    }

    public String fetchCdekData(String url) {
        String response = restTemplate.getForObject(url, String.class);
        if (response == null || response.isBlank() || response.contains("<b>Fatal error</b>")) {
            log.error("Error while getting data from CDEK: {}", response);
            throw new IllegalStateException("Error while getting data from CDEK API");
        }
        return response;
    }

    @Scheduled(cron = "${cdek.cache.scheduling-cron}")
    public void scheduledRefreshWithTimeout() {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Future<?> future = executor.submit(this::refreshCdekCache);
            try {
                future.get(timeoutMinutes, TimeUnit.MINUTES);
                log.info("Update CDEK cache completed successfully.");
                return;
            } catch (TimeoutException e) {
                log.error("Attempt {}: update timed out after {} minutes. Cancelling...", attempt, timeoutMinutes);
                future.cancel(true);
            } catch (Exception e) {
                log.error("Attempt {}: error during cache update", attempt, e);
            }

            if (attempt < maxRetries) {
                log.warn("Retrying update in {} seconds...", retryDelayMs / 1000);
                sleep(retryDelayMs);
            } else {
                log.error("Update CDEK cache failed after {} attempts.", maxRetries);
            }
        }
    }

    public void refreshCdekCache() {
        log.info("Starting refreshCdekCache...");

        List<Map<String, String>> orderedParams = List.of(
                Map.of("action", "offices", "is_handout", "true", "page", "0"),
                Map.of("action", "offices", "is_handout", "true", "page", "1", "size", "1")
        );

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Thread interrupted. Aborting refresh.");
                return;
            }

            List<String> loadedKeys = new ArrayList<>();
            boolean allSuccess = true;

            for (Map<String, String> params : orderedParams) {
                String cacheKey = buildCacheKey(params);
                String url = buildUrl(params);
                try {
                    String response = fetchCdekData(url);
                    redisTemplate.opsForValue().set(cacheKey, response);
                    loadedKeys.add(cacheKey);
                    log.info("Loaded cache key: {}", cacheKey);
                } catch (Exception e) {
                    log.error("Failed to load key {}: {}", cacheKey, e.getMessage());
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess && loadedKeys.size() == orderedParams.size()) {
                redisTemplate.opsForList().rightPushAll(cdekCacheKeys, loadedKeys);
                log.info("Successfully refreshed all CDEK cache keys.");
                return;
            } else {
                redisTemplate.delete(loadedKeys);
                log.warn("Attempt {}/{} failed. Retrying...", attempt, maxRetries);
                sleep(sleepBetweenAttemptsMs);
            }
        }

        log.error("Failed to refresh CDEK cache after {} attempts.", maxRetries);
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted. Exiting.");
        }
    }
}

