package org.site.honey_shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.site.honey_shop.annotation.Adult;
import org.site.honey_shop.annotation.OnCreate;
import org.site.honey_shop.annotation.OnUpdate;
import org.site.honey_shop.annotation.UniqueUserName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userId;

    @UniqueUserName(groups = {OnCreate.class})
    @NotBlank(message = "Имя пользователя не может быть пустым.")
    @Pattern(
            regexp = "^[a-zA-Z0-9]{5,20}$",
            message = "Логин должен содержать только латинские буквы и цифры, от 5 до 20 символов.",
            groups = {OnCreate.class }
    )
    @Column(name = "username")
    private String username;

    @NotBlank(message = "Пароль не может быть пустым.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,64}$",
            message = "Пароль должен быть от 8 до 64 символов и содержать строчные и заглавные латинские буквы, цифры и спецсимволы.",
            groups = {OnCreate.class}
    )
    @Column(name = "password")
    private String password;

    @Size(min = 2, max = 50, message = "Имя не должно быть менее 2 превышать 50 символов.",
    groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "first_name")
    private String firstName;

    @Size(min = 2, max = 50, message = "Фамилия не должна быть менее 2 превышать 50 символов.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "last_name")
    private String lastName;

    @Size(min = 2, max = 50, message = "Отчество не должно быть менее 2 превышать 50 символов.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "middle_name")
    private String middleName;

    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Пожалуйста, введите email в формате mail@example.com.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "email")
    private String email;

    @Pattern(regexp = "^\\+7 \\(\\d{3}\\) \\d{3}-\\d{2}-\\d{2}$",
            message = "Пожалуйста, введите номер телефона в формате +7(123)456-78-90.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "phone")
    private String phone;

    @Size(max = 255, message = "Слишком длинное имя файла изображения.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "photo")
    private String photo;

    @Adult(groups = {OnCreate.class, OnUpdate.class})
    @NotNull(message = "Дата рождения не может быть пустой.",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "enabled")
    private Boolean enabled;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Authority> authorities = new HashSet<>();

    @CreationTimestamp
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled != null && this.enabled;
    }

}
