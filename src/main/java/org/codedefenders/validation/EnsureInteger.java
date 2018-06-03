package org.codedefenders.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

//  https://stackoverflow.com/questions/19537664/how-to-validate-number-string-as-digit-with-hibernate
@Constraint(validatedBy = EnsureIntegerValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnsureInteger {

	String message() default "Not an integer";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
