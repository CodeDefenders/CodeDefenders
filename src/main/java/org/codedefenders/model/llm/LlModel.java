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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.annotations.Expose;

/**
 * Information about a specific Large Language Model that can be used by an LLM Defender or an LLM Attacker.
 * Prompts can be customized for each model, if not specified they inherit the values of the default model, which is
 * obtained by {@link org.codedefenders.persistence.database.LlmRepository#getDefaultModel()}
 * TODO: This could be made easier by using a {@code Map<PromptType, String>} or something similar
 */
public class LlModel {
    @Expose
    private final String name;
    @Expose
    private final LlmType type;
    @Expose
    private boolean active;

    private HashMap<PromptType, String> prompts = new HashMap<>();
    /**
     * If true, dependency code will be part of the user message.
     *
     */
    @Expose
    private boolean attackerDependencies = true;

    /**
     * If true, dependency code will be part of the user message for defender prompts.
     */
    @Expose
    private boolean defenderDependencies = true;

    /**
     * If true, the prompt may be replaced with a special prompt that guides the llm to write a test for a
     * specific method, for example because there are unkilled mutants in this method.
     */
    @Expose
    private boolean defenderMethodFocus = true;

    public LlModel(String name, LlmType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public LlmType getType() {
        return type;
    }

    public boolean isAttackerDependencies() {
        return attackerDependencies;
    }

    public void setAttackerDependencies(boolean attackerDependencies) {
        this.attackerDependencies = attackerDependencies;
    }

    public boolean isDefenderDependencies() {
        return defenderDependencies;
    }

    public void setDefenderDependencies(boolean defenderDependencies) {
        this.defenderDependencies = defenderDependencies;
    }

    public boolean isDefenderMethodFocus() {
        return defenderMethodFocus;
    }

    public void setDefenderMethodFocus(boolean defenderMethodFocus) {
        this.defenderMethodFocus = defenderMethodFocus;
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
        } else return false;
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
        attackerDependencies = other.attackerDependencies;
        defenderDependencies = other.defenderDependencies;
        defenderMethodFocus = other.defenderMethodFocus;

        prompts = new HashMap<>(other.prompts);
    }

    public Optional<String> getPrompt(PromptType type) {
        return Optional.ofNullable(prompts.get(type));
    }

    public void setPrompt(PromptType type, String prompt) {
        prompts.put(type, prompt);
    }

    public Set<PromptType> getCustomPromptTypes() {
        return prompts.keySet();
    }
}
