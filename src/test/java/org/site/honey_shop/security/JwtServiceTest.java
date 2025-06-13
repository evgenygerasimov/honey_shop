package org.site.honey_shop.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private ApplicationContext context;

    @Mock
    private UserService userService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secretKey = Base64.getEncoder().encodeToString("my-very-secret-key-my-very-secret-key".getBytes());
        jwtService = new JwtService(userService, context, tokenRepository);
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testGenerateAccessTokenAndExtractUsername() {

        lenient().when(context.getBean(UserDetailsService.class)).thenReturn(userDetailsService);

        String username = "testuser";

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(userDetails.getAuthorities()).thenAnswer(invocation -> {
            return Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        });

        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        when(userService.findByUsername(username)).thenReturn(new UserResponseDTO(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "testuser",
                "John",
                "Doe",
                "Michael",
                "testuser@example.com",
                "+1234567890",
                "photo.jpg",
                LocalDate.of(1990, 1, 1),
                Role.ROLE_ADMIN,
                true,
                LocalDateTime.now()
        ));

        String token = jwtService.generateAccessToken(username);
        assertNotNull(token);

        String extractedUsername = jwtService.extractUserName(token);
        assertEquals(username, extractedUsername);
        verify(context).getBean(UserDetailsService.class);
    }


    @Test
    void testGenerateRefreshTokenAndExtractUsername() {
        String username = "testuser";

        String token = jwtService.generateRefreshToken(username);
        assertNotNull(token);

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
        token.setAccessTokenValid(true);
        token.setRefreshTokenValid(true);

        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        Token saved = jwtService.saveToken("user", "access", "refresh");

        assertEquals("user", saved.getUsername());
        assertEquals("access", saved.getAccessToken());
        assertEquals("refresh", saved.getRefreshToken());
        assertTrue(saved.isAccessTokenValid());
        assertTrue(saved.isRefreshTokenValid());

        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void testFindByAccessToken_Found() {
        Token token = new Token();
        when(tokenRepository.findByAccessToken("token123")).thenReturn(Optional.of(token));

        Token result = jwtService.findByAccessToken("token123");

        assertNotNull(result);
        verify(tokenRepository).findByAccessToken("token123");
    }

    @Test
    void testFindByAccessToken_NotFound() {
        when(tokenRepository.findByAccessToken("token123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jwtService.findByAccessToken("token123"));
    }

    @Test
    void testFindByRefreshToken_Found() {
        Token token = new Token();
        when(tokenRepository.findByRefreshToken("refresh123")).thenReturn(Optional.of(token));

        Token result = jwtService.findByRefreshToken("refresh123");

        assertNotNull(result);
        verify(tokenRepository).findByRefreshToken("refresh123");
    }

    @Test
    void testFindByRefreshToken_NotFound() {
        when(tokenRepository.findByRefreshToken("refresh123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> jwtService.findByRefreshToken("refresh123"));
    }

    @Test
    void testIsAccessTokenExpiredAndInvalid_NotExpiredAndValid() {
        Token token = new Token();
        token.setAccessTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByAccessToken("access")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isAccessTokenExpiredAndInvalid("access");

        assertFalse(expired);
    }

    @Test
    void testIsAccessTokenExpiredAndInvalid_Expired() {
        Token token = new Token();
        token.setAccessTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusMinutes(16)); // больше 15 мин

        when(tokenRepository.findByAccessToken("access")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isAccessTokenExpiredAndInvalid("access");

        assertTrue(expired);
    }

    @Test
    void testIsAccessTokenExpiredAndInvalid_InvalidFlag() {
        Token token = new Token();
        token.setAccessTokenValid(false);
        token.setCreateDate(LocalDateTime.now());

        when(tokenRepository.findByAccessToken("access")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isAccessTokenExpiredAndInvalid("access");

        assertTrue(expired);
    }

    @Test
    void testIsRefreshTokenExpiredAndInvalid_InvalidFlag() {
        Token token = new Token();
        token.setRefreshTokenValid(false);
        token.setCreateDate(LocalDateTime.now());

        when(tokenRepository.findByRefreshToken("refresh")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isRefreshTokenExpiredAndInvalid("refresh");

        assertTrue(expired);
    }

    @Test
    void testIsRefreshTokenExpiredAndInvalid_Expired() {
        Token token = new Token();
        token.setRefreshTokenValid(true);
        token.setCreateDate(LocalDateTime.now().minusDays(8)); // больше 7 дней

        when(tokenRepository.findByRefreshToken("refresh")).thenReturn(Optional.of(token));

        boolean expired = jwtService.isRefreshTokenExpiredAndInvalid("refresh");

        assertTrue(expired);
    }
}
