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
package org.codedefenders.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;

/** Rejects submitted-code thread operations, including calls through privileged library wrappers. */
@SuppressWarnings("removal")
public final class TestSecurityManager extends SecurityManager {
    public TestSecurityManager() {
        // Resolve trusted jar URLs on this worker, including Debian's symlinked Ant jars.
        setJarUrl("ant.core.lib", "ant.library.url", true);
        // Mutant executions have no agent; keep the policy's agent grant inactive there.
        System.setProperty("jacoco.agent.url", "file:/no-coverage-agent");
        setJarUrl("jacoco.agent.file", "jacoco.agent.url", false);
    }

    private static void setJarUrl(String fileProperty, String urlProperty, boolean parent) {
        String file = System.getProperty(fileProperty);
        if (file != null) {
            try {
                Path jar = Path.of(file).toRealPath();
                if (!Files.isRegularFile(jar)) {
                    throw new IOException("Trusted runtime is not a jar");
                }
                System.setProperty(urlProperty, (parent ? jar.getParent() : jar).toUri().toString());
            } catch (IOException e) {
                throw new IllegalStateException("Could not locate the trusted runtime: " + fileProperty, e);
            }
        }
    }

    @Override
    public void checkPermission(Permission permission) {
        if (permission instanceof RuntimePermission && "getProtectionDomain".equals(permission.getName())) {
            Class<?>[] callers = getClassContext();
            // Only Class.getProtectionDomain calls made by this enforcement class may inspect origins.
            // No mutable inspection flag or submitted-code permission is needed.
            if (callers.length > 2 && callers[1] == Class.class && callers[2] == TestSecurityManager.class) {
                return;
            }
        }
        super.checkPermission(permission);
    }

    @Override
    public void checkAccess(Thread thread) {
        rejectSubmittedThreadAccess();
        super.checkAccess(thread);
    }

    @Override
    public void checkAccess(ThreadGroup group) {
        rejectSubmittedThreadAccess();
        super.checkAccess(group);
    }

    private void rejectSubmittedThreadAccess() {
        for (Class<?> caller : getClassContext()) {
            var source = caller.getProtectionDomain().getCodeSource();
            // Submitted tests and CUT dependencies are directories; runner dependencies are jars.
            if (source != null && source.getLocation() != null
                    && "file".equals(source.getLocation().getProtocol())
                    && source.getLocation().getPath().endsWith("/")) {
                throw new SecurityException("Submitted code cannot create or modify threads");
            }
        }
    }
}
