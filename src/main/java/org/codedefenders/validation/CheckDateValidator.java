package org.codedefenders.validation;

import java.time.format.DateTimeFormatter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckDateValidator implements ConstraintValidator<CheckDateFormat, Object> {

	// Those are checked with AT LEAST semantics, i.e., they are composed with
	// OR
	private String[] patterns;

	@Override
	public void initialize(CheckDateFormat constraintAnnotation) {
		this.patterns = constraintAnnotation.patterns();
	}

	@Override
	public boolean isValid(Object object, ConstraintValidatorContext constraintContext) {
		if (object == null) {
			return true;
		} else if (object instanceof String) {

			for (String pattern : patterns) {
				try {
					DateTimeFormatter.ofPattern(pattern).parse(String.valueOf(object));
					return true;
				} catch (Throwable e) {
					// We do not care about this one, since there might be more than one format
				}
			}
			return false;
		} else if (object instanceof Long) {
			return true;
		}
		return false;
	}
}