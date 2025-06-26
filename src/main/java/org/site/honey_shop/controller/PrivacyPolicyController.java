package org.site.honey_shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/privacy-policy")
public class PrivacyPolicyController {

    @GetMapping
    public String getPrivacyPolicyPage() {
        return "privacy-policy";
    }
}
