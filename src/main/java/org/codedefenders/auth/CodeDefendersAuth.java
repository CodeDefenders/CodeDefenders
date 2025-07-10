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
package org.codedefenders.auth;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;

public interface CodeDefendersAuth {

    boolean isLoggedIn();

    boolean isAdmin();

    /**
     * Returns the user ID of the current user.
     * <p>
     * <b>WARNING: Should never be used on pages a logged-out user may visit, it will lead to an 500 error,
     * even if the return value is never actually used.</b>
     * Use {@link CodeDefendersAuth#getUserIdCareful() instead.}
     */
    int getUserId();

    /**
     * Returns the userID if the currently logged-in user, or -1 if he is not logged in. In contrast to
     * {@link CodeDefendersAuth#getUserId()}, it will not produce an error when the user is not logged in.
     */
    int getUserIdCareful();

    SimpleUser getSimpleUser();

    User getUser();
}
