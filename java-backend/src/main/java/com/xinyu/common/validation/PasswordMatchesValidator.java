package com.xinyu.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            String password = (String) value.getClass().getMethod("password").invoke(value);
            String confirmPassword = (String) value.getClass().getMethod("confirmPassword").invoke(value);
            boolean matches = password != null && Objects.equals(password, confirmPassword);
            if (!matches && context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode("confirmPassword")
                        .addConstraintViolation();
            }
            return matches;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return false;
        }
    }
}
