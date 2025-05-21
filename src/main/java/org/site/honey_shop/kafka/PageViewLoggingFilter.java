package org.site.honey_shop.kafka;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.site.honey_shop.entity.PageViewEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class PageViewLoggingFilter extends OncePerRequestFilter {

    private final PageViewEventPublisher pageViewEventPublisher;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            String path = request.getRequestURI();
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            String sessionId = (request.getSession(false) != null)
                    ? request.getSession(false).getId()
                    : null;
            LocalDateTime visitTime = LocalDateTime.now();

            PageViewEvent event = new PageViewEvent(path, ip, userAgent, sessionId, visitTime);
            pageViewEventPublisher.publishPageViewEvent(event);
        }

        filterChain.doFilter(request, response);
    }
}
