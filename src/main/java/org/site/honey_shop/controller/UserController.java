package org.site.honey_shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.annotation.OnCreate;
import org.site.honey_shop.annotation.OnUpdate;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.security.Principal;
import java.util.UUID;

@Controller
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/list")
    public String showUserList(Model model, HttpServletRequest request) {
        accessDeniedProcessing(request, model);
        model.addAttribute("users", userService.findAll());
        return "all-users";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping
    public String showCreateUserForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "add-user";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping
    public String createUser(@Validated(OnCreate.class) @ModelAttribute User user,
                             BindingResult bindingResult,
                             @RequestParam("image") MultipartFile image,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "add-user";
        }
        try {
            user = userService.save(user, image);
        } catch (ImageUploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users";
        }
        return "redirect:/users/" + user.getUserId();
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{userId}")
    public String showUserData(@PathVariable("userId") UUID userId, HttpServletRequest request, Model model) {
        accessDeniedProcessing(request, model);
        model.addAttribute("authUserId", getCurrentUserId());
        model.addAttribute("user", userService.findById(userId));
        return "user-data";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @GetMapping("/edit_form/{userId}")
    public String showUserEditForm(@PathVariable("userId") UUID userId,
                                   Model model,
                                   Principal principal) {
        addAttributesToModel(userId, model, principal);
        return "edit-user";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @PostMapping("/edit")
    public String updateUser(@Validated(OnUpdate.class) @ModelAttribute User user,
                             BindingResult bindingResult,
                             Principal principal,
                             @RequestParam("image") MultipartFile image,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            addAttributesToModel(user.getUserId(), model, principal);
            model.addAttribute("org.springframework.validation.BindingResult.user", bindingResult);
            return "edit-user";
        }
        try {
            userService.update(user, image, principal);
        } catch (IllegalArgumentException | MyAuthenticationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/edit_form/" + user.getUserId();
        }
        return "redirect:/users/" + user.getUserId();
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/delete/{userId}")
    public String deleteUser(@PathVariable("userId") UUID deletingUserId, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(deletingUserId);
        } catch (MyAuthenticationException e) {
            redirectAttributes.addFlashAttribute("accessDeniedMessage", "Вы не можете удалить сами себя.");
            return "redirect:/users/list";
        }
        return "redirect:/users/list";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/delete-image")
    public ResponseEntity<String> deleteImage(@RequestParam("imageFilename") String imageFilename,
                                              @RequestParam("userId") String userId) {
        File file = new File(imageFilename);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                System.out.println("Failed to deleteCategory file: " + file.getAbsolutePath());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to deleteCategory file");
            }
        }
        userService.removeImageFromUserProfile(UUID.fromString(userId));
        return ResponseEntity.ok("Photo from user profile was deleted successfully.");
    }

    private void accessDeniedProcessing(HttpServletRequest request, Model model) {
        Object accessDeniedMessage = request.getSession().getAttribute("accessDeniedMessage");
        if (accessDeniedMessage != null) {
            model.addAttribute("accessDeniedMessage", accessDeniedMessage);
            request.getSession().removeAttribute("accessDeniedMessage");
        }
    }

    @ModelAttribute("authUserId")
    public UUID authUserId() {
        if (!SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            return userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).userId();
        }
        return null;
    }

    private String getCurrentUserId() {
        return userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .userId()
                .toString();
    }

    private void addAttributesToModel(UUID userId, Model model, Principal principal) {
            UserResponseDTO currentUser = userService.findByUsername(principal.getName());
            UserResponseDTO editableUser = userService.findById(userId);

            boolean isSelfEdit = currentUser.userId().equals(editableUser.userId());
            boolean isSuperAdmin = currentUser.role() == Role.ROLE_SUPER_ADMIN;

            model.addAttribute("user", editableUser);
            model.addAttribute("authUserId", currentUser.userId());
            model.addAttribute("isSelfEdit", isSelfEdit);
            model.addAttribute("isSuperAdmin", isSuperAdmin);
    }
}
