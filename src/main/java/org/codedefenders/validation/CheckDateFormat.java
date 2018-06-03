package org.codedefenders.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = CheckDateValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckDateFormat {

    String message() default "Invalid date format";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    String pattern();

}