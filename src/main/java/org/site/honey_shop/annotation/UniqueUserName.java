package org.site.honey_shop.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueUsernameValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUserName {

    String message() default "Пользователь с таким логином уже существует.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
