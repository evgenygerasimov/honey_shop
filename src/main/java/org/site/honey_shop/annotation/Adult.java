package org.site.honey_shop.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Constraint(validatedBy = AdultValidator.class)
public @interface Adult {

    String message() default "Пользователь должен быть старше 18 лет.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
