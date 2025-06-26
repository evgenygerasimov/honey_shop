package org.site.honey_shop.securityIT;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.security.Oauth2SuccessHandler;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Oauth2SuccessHandlerIT extends TestContainerConfig {

    @Autowired
    private Oauth2SuccessHandler successHandler;

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
    void testOnAuthenticationSuccess_shouldSaveTokensAndRedirect() throws Exception {
        String email = "oauthuser@example.com";
        User user = User.builder()
                .username(email)
                .password("Password1!")
                .firstName("OAuth")
                .lastName("User")
                .middleName("Middle")
                .email(email)
                .phone("+7 (123) 456-78-90")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        Map<String, Object> attributes = Map.of("email", email);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(authorities, attributes, "email");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(oauth2User, null, authorities);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        List<Token> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);

        Token savedToken = tokens.get(0);
        assertThat(savedToken.getUsername()).isEqualTo(email);
        assertThat(savedToken.isAccessTokenValid()).isTrue();
        assertThat(savedToken.isRefreshTokenValid()).isTrue();

        Cookie accessTokenCookie = response.getCookie("access_token");
        Cookie refreshTokenCookie = response.getCookie("refresh_token");

        assertThat(accessTokenCookie).isNotNull();
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(accessTokenCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();

        String expectedRedirect = "/users/" + user.getUserId();
        assertThat(response.getRedirectedUrl()).isEqualTo(expectedRedirect);
    }
}
