package org.site.honey_shop.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.method.HandlerMethod;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JwtInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthService authService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    private final Token validToken = new Token();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validToken.setAccessToken("newAccessToken");
        validToken.setRefreshToken("newRefreshToken");
        validToken.setRefreshTokenValid(true);
    }

    @Test
    @SneakyThrows
    void testPreHandle_NoAnnotation_ShouldProceed() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(null);
        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);
        assertTrue(result);
    }

    @Test
    @SneakyThrows
    void testPreHandle_NoCookies_ShouldRedirect() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        when(request.getCookies()).thenReturn(null);

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendRedirect("/auth/login");
    }

    @Test
    @SneakyThrows
    void testPreHandle_ValidAccessToken_ShouldProceed() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        Cookie access = new Cookie("access_token", "validAccessToken");
        Cookie refresh = new Cookie("refresh_token", "validRefreshToken");
        when(request.getCookies()).thenReturn(new Cookie[]{access, refresh});
        when(jwtService.isAccessTokenExpired("validAccessToken")).thenReturn(false);

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    @SneakyThrows
    void testPreHandle_ExpiredAccessToken_ValidRefreshToken_ShouldRefresh() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        Cookie access = new Cookie("access_token", "expiredAccessToken");
        Cookie refresh = new Cookie("refresh_token", "validRefreshToken");
        when(request.getCookies()).thenReturn(new Cookie[]{access, refresh});

        when(jwtService.isAccessTokenExpired("expiredAccessToken")).thenReturn(true);
        when(jwtService.isRefreshTokenExpired("validRefreshToken")).thenReturn(false);
        when(authService.refreshToken("validRefreshToken")).thenReturn(validToken);
        when(jwtService.findByRefreshToken("validRefreshToken")).thenReturn(validToken);
        when(jwtService.extractUserName("newAccessToken")).thenReturn("john");
        UserDetails user = new User("john", "pass", Collections.emptyList());
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("access_token")));
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("refresh_token")));
    }

    @Test
    @SneakyThrows
    void testPreHandle_ExpiredAccessToken_ExpiredRefreshToken_ShouldRedirect() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        Cookie access = new Cookie("access_token", "expiredAccessToken");
        Cookie refresh = new Cookie("refresh_token", "expiredRefreshToken");
        when(request.getCookies()).thenReturn(new Cookie[]{access, refresh});

        when(jwtService.isAccessTokenExpired("expiredAccessToken")).thenReturn(true);
        when(jwtService.isRefreshTokenExpired("expiredRefreshToken")).thenReturn(true);

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendRedirect("/auth/login");
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("access_token") && cookie.getMaxAge() == 0));
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("refresh_token") && cookie.getMaxAge() == 0));
    }

    @Test
    @SneakyThrows
    void testPreHandle_NoAccessToken_ShouldRedirect() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        Cookie refresh = new Cookie("refresh_token", "validRefreshToken");
        when(request.getCookies()).thenReturn(new Cookie[]{refresh});

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendRedirect("/auth/login");
    }

    @Test
    @SneakyThrows
    void testPreHandle_RefreshTokenException_ShouldRedirect() {
        when(handlerMethod.getMethodAnnotation(PreAuthorize.class)).thenReturn(mock(PreAuthorize.class));
        Cookie access = new Cookie("access_token", "expiredAccessToken");
        Cookie refresh = new Cookie("refresh_token", "validRefreshToken");
        when(request.getCookies()).thenReturn(new Cookie[]{access, refresh});

        when(jwtService.isAccessTokenExpired("expiredAccessToken")).thenReturn(true);
        when(jwtService.isRefreshTokenExpired("validRefreshToken")).thenReturn(false);

        // Добавляем мок для findByRefreshToken:
        Token token = mock(Token.class);
        when(token.isRefreshTokenValid()).thenReturn(true);
        when(jwtService.findByRefreshToken("validRefreshToken")).thenReturn(token);

        when(authService.refreshToken("validRefreshToken")).thenThrow(new RuntimeException("Refresh error"));

        boolean result = jwtInterceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendRedirect("/auth/login");
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("access_token") && cookie.getMaxAge() == 0));
        verify(response).addCookie(argThat(cookie -> cookie.getName().equals("refresh_token") && cookie.getMaxAge() == 0));
    }

}
