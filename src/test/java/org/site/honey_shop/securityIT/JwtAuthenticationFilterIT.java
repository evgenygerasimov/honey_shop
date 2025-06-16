package org.site.honey_shop.securityIT;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.security.JwtAuthenticationFilter;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtAuthenticationFilterIT extends TestContainerConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_withValidAccessToken_authenticatesUser() throws Exception {
        User user = User.builder()
                .username("jwtuser")
                .password("Password1!")
                .firstName("Jwt")
                .lastName("User")
                .middleName("Middle")
                .email("jwt@example.com")
                .phone("+7 (999) 999-99-99")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        jwtService.saveToken(user.getUsername(), accessToken, refreshToken);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/some-protected-endpoint");
        request.setCookies(new Cookie("access_token", accessToken));

        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("jwtuser");
        });
    }

    @Test
    void testDoFilterInternal_withExpiredAccessTokenAndValidRefreshToken_refreshesToken() throws Exception {
        User user = User.builder()
                .username("refreshuser")
                .password("Password1!")
                .firstName("Refresh")
                .lastName("User")
                .middleName("Middle")
                .email("refresh@example.com")
                .phone("+7 (888) 888-88-88")
                .birthDate(LocalDate.of(1995, 5, 5))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        String expiredAccessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        jwtService.saveToken(user.getUsername(), expiredAccessToken, refreshToken);
        jwtService.invalidateToken(jwtService.findByAccessToken(expiredAccessToken));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/some-endpoint");
        request.setCookies(
                new Cookie("access_token", expiredAccessToken),
                new Cookie("refresh_token", refreshToken)
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("refreshuser");
            assertThat(response.getCookies()).extracting("name").contains("access_token", "refresh_token");
        });
    }

    @Test
    void testDoFilterInternal_withInvalidTokens_redirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/secure");
        request.setCookies(
                new Cookie("access_token", "invalid"),
                new Cookie("refresh_token", "invalid")
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/login");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
