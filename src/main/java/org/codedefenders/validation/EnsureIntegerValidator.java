package org.codedefenders.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

// https://stackoverflow.com/questions/19537664/how-to-validate-number-string-as-digit-with-hibernate
public class EnsureIntegerValidator implements ConstraintValidator<EnsureInteger, Object> {
	private EnsureInteger ensureInteger;

	@Override
	public void initialize(EnsureInteger constraintAnnotation) {
		this.ensureInteger = constraintAnnotation;
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return false;
		} else if (value instanceof String) {
			// Initialize it.
			String regex = "^\\d+$";
			String data = String.valueOf(value);
			return data.matches(regex);
			
		} else if (value instanceof Integer) {
			return true;
		} else {
			return false;
		}


	}

}
