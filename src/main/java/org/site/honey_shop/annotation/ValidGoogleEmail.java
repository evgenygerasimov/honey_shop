package org.site.honey_shop.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = GoogleEmailValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGoogleEmail {

    String message() default "Логин должен быть валидным адресом Google почты";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}