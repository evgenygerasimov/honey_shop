package org.site.honey_shop.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.site.honey_shop.dto.OrderDTO;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.*;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.service.OrderService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")    @GetMapping("/{orderId}")
    public String order(@PathVariable UUID orderId, Model model) {
        OrderDTO order = orderService.findOrderDTOById(orderId);

        List<Product> products = order.getOrderItems().stream()
                .map(OrderItem::getProduct)
                .toList();

        Map<UUID, Product> productsMap = products
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        model.addAttribute("order", order);
        model.addAttribute("productsMap", productsMap);
        return "order-details";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")    @GetMapping
    public String orders(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserResponseDTO user = userService.findByUsername(auth.getName());
        model.addAttribute("userId", user.userId());
        model.addAttribute("orders", orderService.findAll());
        return "all-orders";
    }

    @PostMapping
    public String createOrder(@Valid @ModelAttribute("order") Order order,
                              BindingResult result,
                              @RequestParam("orderItemsData") String orderItemsJson,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            System.out.println("Ошибки валидации:");
            result.getAllErrors().forEach(e -> System.out.println(e.toString()));
            return "checkout";
        }
        Order newOrder;
        try {
            newOrder = orderService.save(order, orderItemsJson);
        }
        catch (OrderCreateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
        return "redirect:/payments/new?order_id=" + newOrder.getOrderId();

    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @PostMapping("/update-order-status/{orderId}")
    public String updateOrder(@PathVariable String orderId,
                              @RequestParam OrderStatus orderStatus) {
        orderService.updateOrderStatus(orderService.findById(UUID.fromString(orderId)), orderStatus);
        return "redirect:/orders";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @PostMapping("/update-payment-status/{orderId}")
    public String updateOrder(@PathVariable String orderId,
                              @RequestParam PaymentStatus orderPaymentStatus) {
        orderService.updateOrderPaymentStatus(orderService.findById(UUID.fromString(orderId)), orderPaymentStatus);
        return "redirect:/orders";
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')")
    @PostMapping("/delete/{orderId}")
    public String deleteOrder(@PathVariable UUID orderId) {
        orderService.delete(orderId);
        return "redirect:/orders";
    }
}
