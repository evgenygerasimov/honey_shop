package org.site.honey_shop.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.service.AuthService;
import org.site.honey_shop.service.UserService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testLoginGet() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testLoginPostSuccess() throws Exception {
        String username = "user";
        String password = "pass";
        String accessToken = "mock-access-token";

        Token token = new Token();
        token.setAccessToken(accessToken);

        UUID uuid = UUID.randomUUID();

        UserResponseDTO userDTO = new UserResponseDTO(
                uuid,
                "user",
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "email@example.com",
                "89012345678",
                null,
                null,
                null,
                null,
                null
        );

        when(authService.login(eq(username), eq(password), any(HttpServletResponse.class))).thenReturn(token);
        when(jwtService.extractUserName(accessToken)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(userDTO);

        mockMvc.perform(post("/auth/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + uuid.toString()));
    }

    @Test
    void testLoginPostFailure() throws Exception {
        String username = "user";
        String password = "wrongpass";

        when(authService.login(eq(username), eq(password), any(HttpServletResponse.class)))
                .thenThrow(new MyAuthenticationException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("errorMessage", "Invalid credentials"));
    }

    @Test
    void testLogout() throws Exception {
        String accessToken = "mock-access-token";

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        verify(authService, times(1)).logout(eq(accessToken), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
