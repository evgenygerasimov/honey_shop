package org.site.honey_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.service.AuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthService authService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<String> accessToken = getCookie(cookies, "access_token");
                Optional<String> refreshToken = getCookie(cookies, "refresh_token");

                if (accessToken.isPresent()) {
                    String access = accessToken.get();

                    if (!jwtService.isAccessTokenExpired(access)) {
                        authenticateUser(access);
                    } else if (refreshToken.isPresent()) {
                        String refresh = refreshToken.get();

                        if (!jwtService.isRefreshTokenExpired(refresh) &&
                                jwtService.findByRefreshToken(refresh).isRefreshTokenValid()) {

                            Token newToken = authService.refreshToken(refresh);
                            jwtService.invalidateToken(jwtService.findByRefreshToken(refresh));

                            authenticateUser(newToken.getAccessToken());

                            setCookie(response, "access_token", newToken.getAccessToken());
                            setCookie(response, "refresh_token", newToken.getRefreshToken());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT auth filter error", e);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> getCookie(Cookie[] cookies, String name) {
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void authenticateUser(String accessToken) {
        String username = jwtService.extractUserName(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        log.info("User {} authenticated by JWT", username);
    }

    private void setCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
