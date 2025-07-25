package org.site.honey_shop.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.MainPageService;
import org.site.honey_shop.service.PaymentCashService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@AllArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;
    private final PaymentCashService paymentCashService;
    private final CategoryService categoryService;
    private final HttpSession session;

    @GetMapping
    public String showHomePage(Model model) {
        model.addAttribute("categorizedProducts", mainPageService.getCategorizedProductsSorted());
        model.addAttribute("successPayment", paymentCashService.getPaymentSuccess(session.getId()));
        model.addAttribute("categories", categoryService.findAllTrueVisibleCategories());
        return "index";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("order", new Order());
        return "checkout";
    }
}
