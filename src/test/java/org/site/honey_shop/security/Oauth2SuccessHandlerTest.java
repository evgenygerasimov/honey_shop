package org.site.honey_shop.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Oauth2SuccessHandlerTest {

    @InjectMocks
    private Oauth2SuccessHandler oauth2SuccessHandler;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private DefaultOAuth2User oauth2User;

    @Mock
    private UserDetails userDetails;

    @Mock
    private UserResponseDTO user;

    @Captor
    private ArgumentCaptor<Cookie> cookieCaptor;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testOnAuthenticationSuccess_SuccessfulFlow_SetsAuthenticationAndAddsCookiesAndRedirects() throws Exception {
        String username = "user@example.com";
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        Token token = new Token();
        token.setUsername(username);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);

        UUID userId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of("email", username));

        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        when(userDetails.getAuthorities()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn(username);

        when(jwtService.generateAccessToken(username)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(username)).thenReturn(refreshToken);

        when(jwtService.saveToken(username, accessToken, refreshToken)).thenReturn(token);

        when(userService.findByUsername(username)).thenReturn(user);
        when(user.userId()).thenReturn(userId);

        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertNotNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());

        verify(jwtService).generateAccessToken(username);
        verify(jwtService).generateRefreshToken(username);
        verify(jwtService).saveToken(username, accessToken, refreshToken);

        verify(response, times(2)).addCookie(cookieCaptor.capture());
        var cookies = cookieCaptor.getAllValues();

        assertTrue(cookies.stream().anyMatch(c -> "access_token".equals(c.getName()) && accessToken.equals(c.getValue()) && c.isHttpOnly() && c.getSecure() && "/".equals(c.getPath())));
        assertTrue(cookies.stream().anyMatch(c -> "refresh_token".equals(c.getName()) && refreshToken.equals(c.getValue()) && c.isHttpOnly() && c.getSecure() && "/".equals(c.getPath())));

        verify(response).sendRedirect("/users/" + userId);
    }

    @Test
    void testOnAuthenticationSuccess_UserNotFound_RedirectsToLoginWithError() throws Exception {
        String username = "user@example.com";

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of("email", username));

        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));

        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/auth/login?error=user_not_found");
        verifyNoMoreInteractions(jwtService);
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testExtractUsernameFromEmail_ThrowsIfEmailMissing() throws Exception {
        DefaultOAuth2User userWithoutEmail = mock(DefaultOAuth2User.class);
        when(userWithoutEmail.getAttributes()).thenReturn(Map.of("name", "noemail"));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userWithoutEmail);

        var method = Oauth2SuccessHandler.class.getDeclaredMethod("extractUsernameFromEmail", Authentication.class);
        method.setAccessible(true);
        assertThrows(Exception.class, () -> method.invoke(null, auth));
    }
}
