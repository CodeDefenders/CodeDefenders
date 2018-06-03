package org.codedefenders.validation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = ConsistentDateParameterValidator.class)
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsistentDateParameters {
 
    String message() default
      "End date must be after begin date and both must be in the future";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
}