package org.site.honey_shop.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.DeleteProductException;
import org.site.honey_shop.exception.ProductCreationException;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.ProductService;
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
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public String listProducts(Model model, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("updateDate").descending());
        Page<Product> ordersPage = productService.getAllProducts(pageable);

        model.addAttribute("productsPage", ordersPage);
        model.addAttribute("authUserId", getCurrentUserId());
        return "all-products";
    }

    @GetMapping("/{productId}")
    public String showProduct(@PathVariable UUID productId, Model model) {
        model.addAttribute("product", productService.getProductById(productId));
        return "product-data";
    }

    @GetMapping("/new")
    public String createProduct(Model model, Principal principal) {
        addAttributesToModel(model, principal);
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "add-product";
    }

    @PostMapping
    public String saveProduct(
            @Valid @ModelAttribute Product product,
            BindingResult bindingResult,
            @RequestParam("pictures") List<MultipartFile> pictures,
            @RequestParam("imageOrder") String imageOrder, // Порядок изображений
            Model model,
            Principal principal) {
        if (bindingResult.hasErrors()) {
            addAttributesToModel(model, principal);
            return "add-product";
        }
        try {
            productService.createProduct(product, pictures, imageOrder);
        } catch (ProductCreationException e){
            model.addAttribute("errorMessage", e.getMessage());
            addAttributesToModel(model, principal);
            return "add-product";
        }
        return "redirect:/products";
    }

    @GetMapping("/edit_form/{productId}")
    public String updateProductForm(@PathVariable("productId") UUID productId, Model model, Principal principal) {
        addAttributesToModel(model, principal);
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("category", productService.getProductById(productId).getCategory());
        return "edit-product";
    }

    @PostMapping("/edit")
    public String updateProduct(@Valid @ModelAttribute Product product,
                                BindingResult bindingResult,
                                @RequestParam("pictures") List<MultipartFile> pictures,
                                @RequestParam("imageOrder") String imageOrder,
                                Model model,
                                Principal principal) {
        if (bindingResult.hasErrors()) {
            addAttributesToModel(model, principal);
            return "edit-product";
        }

        try {
            productService.updateProduct(product, pictures, imageOrder);
        } catch (ProductCreationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addAttributesToModel(model, principal);
            return "edit-product";
        }
        return "redirect:/products";
    }

    @PostMapping("/delete/{productId}")
    public String deleteProduct(@PathVariable UUID productId, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(productId);
        } catch (DeleteProductException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products";
        }

        return "redirect:/products";
    }

    @PostMapping("/delete-image")
    public ResponseEntity<String> deleteImage(@RequestParam("imageFilename") String imageFilename,
                                              @RequestParam("productId") String productId) {
        File file = new File(imageFilename);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                System.out.println("Failed to deleteCategory file: " + file.getAbsolutePath());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to deleteCategory file");
            }
        }
        productService.removeImageFromProduct(UUID.fromString(productId), imageFilename);
        return ResponseEntity.ok("Product deleted successfully");
    }

    private void addAttributesToModel(Model model, Principal principal) {
        UserResponseDTO currentUser = userService.findByUsername(principal.getName());
        model.addAttribute("authUserId", currentUser.userId());
        model.addAttribute("categories", categoryService.findAll());
    }

    private String getCurrentUserId() {
        return userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .userId()
                .toString();
    }
}