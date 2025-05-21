package org.site.honey_shop.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class GoogleEmailValidator implements ConstraintValidator<ValidGoogleEmail, String> {

    private static final Pattern GMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && GMAIL_PATTERN.matcher(value).matches();
    }
}
