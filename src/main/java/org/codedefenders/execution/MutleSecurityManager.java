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

import java.net.URI;
import java.security.Permission;

/** Restricts submitted Mutle code without exposing the manager's own origin inspection. */
@SuppressWarnings("removal")
public final class MutleSecurityManager extends SecurityManager {
    private final String workspace = URI.create(System.getProperty("mutle.workspace")).getPath();
    private final ThreadLocal<Boolean> inspecting = ThreadLocal.withInitial(() -> false);

    @Override
    public void checkPermission(Permission permission) {
        // Only this private inspection may read origins through an otherwise unprivileged caller stack.
        if (permission instanceof RuntimePermission && "getProtectionDomain".equals(permission.getName())
                && inspecting.get()) {
            return;
        }
        super.checkPermission(permission);
    }

    @Override
    public void checkPackageAccess(String name) {
        if ((name.equals("org.jacoco.agent.rt") || name.startsWith("org.jacoco.agent.rt."))
                && hasSubmittedCaller()) {
            throw new SecurityException("Mutle tests cannot access coverage agent controls");
        }
        super.checkPackageAccess(name);
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
        if (hasSubmittedCaller()) {
            throw new SecurityException("Mutle tests cannot create or modify threads");
        }
    }

    private boolean hasSubmittedCaller() {
        boolean previous = inspecting.get();
        inspecting.set(true);
        try {
            for (Class<?> caller : getClassContext()) {
                var source = caller.getProtectionDomain().getCodeSource();
                if (source != null && source.getLocation() != null) {
                    URI location = URI.create(source.getLocation().toExternalForm());
                    // Submitted tests and CUT classes live in directories; trusted tools are immutable jars.
                    if ("file".equals(location.getScheme()) && location.getPath().startsWith(workspace)
                            && location.getPath().endsWith("/")) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            inspecting.set(previous);
        }
    }
}
