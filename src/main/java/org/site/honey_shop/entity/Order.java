package org.site.honey_shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID orderId;

    @NotBlank(message = "Пожалуйста, укажите имя.")
    @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов.")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Пожалуйста, укажите фамилию.")
    @Size(min = 2, max = 100, message = "Фамилия должна содержать от 2 до 100 символов.")
    @Column(name = "last_name", nullable = false)
    private String lastName;


    @Column(name = "middle_name")
    private String middleName;

    @Pattern(
            regexp = "^(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Пожалуйста, введите email в формате mail@example.com.")
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @NotBlank(message = "Пожалуйста, выберите адрес доставки.")
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Pattern(
            regexp = "^\\+7 \\(\\d{3}\\) \\d{3}-\\d{2}-\\d{2}$",
            message = "Пожалуйста, введите номер телефона в формате +7(123)456-78-90."
    )
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @DecimalMin(value = "0.01", inclusive = true, message = "Пожалуйста вернитесь в магазин и добавьте товары в корзину.")
    @NotNull(message = "Пожалуйста вернитесь в магазин и добавьте товары в корзину.")
    @Column(name = "product_amount", nullable = false)
    private BigDecimal productAmount;

    @DecimalMin(value = "0.01", inclusive = true, message = "Пожалуйста выберитете тип и адрес доставки.")
    @NotNull(message = "Пожалуйста выберитете тип и адрес доставки.")
    @Column(name = "delivery_amount", nullable = false)
    private BigDecimal deliveryAmount;

    @DecimalMin(value = "0.01", inclusive = true, message = "Пожалуйста вернитесь в магазин и добавьте товары в корзину, затем выберите тип и адрес доставки.")
    @NotNull(message = "Пожалуйста вернитесь в магазин и добавьте товары в корзину, затем выберите тип и адрес доставки.")
    @Column(name = "total_order_amount", nullable = false)
    private BigDecimal totalOrderAmount;

    @Column(name = "delivery_type", nullable = false)
    private String deliveryType;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}