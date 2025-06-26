package org.site.honey_shop.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID productId;

    @NotBlank(message = "Название не должно быть пустым.")
    @Size(max = 100, message = "Название не должно превышать 100 символов.")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Краткое описание не должно быть пустым.")
    @Size(max = 400, message = "Короткое описание не должно превышать 400 символов.")
    @Column(name = "short_description")
    private String shortDescription;

    @NotBlank(message = "Полное описание не должно быть пустым.")
    @Size(max = 10000, message = "Полное описание не должно превышать 10000 символов.")
    @Column(name = "description")
    private String description;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @NotNull(message = "Цена не должна быть пустой.")
    @DecimalMin(value = "0.01", message = "Цена не должна быть равна 0.")
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @NotNull(message = "Размер не должен быть пустым.")
    @DecimalMin(value = "0.01", message = "Размер не должен быть равен 0.")
    @Column(name = "length")
    private Double length;

    @NotNull(message = "Ширина не должна быть пустой.")
    @DecimalMin(value = "0.01", message = "Ширина не должна быть равна 0.")
    @Column(name = "width")
    private Double width;

    @NotNull(message = "Высота не должна быть пустой.")
    @DecimalMin(value = "0.01", message = "Высота не должна быть равна 0.")
    @Column(name = "height")
    private Double height;

    @NotNull(message = "Вес не должен быть пустым.")
    @DecimalMin(value = "0.01", message = "Вес не должен быть равен 0.")
    @Column(name = "weight")
    private Double weight;

    @NotNull(message = "Количество на складе не должно быть пустым.")
    @Min(value = 0, message = "Количество на складе должно быть равно или больше 0.")
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @ManyToOne
    @Valid
    @NotNull(message = "Создайте категорию или выберите из списка.")
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "show_in_showcase", nullable = false)
    @Builder.Default
    private boolean showInShowcase = false;

    @Column(name = "showcase_order")
    private Integer showcaseOrder;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}