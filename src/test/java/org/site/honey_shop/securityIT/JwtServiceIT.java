package org.site.honey_shop.securityIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceIT extends TestContainerConfig {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenRepository tokenRepository;

    @BeforeEach
    void clearDb() {
        tokenRepository.deleteAll();
    }

    @Test
    void testGenerateAccessToken_success() {
        String username = "testuser";

        String accessToken = jwtService.generateAccessToken(username);

        assertThat(accessToken).isNotBlank();
        assertThat(accessToken).contains(".");
        assertThat(accessToken).doesNotContain(" ");
    }

    @Test
    void testGenerateRefreshToken_success() {
        String username = "testuser";

        String refreshToken = jwtService.generateRefreshToken(username);

        assertThat(refreshToken).isNotBlank();
        assertThat(refreshToken).contains(".");
        assertThat(refreshToken).doesNotContain(" ");
    }

    @Test
    void testExtractUserName_success() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);

        String extractedUsername = jwtService.extractUserName(accessToken);

        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void testSaveToken_success() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);

        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        assertThat(token).isNotNull();
        assertThat(token.getAccessToken()).isEqualTo(accessToken);
        assertThat(token.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void testInvalidateToken_success() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        jwtService.invalidateToken(token);

        Token foundToken = tokenRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        assertThat(foundToken.isAccessTokenValid()).isFalse();
        assertThat(foundToken.isRefreshTokenValid()).isFalse();
    }

    @Test
    @Transactional
    void testFindByAccessToken_success() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        Token foundToken = jwtService.findByAccessToken(accessToken);

        assertThat(foundToken).isEqualTo(token);
    }

    @Test
    @Transactional
    void testFindByRefreshToken_success() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        Token foundToken = jwtService.findByRefreshToken(refreshToken);

        assertThat(foundToken).isEqualTo(token);
    }

    @Test
    void testIsRefreshTokenExpired_returnsTrue_whenExpired() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        token.setCreateDate(token.getCreateDate().minusDays(8));
        tokenRepository.save(token);

        boolean isExpired = jwtService.isRefreshTokenExpired(refreshToken);

        assertThat(isExpired).isTrue();
    }

    @Test
    void testIsAccessTokenExpired_returnsTrue_whenExpired() {
        String username = "testuser";
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        Token token = jwtService.saveToken(username, accessToken, refreshToken);

        token.setCreateDate(token.getCreateDate().minusMinutes(16));
        tokenRepository.save(token);

        boolean isExpired = jwtService.isAccessTokenExpired(accessToken);

        assertThat(isExpired).isTrue();
    }
}
