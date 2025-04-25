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

import java.util.Optional;
import java.util.UUID;

import com.google.gson.annotations.Expose;

public class Classroom {
    @Expose
    private final int id;

    @Expose
    private final UUID uuid;

    @Expose
    private final Integer creatorId;

    @Expose
    private final String name;

    private final String password;

    @Expose
    private final boolean open;

    @Expose
    private final boolean visible;

    @Expose
    private final boolean archived;

    public Classroom(
            int id,
            UUID uuid,
            Integer creatorId,
            String name,
            String password,
            boolean open,
            boolean visible,
            boolean archived) {
        this.id = id;
        this.uuid = uuid;
        this.creatorId = creatorId;
        this.name = name;
        this.password = password;
        this.open = open;
        this.visible = visible;
        this.archived = archived;
    }

    public Classroom(Classroom other) {
        this.id = other.id;
        this.uuid = other.uuid;
        this.creatorId = other.creatorId;
        this.name = other.name;
        this.password = other.password;
        this.open = other.open;
        this.visible = other.visible;
        this.archived = other.archived;
    }

    public int getId() {
        return id;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Optional<Integer> getCreatorId() {
        return Optional.ofNullable(creatorId);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isArchived() {
        return archived;
    }

    public static Builder builderFrom(Classroom classroom) {
        return new Builder(classroom);
    }

    public static class Builder {
        private int id;
        private UUID uuid;
        private Integer creatorId;
        private String name;
        private String password;
        private boolean open;
        private boolean visible;
        private boolean archived;

        private Builder() {

        }

        private Builder(Classroom classroom) {
            this.id = classroom.getId();
            this.uuid = classroom.getUUID();
            this.creatorId = classroom.getCreatorId().orElse(null);
            this.name = classroom.getName();
            this.password = classroom.getPassword().orElse(null);
            this.open = classroom.isOpen();
            this.visible = classroom.isVisible();
            this.archived = classroom.isArchived();
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setUUID(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder setCreatorId(Integer creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setOpen(boolean open) {
            this.open = open;
            return this;
        }

        public Builder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder setArchived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public Classroom build() {
            return new Classroom(
                    this.id,
                    this.uuid,
                    this.creatorId,
                    this.name,
                    this.password,
                    this.open,
                    this.visible,
                    this.archived
            );
        }
    }
}
