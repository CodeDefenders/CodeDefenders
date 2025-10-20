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
package org.codedefenders.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.codedefenders.configuration.source.ConfigurationSource;
import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helps with initializing Weld in absence of Tomcat. */
public class WeldInit {
    private static final Logger logger = LoggerFactory.getLogger(WeldInit.class);

    /** Annotations that mark a class, method or field as a bean producer. */
    private static final Set<Class<? extends Annotation>> beanAnnotations = new HashSet<>();
    static {
        // Jakarta's bean defining annotations
        // See: https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0.html#bean_defining_annotations
        beanAnnotations.add(ApplicationScoped.class);
        beanAnnotations.add(RequestScoped.class);
        beanAnnotations.add(SessionScoped.class);
        beanAnnotations.add(ConversationScoped.class);
        beanAnnotations.add(Interceptor.class);
        beanAnnotations.add(Stereotype.class);
        beanAnnotations.add(Dependent.class);
        // Additional bean annotations
        beanAnnotations.add(Singleton.class);
        beanAnnotations.add(Produces.class);
        beanAnnotations.add(Alternative.class);
        beanAnnotations.add(Priority.class);
        beanAnnotations.add(Typed.class);
        beanAnnotations.add(Named.class);
    }

    /** Checks if any annotations match. */
    private static boolean hasMatch(Annotation[] annotations, Set<Class<? extends Annotation>> searchedAnnotations) {
        for (var annotation : annotations) {
            if (searchedAnnotations.contains(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given class, or any of its fields or methods contain a matching annotation.
     * @param recursive Whether to include sub-classes in the search.
     */
    public static boolean containsAnnotation(Class<?> clazz, Set<Class<? extends Annotation>> searchedAnnotations, boolean recursive) {
        if (hasMatch(clazz.getAnnotations(), searchedAnnotations)) {
            return true;
        }
        for (var method : clazz.getDeclaredMethods()) {
            if (hasMatch(method.getAnnotations(), searchedAnnotations)) {
                return true;
            }
        }
        for (var field : clazz.getDeclaredFields()) {
            if (hasMatch(field.getAnnotations(), searchedAnnotations)) {
                return true;
            }
        }
        if (recursive) {
            for (var innerClass : clazz.getDeclaredClasses()) {
                if (containsAnnotation(innerClass, searchedAnnotations, recursive)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Scans package for classes that are beans or contain bean producers.
     * Excludes test package classes.
     * @param packageName The package to scan, e.g. "org.codedefenders".
     */
    public static List<Class<?>> scanBeans(String packageName) {
        return scanClasspath(packageName).stream()
                .filter(clazz -> containsAnnotation(clazz, beanAnnotations, true)).toList();
    }

    /**
     * Finds all classes in a package.
     * Excludes test package classes.
     * @param packageName The package to scan, e.g. "org.codedefenders".
     */
    public static List<Class<?>> scanClasspath(String packageName) {
        Acc acc = new Acc(new ArrayList<>(), new HashSet<>());
        scanPackage(packageName, acc);
        return acc.classes;
    }

    private record Acc(List<Class<?>> classes, Set<String> seenPackages) {}

    private static void scanPackage(String packageName, Acc acc) {
        // Prevent scanning package multiple times if multiple class file location exist.
        if (!acc.seenPackages.add(packageName)) {
            return;
        }

        Enumeration<URL> resources;
        try {
            resources = WeldInit.class.getClassLoader()
                    .getResources(packageName.replaceAll("[.]", "/"));
        } catch (IOException e) {
            throw new ClassDiscoveryException(e);
        }

        // The package name is fixed here, but it can correspond to multiple class file locations.
        // For example, "target/classes/org/codedefenders" and "target/test-classes/org/codedefenders".
        while (resources.hasMoreElements()) {
            var url = resources.nextElement();
            // Filter out test classes via their location.
            // This might break in the future, but will probably not be needed.
            if (url.toString().contains("test-classes")) {
                continue;
            }
            scanDirectory(url, packageName, acc);
        }
    }

    private static void scanDirectory(URL directory, String packageName, Acc acc) {
        InputStream stream;
        try {
            stream = directory.openStream();
        } catch (IOException e) {
            throw new ClassDiscoveryException(e);
        }
        if (stream == null) {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        reader.lines().forEach(filename -> {
            if (filename.endsWith(".class")) {
                loadClass(filename, packageName).ifPresent(acc.classes::add);
            } else {
                scanPackage(packageName + "." + filename, acc);
            }
        });
    }

    private static Optional<Class<?>> loadClass(String filename, String packageName) {
        try {
            return Optional.of(Class.forName(packageName + "." + filename.substring(0, filename.lastIndexOf('.'))));
        } catch (ClassNotFoundException e) {
            throw new ClassDiscoveryException(e);
        }
    }

    public static class ClassDiscoveryException extends RuntimeException {
        public ClassDiscoveryException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Initializes a Weld SE container and adds all Code Defenders bean classes to it.
     * @param alternatives Classes containing bean alternatives to enable.
     * @param includeConfig Whether to include {@link ConfigurationSource} beans in the scanning.
     */
    public static SeContainer initWeld(Class<?>[] alternatives, boolean includeConfig) {
        var log = new StringJoiner("\n");
        log.add("Initializing Weld with the following beans and alternatives:");

        SeContainerInitializer init = Weld.newInstance();
        init.disableDiscovery();

        // Add beans
        Class<?>[] beanClasses = scanBeans("org.codedefenders")
            .stream()
            .filter(clazz -> includeConfig || !ConfigurationSource.class.isAssignableFrom(clazz))
            .toArray(Class<?>[]::new);
        init.addBeanClasses(beanClasses);
        Arrays.stream(beanClasses).map(clazz -> "- bean: " + clazz.getName()).forEach(log::add);

        // Add alternatives
        if (alternatives != null) {
            init.selectAlternatives(alternatives);
            Arrays.stream(alternatives).map(clazz -> "- alternative: " + clazz.getName()).forEach(log::add);
        }

        logger.debug(log.toString());
        return init.initialize();
    }
}

