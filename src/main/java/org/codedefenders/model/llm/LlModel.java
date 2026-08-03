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
package org.codedefenders.model.llm;

import com.google.gson.annotations.Expose;

/**
 * Information about a specific Large Language Model that can be used by an LLM Defender or an LLM Attacker.
 */
public class LlModel {
    @Expose
    private final String name;
    @Expose
    private final LlmType type;
    @Expose
    private boolean active;

    public LlModel(String name, LlmType type) {
        this.name = name;
        this.type = type;
    }

    public LlModel(String name, LlmType type, boolean active) {
        this(name, type);
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public LlmType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LlModel m) {
            return m.type == type && (m.getName() == null && name == null || m.getName().equals(name));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return type + ":" + name;
    }

    /**
     * Loads all values except name and type from the other model.
     */
    public void copyValues(LlModel other) {
        active = other.active;
    }
}
