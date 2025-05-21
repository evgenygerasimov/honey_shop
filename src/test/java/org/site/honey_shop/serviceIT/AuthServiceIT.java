package org.site.honey_shop.serviceIT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.service.AuthService;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIT extends TestContainerConfig {

    @Autowired
    private AuthService authService;

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
    void testLogin_success() {
        User user = userService.save(
                User.builder()
                        .username("testuser")
                        .password("Password1!")
                        .firstName("Test")
                        .lastName("User")
                        .middleName("Middle")
                        .email("test@example.com")
                        .phone("+7 (111) 222-33-44")
                        .birthDate(LocalDate.of(2000, 1, 1))
                        .role(Role.ROLE_SUPER_ADMIN)
                        .enabled(true)
                        .build(),
                null
        );

        HttpServletResponse response = mock(HttpServletResponse.class);

        Token token = authService.login("testuser", "Password1!", response);

        assertThat(token).isNotNull();
        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();

        verify(response).addCookie(Mockito.argThat(cookie ->
                cookie.getName().equals("access_token") && cookie.getValue().equals(token.getAccessToken())));
        verify(response).addCookie(Mockito.argThat(cookie ->
                cookie.getName().equals("refresh_token") && cookie.getValue().equals(token.getRefreshToken())));
    }

    @Test
    void testLogin_withInvalidPassword_throwsException() {
        userService.save(
                User.builder()
                        .username("wrongpassuser")
                        .password("Password1!")
                        .firstName("Wrong")
                        .lastName("User")
                        .email("wrong@example.com")
                        .phone("+7 (999) 000-00-00")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .role(Role.ROLE_SUPER_ADMIN)
                        .enabled(true)
                        .build(),
                null
        );

        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> authService.login("wrongpassuser", "WrongPassword", response))
                .isInstanceOf(MyAuthenticationException.class)
                .hasMessageContaining("Неверный логин или пароль");
    }

    @Test
    void testRefreshToken_success() {
        User user = userService.save(
                User.builder()
                        .username("refreshuser")
                        .password("Password1!")
                        .firstName("Refresh")
                        .lastName("User")
                        .email("refresh@example.com")
                        .phone("+7 (111) 999-88-77")
                        .birthDate(LocalDate.of(1995, 5, 5))
                        .role(Role.ROLE_SUPER_ADMIN)
                        .enabled(true)
                        .build(),
                null
        );

        HttpServletResponse response = mock(HttpServletResponse.class);
        Token oldToken = authService.login("refreshuser", "Password1!", response);

        Token newToken = authService.refreshToken(oldToken.getRefreshToken());

        assertThat(newToken.getAccessToken()).isNotEqualTo(oldToken.getAccessToken());
        assertThat(newToken.getRefreshToken()).isNotEqualTo(oldToken.getRefreshToken());
    }

    @Test
    void testLogout_success() {
        User user = userService.save(
                User.builder()
                        .username("logoutuser")
                        .password("Password1!")
                        .firstName("Logout")
                        .lastName("User")
                        .email("logout@example.com")
                        .phone("+7 (888) 333-22-11")
                        .birthDate(LocalDate.of(1985, 3, 15))
                        .role(Role.ROLE_SUPER_ADMIN)
                        .enabled(true)
                        .build(),
                null
        );

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        Token token = authService.login("logoutuser", "Password1!", response);

        authService.logout(token.getAccessToken(), request, response);

        verify(session).invalidate();
        Optional<Token> found = tokenRepository.findByAccessToken(token.getAccessToken());
        assertThat(found.get().isAccessTokenValid()).isFalse();
    }
}
