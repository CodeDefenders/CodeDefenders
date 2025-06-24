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
package org.codedefenders.dependencies;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.dependencies.MavenDependencyResolver.MavenDependencyResolverException;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public class DependencyProvider {
    private static final Logger logger = LoggerFactory.getLogger(DependencyProvider.class);

    private static final List<String> JACOCO_SPECS = List.of(
        "org.jacoco:org.jacoco.core:0.8.8",
        "org.jacoco:org.jacoco.report:0.8.8",
        "org.jacoco:org.jacoco.agent:0.8.8",
        "org.jacoco:org.jacoco.ant:0.8.8"
    );

    private static final List<String> COMMON_SPECS = List.of(
        // JUnit
        "junit:junit:4.13.2",
        "org.junit.jupiter:junit-jupiter-api:5.9.0",
        "org.opentest4j:opentest4j:1.2.0",
        "org.junit.platform:junit-platform-commons:1.9.0",

        // Mockito
        "org.mockito:mockito-core:4.8.0",
        "org.objenesis:objenesis:3.2",
        "net.bytebuddy:byte-buddy:1.12.14",
        "net.bytebuddy:byte-buddy-agent:1.12.14",

        // Hamcrest
        "org.hamcrest:hamcrest:2.2",

        // Google Truth
        "com.google.truth:truth:1.1.3",
        "com.google.truth.extensions:truth-java8-extension:1.1.3",
        "com.google.guava:guava:31.1-jre"
    );

    private static String JACOCO_CLASSPATH;
    private static String COMMON_CLASSPATH;
    private static String TEST_CLASSPATH;

    private final Configuration config;

    @Inject
    public DependencyProvider(Configuration config) {
        this.config = config;
    }

    public String getJacocoClasspath() {
        return checkClasspath(JACOCO_CLASSPATH);
    }

    public String getCommonClasspath() {
        return checkClasspath(COMMON_CLASSPATH);
    }

    public String getTestClasspath() {
        return checkClasspath(TEST_CLASSPATH);
    }

    public void installDependencies() throws MavenDependencyResolverException {
        try (var resolver = new MavenDependencyResolver(getLocalRepoPath(), "16")) {
            JACOCO_CLASSPATH = resolver.resolveDependencies(parseSpecs(JACOCO_SPECS)).getClasspath();
            logger.info("Resolved JaCoCo Classpath: " + JACOCO_CLASSPATH);

            COMMON_CLASSPATH = resolver.resolveDependencies(parseSpecs(COMMON_SPECS)).getClasspath();
            logger.info("Resolved Common Classpath: " + COMMON_CLASSPATH);

            List<String> testSpecs = new ArrayList<>();
            testSpecs.addAll(JACOCO_SPECS);
            testSpecs.addAll(COMMON_SPECS);
            TEST_CLASSPATH = resolver.resolveDependencies(parseSpecs(testSpecs)).getClasspath();
            logger.info("Resolved Test Classpath: " + TEST_CLASSPATH);
        }
    }

    public Path getLocalRepoPath() {
        return config.getLibraryDir().toPath().resolve("m2");
    }

    public String getJavaVersion() {
        return config.getAntJavaVersion()
            .map(String::valueOf)
            .orElseThrow();
    }

    private Collection<Artifact> parseSpecs(Collection<String> specs) {
        return specs.stream()
            .map(MavenDependencyResolver::parseArtifactSpec)
            .toList();
    }

    private String checkClasspath(String classpath) {
        if (classpath == null) {
            throw new IllegalStateException("Classpath is not initialized.");
        }
        return classpath;
    }
}
