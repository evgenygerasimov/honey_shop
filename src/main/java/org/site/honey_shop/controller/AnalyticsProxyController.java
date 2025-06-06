package org.site.honey_shop.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsProxyController {

    private final RestTemplate restTemplate;

    @Value("${analytics.base-url}")
    private String analyticsBaseUrl;

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxyToAnalytics(HttpServletRequest request,
                                                   HttpMethod method,
                                                   HttpServletResponse response,
                                                   @RequestBody(required = false) byte[] body,
                                                   @RequestHeader HttpHeaders headers) {

        String forwardUri = request.getRequestURI().replaceFirst("/analytics", "");
        String targetUrl = analyticsBaseUrl
                + forwardUri
                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        String accessToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("access_token")) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(targetUrl, method, requestEntity, byte[].class);
    }
}
