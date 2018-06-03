package org.codedefenders.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;

@SupportedValidationTarget(ValidationTarget.PARAMETERS)
public class ConsistentDateParameterValidator 
  implements ConstraintValidator<ConsistentDateParameters, Object[]> {
 
    @Override
    public boolean isValid(
      Object[] value, 
      ConstraintValidatorContext context) {
         
    	System.out.println("Validating correct date "+ value[12]  + " -- " +  value[13] );
        if (value[12] == null || value[13] == null) {
            return false;
        }
        
        // FIXME
        // Parse to date if possible and then compare with before/after
        return true;
// 
//        if (!(value[0] instanceof LocalDate) 
//          || !(value[1] instanceof LocalDate)) {
//            throw new IllegalArgumentException(
//              "Illegal method signature, expected two parameters of type LocalDate.");
//        }
// 
//        return ((LocalDate) value[0]).isAfter(LocalDate.now()) 
//          && ((LocalDate) value[0]).isBefore((LocalDate) value[1]);
    }
}