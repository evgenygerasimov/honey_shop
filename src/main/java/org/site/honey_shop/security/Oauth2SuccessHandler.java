package org.site.honey_shop.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = extractUsernameFromEmail(authentication);
        UserDetails userDetails;

        try {
            userDetails = userService.userDetailsService().loadUserByUsername(username);
        } catch (Exception e) {
            response.sendRedirect("/auth/login?error=user_not_found");
            return;
        }

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        jwtService.saveToken(userDetails.getUsername(), accessToken, refreshToken);

        addTokensToCookie(response, accessToken, refreshToken);

        response.sendRedirect("/users/" + userService.findByUsername(username).userId());
    }

    private static String extractUsernameFromEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
        Map<String, Object> attributes = oauth2User.getAttributes();
        return attributes.entrySet().stream().filter(entry -> entry
                .getKey().equals("email")).map(entry -> entry
                .getValue()
                .toString())
                .findFirst()
                .orElseThrow();
    }

    private void addTokensToCookie(HttpServletResponse response, String accessToken, String refreshToken) {
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
}
