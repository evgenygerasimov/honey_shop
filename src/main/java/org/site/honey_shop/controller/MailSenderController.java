package org.site.honey_shop.controller;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Controller
@RequestMapping("/contacts")
@AllArgsConstructor
public class MailSenderController {

    private final JavaMailSender mailSender;

    @PostMapping("/send")
    @ResponseBody
    public void sendMessage(@RequestParam("name") String name,
                            @RequestParam("email") String email,
                            @RequestParam("phone") String phone,
                            @RequestParam("message") String message) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setFrom("phpaseka@gmail.com");
        helper.setTo("phpaseka@gmail.com");
        helper.setSubject("Новое сообщение с сайта");

        helper.setText(
                "От: " + name +
                        "\nEmail: " + email +
                        "\nPhone: " + phone +
                        "\nСообщение: " + message
        );

        helper.setReplyTo(email);
        mailSender.send(mimeMessage);
    }
}
