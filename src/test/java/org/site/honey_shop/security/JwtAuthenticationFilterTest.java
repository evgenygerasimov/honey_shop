package org.site.honey_shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

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

    @Captor
    private ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_WithValidAccessToken_SetsAuthentication() throws Exception {
        String accessToken = "validToken";
        String username = "testUser";

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", accessToken)});
        when(jwtService.isAccessTokenExpired(accessToken)).thenReturn(false);
        when(jwtService.extractUserName(accessToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(null); // или замокать список ролей
        when(userDetails.getUsername()).thenReturn(username); // 🔥 добавлено!

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());

        verify(jwtService).isAccessTokenExpired(accessToken);
        verify(jwtService).extractUserName(accessToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(filterChain).doFilter(request, response);
    }


    @Test
    void testDoFilterInternal_WithNoCookies_DoesNotSetAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithExpiredToken_DoesNotSetAuthentication() throws Exception {
        String expiredToken = "expiredToken";

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", expiredToken)});
        when(jwtService.isAccessTokenExpired(expiredToken)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).isAccessTokenExpired(expiredToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithInvalidToken_ThrowsExceptionAndContinues() throws Exception {
        String invalidToken = "invalidToken";

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", invalidToken)});
        when(jwtService.isAccessTokenExpired(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).isAccessTokenExpired(invalidToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithNullUsername_DoesNotAuthenticate() throws Exception {
        String accessToken = "someToken";

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("access_token", accessToken)});
        when(jwtService.isAccessTokenExpired(accessToken)).thenReturn(false);
        when(jwtService.extractUserName(accessToken)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractUserName(accessToken);
        verify(filterChain).doFilter(request, response);
    }
}
