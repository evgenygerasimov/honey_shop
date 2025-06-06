package org.site.honey_shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.site.honey_shop.annotation.OnCreate;
import org.site.honey_shop.annotation.OnUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID categoryId;

    @NotBlank(message = "Название категории не может быть пустым!")
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 255, message = "Слишком длинное имя файла изображения.")
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "visible", nullable = false)
    private Boolean visible;

    @Column(name = "showcase_order")
    private Integer showcaseOrder;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}