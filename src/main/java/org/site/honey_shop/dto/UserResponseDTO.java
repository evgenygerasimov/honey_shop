package org.site.honey_shop.dto;

import org.site.honey_shop.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;
import java.time.LocalDate;

public record UserResponseDTO (
     UUID userId,
     String username,
     String firstName,
     String lastName,
     String middleName,
     String email,
     String phone,
     String photo,
     LocalDate birthDate,
     Role role,
     Boolean enabled,
     LocalDateTime createDate
) {}
