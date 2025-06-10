package org.site.honey_shop.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorControllerImpl implements ErrorController {

    @GetMapping("/error")
    public String handleError(
            @RequestParam(name = "errorMessage", required = false) String errorMessage,
            Model model) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        } else {
            model.addAttribute("errorMessage", "Страница не найдена или произошла ошибка.");
        }
        return "error";
    }
}