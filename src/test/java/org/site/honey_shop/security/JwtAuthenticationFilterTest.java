package org.site.honey_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.service.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Token token;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_LogoutPath_CallsLogoutAndContinuesFilterChain() throws Exception {
        String path = "/auth/logout";
        String accessTokenValue = "accessToken";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", accessTokenValue)});

        doNothing().when(authService).logout(eq(accessTokenValue), eq(request), eq(response));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(authService).logout(eq(accessTokenValue), eq(request), eq(response));
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_LogoutPath_MissingAccessToken_RedirectsToError() throws Exception {
        String path = "/auth/logout";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getContextPath()).thenReturn("/app");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        String expectedRedirect = "/app/error?errorMessage=" + URLEncoder.encode("Access token not found", StandardCharsets.UTF_8);

        verify(response).sendRedirect(expectedRedirect);
        verifyNoInteractions(authService, jwtService, userDetailsService);
        verifyNoMoreInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithValidAccessToken_AuthenticatesUserAndContinues() throws Exception {
        String path = "/some/other/path";
        String accessTokenValue = "validAccessToken";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("access_token", accessTokenValue)
        });

        when(jwtService.isAccessTokenExpiredAndInvalid(accessTokenValue)).thenReturn(false);
        when(jwtService.extractUserName(accessTokenValue)).thenReturn("username");
        when(userDetailsService.loadUserByUsername("username")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn("username");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("username", SecurityContextHolder.getContext().getAuthentication().getName());

        verify(jwtService).isAccessTokenExpiredAndInvalid(accessTokenValue);
        verify(jwtService).extractUserName(accessTokenValue);
        verify(userDetailsService).loadUserByUsername("username");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithExpiredAccessTokenAndValidRefreshToken_RefreshesTokensAndAuthenticates() throws Exception {
        String path = "/some/path";
        String expiredAccessToken = "expiredAccessToken";
        String refreshTokenValue = "validRefreshToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("access_token", expiredAccessToken),
                new Cookie("refresh_token", refreshTokenValue)
        });

        when(jwtService.isAccessTokenExpiredAndInvalid(expiredAccessToken)).thenReturn(true);
        when(jwtService.isRefreshTokenExpiredAndInvalid(refreshTokenValue)).thenReturn(false);

        when(jwtService.findByRefreshToken(refreshTokenValue)).thenReturn(token);
        when(token.isRefreshTokenValid()).thenReturn(true);

        when(authService.refreshToken(refreshTokenValue)).thenReturn(token);
        when(token.getAccessToken()).thenReturn(newAccessToken);
        when(token.getRefreshToken()).thenReturn(newRefreshToken);

        doNothing().when(jwtService).invalidateToken(token);

        when(jwtService.extractUserName(newAccessToken)).thenReturn("userRefreshed");
        when(userDetailsService.loadUserByUsername("userRefreshed")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn("userRefreshed");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("userRefreshed", SecurityContextHolder.getContext().getAuthentication().getName());

        verify(jwtService).invalidateToken(token);
        verify(authService).refreshToken(refreshTokenValue);
        verify(response, times(2)).addCookie(any(Cookie.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithExpiredAccessTokenAndNoRefreshToken_RedirectsToLogin() throws Exception {
        String path = "/path";
        String expiredAccessToken = "expiredAccessToken";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("access_token", expiredAccessToken)
        });

        when(jwtService.isAccessTokenExpiredAndInvalid(expiredAccessToken)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/auth/login");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithExpiredAccessTokenAndInvalidRefreshToken_RedirectsToLogin() throws Exception {
        String path = "/path";
        String expiredAccessToken = "expiredAccessToken";
        String refreshTokenValue = "invalidRefreshToken";

        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("access_token", expiredAccessToken),
                new Cookie("refresh_token", refreshTokenValue)
        });

        when(jwtService.isAccessTokenExpiredAndInvalid(expiredAccessToken)).thenReturn(true);
        when(jwtService.isRefreshTokenExpiredAndInvalid(refreshTokenValue)).thenReturn(false);

        when(jwtService.findByRefreshToken(refreshTokenValue)).thenReturn(token);
        when(token.isRefreshTokenValid()).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/auth/login");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithException_LogsErrorAndRedirectsToLogin() throws Exception {
        String path = "/any";
        when(request.getRequestURI()).thenReturn(path);
        when(request.getCookies()).thenThrow(new RuntimeException("Error getting cookies"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/auth/login");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_WithNoCookies_ContinuesFilterChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/other");
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
