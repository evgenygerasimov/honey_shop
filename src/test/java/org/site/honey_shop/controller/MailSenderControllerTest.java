package org.site.honey_shop.controller;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.site.honey_shop.controller.MailSenderController;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class MailSenderControllerTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    @InjectMocks
    private MailSenderController mailSenderController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        mockMvc = MockMvcBuilders.standaloneSetup(mailSenderController).build();
    }

    @Test
    void testSendMessage_Success() throws Exception {
        mockMvc.perform(post("/contacts/send")
                        .param("name", "Иван")
                        .param("email", "ivan@example.com")
                        .param("phone", "1234567890")
                        .param("message", "Привет, это тестовое сообщение"))
                .andExpect(status().isOk());

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
