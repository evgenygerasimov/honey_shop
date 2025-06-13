package org.site.honey_shop.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public Token login(String username, String password, HttpServletResponse response) {

        try {
            log.info("Trying to login user: {}", username);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
            String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

            addTokensToCookie(response, accessToken, refreshToken);

            log.info("User {} logged in successfully", username);
            return jwtService.saveToken(userDetails.getUsername(), accessToken, refreshToken);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", username, e);
            throw new MyAuthenticationException("Неверный логин или пароль");
        }
    }

    private static void addTokensToCookie(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    public Token refreshToken(String refreshToken) {
        log.info("Trying to refresh token: {}", refreshToken);
        Token storedToken = jwtService.findByRefreshToken(refreshToken);
        if (jwtService.isRefreshTokenExpiredAndInvalid(refreshToken)) {
            log.info("Refresh token expired for user: {}", jwtService.extractUserName(refreshToken));
            storedToken.setRefreshTokenValid(false);
            jwtService.invalidateToken(storedToken);
            log.info("Expired Refresh token was successfully invalidated for user: {}", jwtService.extractUserName(refreshToken));
        }
        String newAccessToken = jwtService.generateAccessToken(storedToken.getUsername());
        String newRefreshToken = jwtService.generateRefreshToken(storedToken.getUsername());
        storedToken.setAccessToken(newAccessToken);
        storedToken.setRefreshToken(newRefreshToken);
        log.info("New access and refresh tokens generated for user: {}", jwtService.extractUserName(newAccessToken));
        return jwtService.saveToken(storedToken.getUsername(), newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        jwtService.invalidateToken(jwtService.findByAccessToken(accessToken));

        Cookie accessTokenCookie = new Cookie("access_token", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);

        Cookie refreshTokeCookie = new Cookie("refresh_token", null);
        refreshTokeCookie.setHttpOnly(true);
        refreshTokeCookie.setPath("/");
        refreshTokeCookie.setMaxAge(0);
        response.addCookie(refreshTokeCookie);

        SecurityContextHolder.clearContext();

        request.getSession().invalidate();

    }
}
