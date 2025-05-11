package org.site.honey_shop.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.security.JwtService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private JwtService jwtService;

    private String secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Base64.getEncoder().encodeToString("my-very-secret-key-my-very-secret-key".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
    }

    @Test
    void testGenerateAccessTokenAndExtractUsername() {
        String username = "testuser";
        String token = jwtService.generateAccessToken(username);
        String extractedUsername = jwtService.extractUserName(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void testGenerateRefreshTokenAndExtractUsername() {
        String username = "testuser";
        String token = jwtService.generateRefreshToken(username);
        String extractedUsername = jwtService.extractUserName(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void testInvalidateToken() {
        Token token = new Token();
        token.setAccessTokenValid(true);
        token.setRefreshTokenValid(true);

        jwtService.invalidateToken(token);

        assertFalse(token.isAccessTokenValid());
        assertFalse(token.isRefreshTokenValid());
        verify(tokenRepository).save(token);
    }

    @Test
    void testSaveToken() {
        Token token = new Token();
        token.setUsername("user");
        token.setAccessToken("access");
        token.setRefreshToken("refresh");

        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        Token saved = jwtService.saveToken("user", "access", "refresh");

        assertEquals("user", saved.getUsername());
        assertEquals("access", saved.getAccessToken());
        assertEquals("refresh", saved.getRefreshToken());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void testFindByAccessToken() {
        Token token = new Token();
        when(tokenRepository.findByAccessToken("token123")).thenReturn(Optional.of(token));

        Token result = jwtService.findByAccessToken("token123");

        assertNotNull(result);
        verify(tokenRepository).findByAccessToken("token123");
    }

    @Test
    void testFindByRefreshToken() {
        Token token = new Token();
        when(tokenRepository.findByRefreshToken("refresh123")).thenReturn(Optional.of(token));

        Token result = jwtService.findByRefreshToken("refresh123");

        assertNotNull(result);
        verify(tokenRepository).findByRefreshToken("refresh123");
    }

    @Test
    void testIsAccessTokenExpired_ValidToken() {
        Token token = new Token();
        token.setAccessTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusMinutes(10));

        when(tokenRepository.findByAccessToken("access")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isAccessTokenExpired("access");

        assertFalse(expired);
    }

    @Test
    void testIsAccessTokenExpired_ExpiredToken() {
        Token token = new Token();
        token.setAccessTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusMinutes(20));

        when(tokenRepository.findByAccessToken("access")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isAccessTokenExpired("access");

        assertTrue(expired);
    }

    @Test
    void testIsRefreshTokenExpired_InvalidToken() {
        Token token = new Token();
        token.setRefreshTokenValid(false);
        token.setCreateDate(LocalDateTime.now());

        when(tokenRepository.findByRefreshToken("refresh")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isRefreshTokenExpired("refresh");

        assertTrue(expired);
    }

    @Test
    void testIsRefreshTokenExpired_ExpiredToken() {
        Token token = new Token();
        token.setRefreshTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusDays(8));

        when(tokenRepository.findByRefreshToken("refresh")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isRefreshTokenExpired("refresh");

        assertTrue(expired);
    }
}
