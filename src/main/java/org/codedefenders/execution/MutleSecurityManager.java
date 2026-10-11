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

/** Prevents submitted Mutle tests from creating native threads, including through privileged library calls. */
@SuppressWarnings("removal")
public final class MutleSecurityManager extends SecurityManager {
    private final String workspace = URI.create(System.getProperty("mutle.workspace")).getPath();

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
            if (source != null && source.getLocation() != null) {
                URI location = URI.create(source.getLocation().toExternalForm());
                // Submitted tests and CUT classes live in directories; trusted tools are immutable jars.
                if ("file".equals(location.getScheme()) && location.getPath().startsWith(workspace)
                        && location.getPath().endsWith("/")) {
                    throw new SecurityException("Mutle tests cannot create or modify threads");
                }
            }
        }
    }
}
