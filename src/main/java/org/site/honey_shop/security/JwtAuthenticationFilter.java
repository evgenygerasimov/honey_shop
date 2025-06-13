package org.site.honey_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.service.AuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

import static java.util.Arrays.stream;

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

        String path = request.getRequestURI();

        if (path.equals("/auth/logout")) {
            Cookie[] cookies = request.getCookies();
            Optional<String> accessTokenOptional = Optional.empty();
            if (cookies != null) {
                accessTokenOptional = getCookie(cookies, "access_token");
            }
            try {
                String accessToken = accessTokenOptional.orElseThrow(()
                        -> new MyAuthenticationException("Access token not found"));
                authService.logout(accessToken, request, response);

                filterChain.doFilter(request, response);
            } catch (MyAuthenticationException ex) {
                response.sendRedirect(request.getContextPath() + "/error?errorMessage=" + URLEncoder.encode(ex.getMessage(), "UTF-8"));
            }
            return;
        }



        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<String> accessToken = getCookie(cookies, "access_token");
                Optional<String> refreshToken = getCookie(cookies, "refresh_token");

                if (accessToken.isPresent()) {
                    String access = accessToken.get();

                    if (!jwtService.isAccessTokenExpiredAndInvalid(access)) {
                        authenticateUser(access);
                    } else if (refreshToken.isPresent()) {
                        String refresh = refreshToken.get();

                        if (!jwtService.isRefreshTokenExpiredAndInvalid(refresh) &&
                                jwtService.findByRefreshToken(refresh).isRefreshTokenValid()) {

                            Token newToken = authService.refreshToken(refresh);
                            jwtService.invalidateToken(jwtService.findByRefreshToken(refresh));

                            authenticateUser(newToken.getAccessToken());

                            setCookie(response, "access_token", newToken.getAccessToken());
                            setCookie(response, "refresh_token", newToken.getRefreshToken());
                        } else {
                            SecurityContextHolder.clearContext();
                            redirectToLogin(response);
                            return;
                        }
                    } else {
                        SecurityContextHolder.clearContext();
                        redirectToLogin(response);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT auth filter error", e);
            SecurityContextHolder.clearContext();
            try {
                redirectToLogin(response);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> getCookie(Cookie[] cookies, String name) {
        return stream(cookies)
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

    private void redirectToLogin(HttpServletResponse response) throws Exception {
        response.sendRedirect("/auth/login");
    }
}
