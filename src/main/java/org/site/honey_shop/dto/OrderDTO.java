package org.site.honey_shop.dto;

import lombok.*;
import org.site.honey_shop.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private UUID orderId;
    private String fullName;
    private String customerEmail;
    private String deliveryAddress;
    private String customerPhone;
    private BigDecimal productAmount;
    private BigDecimal deliveryAmount;
    private BigDecimal totalOrderAmount;
    private String deliveryType;
    private List<OrderItem> orderItems;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
}