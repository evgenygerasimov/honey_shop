package org.site.honey_shop.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.service.UserService;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class AnalyticsProxyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserService userService;

    @InjectMocks
    private AnalyticsProxyController analyticsProxyController;

    private UserResponseDTO user;

    @BeforeEach
    void setUp() {
        var authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user = new UserResponseDTO(
                UUID.randomUUID(),
                "user",
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "email@example.com",
                "89012345678",
                null, null, null, null, null
        );

        when(userService.findByUsername("admin")).thenReturn(user);

        mockMvc = MockMvcBuilders
                .standaloneSetup(analyticsProxyController)
                .build();
    }

    @Test
    void testProxyToAnalytics_ForwardsRequestAndReturnsResponse() throws Exception {
        byte[] mockResponseBody = "OK".getBytes();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockResponseBody, responseHeaders, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(), any(), eq(byte[].class))).thenReturn(mockResponse);

        mockMvc.perform(get("/analytics/test-endpoint")
                        .cookie(new Cookie("access_token", "token")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mockResponseBody));
    }

    @Test
    void testProxyToAnalytics_ReturnsStatusFromAnalytics() throws Exception {
        byte[] responseBody = "Not Found".getBytes();
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(anyString(), any(), any(), eq(byte[].class))).thenReturn(mockResponse);

        mockMvc.perform(get("/analytics/missing")
                        .cookie(new Cookie("access_token", "token")))
                .andExpect(status().isNotFound())
                .andExpect(content().bytes(responseBody));
    }
}
