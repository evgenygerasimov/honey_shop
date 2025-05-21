package org.site.honey_shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.site.honey_shop.security.JwtService;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.service.AuthService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            Token token = authService.login(username, password, response);
            UserResponseDTO user = userService.findByUsername(jwtService.extractUserName(token.getAccessToken()));
            return "redirect:/users/" + user.userId();
        } catch (MyAuthenticationException e) {

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @PostMapping("/logout")
    public String logout(@CookieValue(name = "access_token", required = false) String accessToken,
                         HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest) {
        authService.logout(accessToken, httpServletRequest, httpServletResponse);
        return "redirect:/auth/login";
    }
}