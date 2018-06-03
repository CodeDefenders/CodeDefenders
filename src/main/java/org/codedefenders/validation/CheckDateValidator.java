package org.codedefenders.validation;

import java.time.format.DateTimeFormatter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

// For some reason this is broken...
public class CheckDateValidator implements ConstraintValidator<CheckDateFormat, Object> {

	private String pattern;

	@Override
	public void initialize(CheckDateFormat constraintAnnotation) {
		this.pattern = constraintAnnotation.pattern();
	}

	@Override
	public boolean isValid(Object object, ConstraintValidatorContext constraintContext) {
		if (object == null) {
			return true;
		} else if (object instanceof String) {
			try {
				DateTimeFormatter.ofPattern(pattern).parse(String.valueOf(object));
				return true;
			} catch (Throwable e) {
				return false;
			}
		} else if ( object instanceof Long){
			return true;
		}
		return false;
	}
}