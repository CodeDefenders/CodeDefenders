/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.util;

import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.Extension;

import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.misc.WeldInit;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Starts up a Weld SE container and adds all annotated beans from the classpath.
 *
 * <p> Beans declared in the test will be taken into account, however, only
 * top-level producer fields and methods will be recognized as bean producers.
 * Annotate a bean with {@link Alternative} to override the default implementation.
 *
 * <pre>
 *    {@code @ExtendWith}({MockitoExtension.class, WeldExtension.class})
 *    public class Test {
 *        {@code @Mock @Produces @Alternative} UserRepository userRepo;
 *
 *        {@code @Produces @Alternative}
 *        public Configuration config() {
 *            return new Configuration(...);
 *        }
 *    }
 * </pre>
 */
public class WeldExtension extends WeldJunit5AutoExtension implements Extension {
    @Override
    protected void weldInit(ExtensionContext context, Weld weld, WeldInitiator.Builder weldInitiatorBuilder) {
        super.weldInit(context, weld, weldInitiatorBuilder);
        weld.addBeanClass(context.getRequiredTestClass());
        if (WeldInit.containsAnnotation(context.getRequiredTestClass(), Set.of(Alternative.class), false)) {
            weld.addAlternative(context.getRequiredTestClass());
        }
        var beanClasses = WeldInit.scanBeans("org.codedefenders")
            .stream()
            .filter(clazz -> !ConfigurationSource.class.isAssignableFrom(clazz))
            .toArray(Class<?>[]::new);
        weld.addBeanClasses(beanClasses);
    }
}
