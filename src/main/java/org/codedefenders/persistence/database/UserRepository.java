/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.persistence.database;

import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.database.UserDAO;
import org.codedefenders.model.UserEntity;

@ApplicationScoped
public class UserRepository {

    public UserEntity getUserById(int userId) {
        return UserDAO.getUserById(userId);
    }

    public UserEntity getUserByName(String name) {
        return UserDAO.getUserByName(name);
    }

    public UserEntity getUserByEmail(String email) {
        return UserDAO.getUserByEmail(email);
    }
}
