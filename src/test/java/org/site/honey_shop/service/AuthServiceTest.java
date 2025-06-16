package org.site.honey_shop.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

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

    @Mock
    private HttpServletRequest request;

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

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(jwtService.generateAccessToken(username)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(username)).thenReturn(refreshToken);
        when(jwtService.saveToken(username, accessToken, refreshToken)).thenReturn(token);

        Token result = authService.login(username, password, response);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(refreshToken, result.getRefreshToken());

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        assertEquals(2, cookies.size());

        Cookie accessCookie = cookies.stream().filter(c -> "access_token".equals(c.getName())).findFirst().orElse(null);
        Cookie refreshCookie = cookies.stream().filter(c -> "refresh_token".equals(c.getName())).findFirst().orElse(null);

        assertNotNull(accessCookie);
        assertEquals(accessToken, accessCookie.getValue());
        assertTrue(accessCookie.isHttpOnly());
        assertEquals("/", accessCookie.getPath());

        assertNotNull(refreshCookie);
        assertEquals(refreshToken, refreshCookie.getValue());
        assertTrue(refreshCookie.isHttpOnly());
        assertEquals("/", refreshCookie.getPath());
    }

    @Test
    void testLoginFailsWithInvalidCredentials() {
        String username = "badUser";
        String password = "badPass";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("Bad credentials") {});

        MyAuthenticationException ex = assertThrows(MyAuthenticationException.class, () -> {
            authService.login(username, password, response);
        });

        assertEquals("Неверный логин или пароль", ex.getMessage());
    }

    @Test
    void testRefreshTokenSuccessAndExpiredHandled() {
        String oldRefresh = "old-refresh-token";
        String username = "testUser";
        String newAccess = "new-access-token";
        String newRefresh = "new-refresh-token";

        Token storedToken = new Token();
        storedToken.setUsername(username);
        storedToken.setRefreshToken(oldRefresh);
        storedToken.setRefreshTokenValid(true);

        when(jwtService.findByRefreshToken(oldRefresh)).thenReturn(storedToken);
        when(jwtService.isRefreshTokenExpiredAndInvalid(oldRefresh)).thenReturn(true);
        when(jwtService.generateAccessToken(username)).thenReturn(newAccess);
        when(jwtService.generateRefreshToken(username)).thenReturn(newRefresh);
        when(jwtService.saveToken(username, newAccess, newRefresh)).thenReturn(token);
        when(jwtService.extractUserName(oldRefresh)).thenReturn(username);

        Token result = authService.refreshToken(oldRefresh);

        verify(jwtService).invalidateToken(storedToken);
        assertFalse(storedToken.isRefreshTokenValid());

        assertNotNull(result);
        assertEquals(token, result);
    }

    @Test
    void testRefreshTokenNotExpired() {
        String oldRefresh = "refresh-token";
        String username = "testUser";
        String newAccess = "new-access-token";
        String newRefresh = "new-refresh-token";

        Token storedToken = new Token();
        storedToken.setUsername(username);
        storedToken.setRefreshToken(oldRefresh);
        storedToken.setRefreshTokenValid(true);

        when(jwtService.findByRefreshToken(oldRefresh)).thenReturn(storedToken);
        when(jwtService.isRefreshTokenExpiredAndInvalid(oldRefresh)).thenReturn(false);
        when(jwtService.generateAccessToken(username)).thenReturn(newAccess);
        when(jwtService.generateRefreshToken(username)).thenReturn(newRefresh);
        when(jwtService.saveToken(username, newAccess, newRefresh)).thenReturn(token);

        Token result = authService.refreshToken(oldRefresh);

        verify(jwtService, never()).invalidateToken(storedToken);

        assertNotNull(result);
        assertEquals(token, result);
    }

    @Test
    void testLogout() {
        when(jwtService.findByAccessToken(token.getAccessToken())).thenReturn(token);

        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        authService.logout(token.getAccessToken(), request, response);

        verify(jwtService).invalidateToken(token);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        List<Cookie> cookies = cookieCaptor.getAllValues();
        assertEquals(2, cookies.size());

        for (Cookie c : cookies) {
            assertNull(c.getValue());
            assertEquals(0, c.getMaxAge());
            assertTrue(c.isHttpOnly());
            assertEquals("/", c.getPath());
        }

        verify(request.getSession()).invalidate();
    }
}
