/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.dto;

import org.codedefenders.model.KeyMap;

/**
 * This object contains all the properties of a User we expose to the application.
 */
public class User extends SimpleUser {

    public final Boolean active;

    public final String email;
    public final Boolean emailValidated;
    public final Boolean allowContact;

    public final KeyMap keyMap;

    public User(int id, String name, Boolean active, String email, Boolean emailValidated,
            Boolean allowContact, KeyMap keyMap) {
        super(id, name);
        this.active = active;
        this.email = email;
        this.emailValidated = emailValidated;
        this.allowContact = allowContact;
        this.keyMap = keyMap;
    }
}
