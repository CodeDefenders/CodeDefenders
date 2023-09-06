package org.codedefenders.util;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.jboss.weld.junit4.WeldInitiator;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Wrapper around the JUnit 4 WeldInitiator rule to use it with JUnit 5.
 *
 * <p>Using the Weld Junit 5 Extension doesn't work (yet), since new versions depends on Jakarta, and old versions
 * depend on old versions of JUnit 5.
 */
public class WeldExtension implements InvocationInterceptor {
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {

        // Get JUnit4 WeldInitiator
        WeldInitiator weld = Arrays.stream(invocationContext.getTargetClass().getFields())
                .filter(field -> field.isAnnotationPresent(WeldSetup.class))
                .filter(field -> field.getType().equals(WeldInitiator.class))
                .map(field -> {
                    try {
                        return (WeldInitiator) field.get(invocationContext.getTarget().get());
                    } catch (IllegalAccessException e) {
                        throw new TestInstantiationException("Invalid @WeldSetup.", e);
                    }
                })
                .findAny()
                .orElseThrow(() -> new TestInstantiationException("Couldn't find any @WeldSetup."));

        // Create JUnit4 context from JUnit5 context
        Description description = Description.createSuiteDescription(invocationContext.getTargetClass());
        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                invocation.proceed();
            }
        };

        // Execute the WeldInitiator rule
        weld.apply(statement, description).evaluate();
    }
}
