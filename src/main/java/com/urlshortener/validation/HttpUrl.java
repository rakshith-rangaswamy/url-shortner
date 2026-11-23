package com.urlshortener.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be an absolute http:// or https:// URL with a host. Null is allowed (use @NotBlank). */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {
    String message() default "must be an absolute http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
