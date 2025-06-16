package org.site.honey_shop.securityIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceIT extends TestContainerConfig {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDb() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testGenerateAndValidateTokens() {
        User user = User.builder()
                .username("jwtuser")
                .password("Password1!")
                .firstName("Jwt")
                .lastName("User")
                .middleName("Middle")
                .email("jwt@example.com")
                .phone("+7 (987) 654-32-10")
                .birthDate(LocalDate.of(1990, 3, 15))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        String extractedUsername = jwtService.extractUserName(accessToken);
        assertThat(extractedUsername).isEqualTo(user.getUsername());

        Token savedToken = jwtService.saveToken(user.getUsername(), accessToken, refreshToken);
        assertThat(savedToken.getTokenId()).isNotNull();
        assertThat(savedToken.isAccessTokenValid()).isTrue();
        assertThat(savedToken.isRefreshTokenValid()).isTrue();
    }

    @Test
    void testInvalidateToken() {
        User user = User.builder()
                .username("invalidateuser")
                .password("Password1!")
                .firstName("Invalidate")
                .lastName("User")
                .middleName("Middle")
                .email("inv@example.com")
                .phone("+7 (900) 000-00-00")
                .birthDate(LocalDate.of(1992, 6, 12))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        Token token = jwtService.saveToken(user.getUsername(), accessToken, refreshToken);

        jwtService.invalidateToken(token);

        Token invalidatedToken = tokenRepository.findById(token.getTokenId()).orElseThrow();

        assertThat(invalidatedToken.isAccessTokenValid()).isFalse();
        assertThat(invalidatedToken.isRefreshTokenValid()).isFalse();
    }

    @Test
    void testFindTokenByAccessToken() {
        User user = User.builder()
                .username("findaccessuser")
                .password("Password1!")
                .firstName("Find")
                .lastName("Access")
                .middleName("User")
                .email("findaccess@example.com")
                .phone("+7 (901) 123-45-67")
                .birthDate(LocalDate.of(1991, 7, 20))
                .role(Role.ROLE_SUPER_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        jwtService.saveToken(user.getUsername(), accessToken, refreshToken);

        Token found = jwtService.findByAccessToken(accessToken);

        assertThat(found).isNotNull();
        assertThat(found.getAccessToken()).isEqualTo(accessToken);
    }

    @Test
    void testIsAccessTokenExpiredAndInvalidReturnsFalseForFreshToken() {
        User user = User.builder()
                .username("freshuser")
                .password("Password1!")
                .firstName("Fresh")
                .lastName("User")
                .middleName("Middle")
                .email("fresh@example.com")
                .phone("+7 (902) 123-45-67")
                .birthDate(LocalDate.of(1998, 8, 8))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        jwtService.saveToken(user.getUsername(), accessToken, refreshToken);

        boolean isExpiredOrInvalid = jwtService.isAccessTokenExpiredAndInvalid(accessToken);

        assertThat(isExpiredOrInvalid).isFalse();
    }
}
