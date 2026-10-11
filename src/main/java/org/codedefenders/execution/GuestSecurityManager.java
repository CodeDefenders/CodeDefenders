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
import java.security.AccessController;
import java.security.PrivilegedAction;

/** Prevents submitted code from creating unbounded native threads, including through privileged library calls. */
@SuppressWarnings("removal")
public class GuestSecurityManager extends SecurityManager {
    @Override
    public void checkAccess(Thread thread) {
        rejectGuestThreadAccess();
        super.checkAccess(thread);
    }

    @Override
    public void checkAccess(ThreadGroup group) {
        rejectGuestThreadAccess();
        super.checkAccess(group);
    }

    private void rejectGuestThreadAccess() {
        Class<?>[] callers = getClassContext();
        boolean guest = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            String workspace = URI.create(System.getProperty("guest.workspace")).getPath();
            for (Class<?> caller : callers) {
                var source = caller.getProtectionDomain().getCodeSource();
                if (source != null && source.getLocation() != null) {
                    URI location = URI.create(source.getLocation().toExternalForm());
                    // All submitted/CUT classes live in private workspace directories; trusted tools are jars.
                    if ("file".equals(location.getScheme()) && location.getPath().startsWith(workspace)
                            && location.getPath().endsWith("/")) {
                        return true;
                    }
                }
            }
            return false;
        });
        if (guest) {
            throw new SecurityException("Guest tests cannot create or modify threads");
        }
    }
}
