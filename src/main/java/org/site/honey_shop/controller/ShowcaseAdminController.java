package org.site.honey_shop.controller;

import lombok.RequiredArgsConstructor;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.ShowcaseService;
import org.site.honey_shop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/showcase")
public class ShowcaseAdminController {

    private final ShowcaseService showcaseService;
    private final ProductRepository productRepository;
    private final UserService userService;

    @GetMapping
    public String showShowcaseManagementPage(Model model) {
        List<Category> categories = showcaseService.getShowcase();

        Map<Category, List<Product>> categoryProductsMap = new LinkedHashMap<>();

        for (Category category : categories) {
            List<Product> products = productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(category.getCategoryId());
            categoryProductsMap.put(category, products);
        }

        model.addAttribute("userId", getCurrentUserId());
        model.addAttribute("categoryProductsMap", categoryProductsMap);
        return "showcase";
    }

    @PostMapping("/reorder")
    @ResponseBody
    public ResponseEntity<?> reorderShowcase(@RequestBody Map<String, Object> payload) {

        List<String> categoryOrderRaw = (List<String>) payload.get("categoryOrder");
        List<UUID> categoryOrder = categoryOrderRaw.stream()
                .map(UUID::fromString)
                .toList();

        Map<String, List<String>> productOrderRaw = (Map<String, List<String>>) payload.get("productOrder");

        Map<UUID, List<UUID>> productOrder = productOrderRaw.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> UUID.fromString(e.getKey()),
                        e -> e.getValue().stream().map(UUID::fromString).toList()
                ));

        showcaseService.reorder(categoryOrder, productOrder);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        return userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .userId()
                .toString();
    }
}
