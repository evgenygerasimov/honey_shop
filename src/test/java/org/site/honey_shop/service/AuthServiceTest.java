package org.site.honey_shop.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private Token token;

    @BeforeEach
    void setUp() {
        token = new Token();
        token.setUsername("testUser");
        token.setAccessToken("access-token");
        token.setRefreshToken("refresh-token");
        token.setAccessTokenValid(true);
        token.setRefreshTokenValid(true);
    }

    @Test
    void testSuccessfulLogin() {
        String username = "testUser";
        String password = "testPass";
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.getTokens()).thenReturn(new ArrayList<>());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(jwtService.generateAccessToken(username)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(username)).thenReturn(refreshToken);
        when(jwtService.saveToken(eq(username), eq(accessToken), eq(refreshToken)))
                .thenReturn(token);

        Token token = authService.login(username, password, response);

        assertEquals(username, token.getUsername());
        assertEquals(accessToken, token.getAccessToken());
        assertEquals(refreshToken, token.getRefreshToken());

        verify(response, times(2)).addCookie(any(Cookie.class));
    }

    @Test
    void testLoginWithExistingValidToken() {
        String username = "testUser";
        Token existingToken = token;
        existingToken.setAccessTokenValid(true);

        when(jwtService.getTokens()).thenReturn(List.of(existingToken));
        when(jwtService.isAccessTokenExpired(existingToken.getAccessToken())).thenReturn(false);
        when(jwtService.isRefreshTokenExpired(existingToken.getRefreshToken())).thenReturn(false);

        Token token = authService.login(username, "password", response);

        assertEquals(existingToken, token);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testLoginFailsWithInvalidCredentials() {
        String username = "badUser";
        String password = "badPass";

        when(jwtService.getTokens()).thenReturn(new ArrayList<>());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {
                });

        assertThrows(MyAuthenticationException.class, () -> {
            authService.login(username, password, response);
        });
    }

    @Test
    void testRefreshTokenSuccess() {
        String oldRefresh = "old-refresh-token";
        String newAccess = "access-token";
        String newRefresh = "refresh-token";
        String username = "testUser";

        Token storedToken = new Token();
        storedToken.setUsername(username);
        storedToken.setRefreshToken(oldRefresh);
        storedToken.setRefreshTokenValid(true);

        when(jwtService.findByRefreshToken(oldRefresh)).thenReturn(storedToken);
        when(jwtService.isRefreshTokenExpired(oldRefresh)).thenReturn(false);
        when(jwtService.generateAccessToken(username)).thenReturn(newAccess);
        when(jwtService.generateRefreshToken(username)).thenReturn(newRefresh);
        when(jwtService.saveToken(eq(username), eq(newAccess), eq(newRefresh)))
                .thenReturn(token);
        when(jwtService.extractUserName(any())).thenReturn(username);

        Token result = authService.refreshToken(oldRefresh);

        assertEquals(newAccess, result.getAccessToken());
        assertEquals(newRefresh, result.getRefreshToken());
    }

    @Test
    void testRefreshTokenExpired() {
        String expiredRefresh = "expired-refresh-token";
        String username = "user";

        Token storedToken = new Token();
        storedToken.setUsername(username);
        storedToken.setRefreshToken(expiredRefresh);
        storedToken.setRefreshTokenValid(true);

        when(jwtService.findByRefreshToken(expiredRefresh)).thenReturn(storedToken);
        when(jwtService.isRefreshTokenExpired(expiredRefresh)).thenReturn(true);
        when(jwtService.extractUserName(expiredRefresh)).thenReturn(username);

        // No new tokens will be generated
        Token result = authService.refreshToken(expiredRefresh);

        verify(jwtService).invalidateToken(storedToken);
        assertFalse(storedToken.isRefreshTokenValid());
    }

    @Test
    void testLogout() {
        when(jwtService.findByAccessToken(token.getAccessToken())).thenReturn(token);

        authService.logout(token.getAccessToken(), response);

        verify(jwtService).invalidateToken(token);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();

        assertEquals(2, cookies.size());
        for (Cookie c : cookies) {
            assertEquals(0, c.getMaxAge());
        }
    }
}
