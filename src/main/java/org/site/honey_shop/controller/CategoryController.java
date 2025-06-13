package org.site.honey_shop.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.UUID;

@Controller
@RequestMapping("/categories")
@AllArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public String showCategories(Model model, @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10")  int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updateDate").descending());
        Page<Category> categoryPage = categoryService.findAll(pageable);
        model.addAttribute("authUserId", getCurrentUserId());
        model.addAttribute("categoriesPage", categoryPage);
        return "all-categories";
    }

    @GetMapping("/new")
    public String showCategoryForm(Model model) {
        model.addAttribute("authUserId", getCurrentUserId());
        model.addAttribute("category", new Category());
        return "add-category";
    }

    @PostMapping
    public String createCategoryByName(@Valid @ModelAttribute Category category,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("authUserId", getCurrentUserId());
            return "add-category";
        }
        try {
            categoryService.saveCategoryByName(category.getName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/categories/new";
        }
        return "redirect:/categories";
    }

    @PostMapping("/create-with-image")
    public String fullCreateCategory(@Valid @ModelAttribute Category category,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes,
                                     Model model,
                                     @RequestParam("image") MultipartFile image) {
        if (result.hasErrors()) {
            model.addAttribute("authUserId", getCurrentUserId());
            return "add-category";
        }
        try {
            categoryService.saveCategoryWithImage(category, image);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/categories/new";
        }
        return "redirect:/categories";
    }

    @PostMapping("/update-category-name/{id}")
    public String updateCategoryName(@PathVariable UUID id, @RequestParam String name) {
        Category category = categoryService.findById(id);

        category.setName(name);
        categoryService.updateCategoryName(category);

        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable UUID id, Model model) {
        model.addAttribute("authUserId", getCurrentUserId());
        model.addAttribute("category", categoryService.findById(id));
        return "edit-category";
    }

    @PostMapping("/full-update-category")
    public String fullUpdateCategory(@Valid @ModelAttribute Category category,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes,
                                     Model model,
                                     @RequestParam("image") MultipartFile image) {
        if (result.hasErrors()) {
            model.addAttribute("authUserId", getCurrentUserId());
            return "edit-category";
        }
        try {
            categoryService.fullUpdateCategory(category, image);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/categories/edit/" + category.getCategoryId();
        }

        return "redirect:/categories";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }

    @PostMapping("/delete-image")
    public ResponseEntity<String> deleteImage(@RequestParam("imageFilename") String imageFilename,
                                              @RequestParam("categoryId") String categoryId) {
        File file = new File(imageFilename);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                System.out.println("Failed to deleteCategory file: " + file.getAbsolutePath());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to deleteCategory file");
            }
        }
        categoryService.removeImageFromCategory(UUID.fromString(categoryId));
        return ResponseEntity.ok("Photo from category was deleted successfully.");
    }


    private String getCurrentUserId() {
        return userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .userId()
                .toString();
    }
}
