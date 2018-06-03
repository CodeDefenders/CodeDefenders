package org.codedefenders.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

// https://stackoverflow.com/questions/19537664/how-to-validate-number-string-as-digit-with-hibernate
public class EnsureFloatValidator implements ConstraintValidator<EnsureFloat, Object> {
	private EnsureFloat ensureFloat;

	@Override
	public void initialize(EnsureFloat constraintAnnotation) {
		this.ensureFloat = constraintAnnotation;
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return false;
		} else if (value instanceof String) {
			try {
				Float.parseFloat(String.valueOf(value));
				return true;
			} catch (Throwable t) {
				return false;
			}
		} else if (value instanceof Float) {
			return true;
		} else {
			return false;
		}

	}

}
