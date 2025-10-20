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

import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.Extension;

import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.misc.WeldInit;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *        {@code @Produces} ConfigurationSource config = new ConfigurationSourceBuilder().
 *            .withProperty("dataDir", "/tmp");
 *
 *        {@code @Produces @Alternative}
 *        public PlayerRepository playerRepo() {
 *            return new PlayerRepository(...);
 *        }
 *    }
 * </pre>
 *
 * <p> Configuration sources are excluded from bean discovery. Use
 * {@link org.codedefenders.util.config.ConfigurationSourceBuilder} for setting config values.
 *
 * <p>This extension inherits from {@link WeldJunit5AutoExtension}.
 * See https://github.com/weld/weld-testing/blob/master/junit6/README.md#weldjunit5autoextension for documentation.
 */
public class WeldExtension extends WeldJunit5AutoExtension implements Extension {
    private static final Logger logger = LoggerFactory.getLogger(WeldInit.class);

    @Override
    protected void weldInit(ExtensionContext context, Weld weld, WeldInitiator.Builder weldInitiatorBuilder) {
        super.weldInit(context, weld, weldInitiatorBuilder);

        var log = new StringJoiner("\n");
        log.add("Initializing Weld with the following beans and alternatives:");

        // Add test class as bean
        weld.addBeanClass(context.getRequiredTestClass());
        log.add("- bean: " + context.getRequiredTestClass());

        // Add test class as alternative
        if (WeldInit.containsAnnotation(context.getRequiredTestClass(), Set.of(Alternative.class), false)) {
            weld.addAlternative(context.getRequiredTestClass());
            log.add("- alternative: " + context.getRequiredTestClass());
        }

        // Add beans
        var beanClasses = WeldInit.scanBeans("org.codedefenders")
            .stream()
            .filter(clazz -> !ConfigurationSource.class.isAssignableFrom(clazz))
            .toArray(Class<?>[]::new);
        weld.addBeanClasses(beanClasses);
        Arrays.stream(beanClasses).map(clazz -> "- bean: " + clazz.getName()).forEach(log::add);

        logger.debug(log.toString());
    }
}
