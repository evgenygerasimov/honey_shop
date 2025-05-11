package org.site.honey_shop.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final AuthService authService;
    private final UserDetailsService userDetailsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.getMethodAnnotation(PreAuthorize.class) == null) {
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            redirectToLogin(response);
            return false;
        }

        Optional<String> accessToken = getCookie(cookies, "access_token");
        Optional<String> refreshToken = getCookie(cookies, "refresh_token");

        if (accessToken.isPresent()) {
            if (!jwtService.isAccessTokenExpired(accessToken.get())) {
                log.info("Access token {} is valid.", accessToken.get());
                return true;
            }

            if (refreshToken.isPresent() && !jwtService.isRefreshTokenExpired(refreshToken.get())) {
                try {
                    log.info("Trying to refresh access token using refresh token {}.", refreshToken.get());
                    Token newToken = authService.refreshToken(refreshToken.get());
                    jwtService.invalidateToken(jwtService.findByRefreshToken(refreshToken.get()));

                    String username = jwtService.extractUserName(newToken.getAccessToken());
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    setCookie(response, "access_token", newToken.getAccessToken());
                    setCookie(response, "refresh_token", newToken.getRefreshToken());
                    log.info("Access token {} refreshed successfully.", accessToken.get());
                    return true;
                } catch (Exception e) {
                    log.error("Error while refreshing access token.", e);
                    handleTokenError(response, refreshToken.get());
                    redirectToLogin(response);
                    return false;
                }
            } else {
                log.info("Refresh token is expired or is not present in cookies.");
                handleTokenError(response, refreshToken.orElse(null));
                redirectToLogin(response);
                return false;
            }
        }
        log.info("Access token  is not present in cookies.");
        redirectToLogin(response);
        return false;
    }

    private void redirectToLogin(HttpServletResponse response) throws Exception {
        response.sendRedirect("/auth/login");
    }

    private Optional<String> getCookie(Cookie[] cookies, String name) {
        return Arrays.stream(cookies).filter(c -> c.getName().equals(name)).map(Cookie::getValue).findFirst();
    }

    private void setCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private void handleTokenError(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null) {
            jwtService.invalidateToken(jwtService.findByRefreshToken(refreshToken));
        }
        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
