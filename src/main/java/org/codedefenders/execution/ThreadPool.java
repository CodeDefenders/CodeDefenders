package org.codedefenders.execution;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
 
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
 
/**
 * This is taken from:
 * 
 * https://rmannibucau.wordpress.com/2016/02/29/cdi-replace-the-configuration-by-a-register-pattern/
 */
@Qualifier
@Target({METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ThreadPool {
    /**
     * @return the name of the pool to use.
     */
    @Nonbinding
    String value();
}