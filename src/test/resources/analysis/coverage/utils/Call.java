package utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import utils.TestRuntimeException;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Call {
    String[] params() default {};
    Class<? extends Throwable> exception() default TestRuntimeException.class;
}
