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
package org.codedefenders.model;

import com.google.gson.annotations.Expose;

public class ClassroomMember {
    @Expose
    private final int userId;

    @Expose
    private final int classroomId;

    @Expose
    private final ClassroomRole role;

    public ClassroomMember(int userId, int classroomId, ClassroomRole role) {
        this.userId = userId;
        this.classroomId = classroomId;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public int getClassroomId() {
        return classroomId;
    }

    public ClassroomRole getRole() {
        return role;
    }
}
