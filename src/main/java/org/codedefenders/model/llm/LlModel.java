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

import java.util.Optional;

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

    /**
     * The standard attack prompt. The code of the CuT is supplied as the user message for attacker prompts.
     */
    @Expose
    private String attackerPrompt;
    /**
     * If true, dependency code will be part of the user message.
     *
     */
    @Expose
    private boolean attackerDependencies = true;
    /**
     * This String replaces the normal prompt if there are any dependency classes (and if
     * {@link LlModel#attackerDependencies} is true).
     * The code of the dependencies will be appended to the code of the CuT as part of the user message.
     */
    @Expose
    private String attackerDependencyPrompt;

    /**
     * This prompt is used to generate a test to kill a suspected mutant. The user message consists of the CuT,
     * the dependencies if {@link LlModel#attackerDependencies} is true, and the diff of the suspected mutant.
     */
    @Expose
    private String resolveEquivalencePrompt;

    /**
     * The standard defender prompt. The code of the CuT is supplied as the user message.
     */
    @Expose
    private String defenderPrompt;

    /**
     * If true, dependency code will be part of the user message for defender prompts.
     */
    @Expose
    private boolean defenderDependencies = true;

    /**
     * This String replaces the normal defender if there are any dependency classes (and if
     * {@link LlModel#attackerDependencies} is true).
     * The code of the dependencies will be appended to the code of the CuT as part of the user message.
     */
    @Expose
    private String defenderDependencyPrompt;

    /**
     * If true, the prompt may be replaced with a special prompt that guides the llm to write a test for a
     * specific method, for example because there are unkilled mutants in this method.
     */
    @Expose
    private boolean defenderMethodFocus = true;

    /**
     * This String format replaces the normal or dependency prompt to focus on a specific method.
     * This should only be used with {@link String#format(String, Object...)} with the method name
     * as the single argument.
     */
    @Expose
    private String defenderMethodFocusPrompt;

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

    public Optional<String> getAttackerPrompt() {
        return ofNullOrEmpty(attackerPrompt);
    }

    public void setAttackerPrompt(String attackerPrompt) {
        this.attackerPrompt = attackerPrompt;
    }

    public boolean isAttackerDependencies() {
        return attackerDependencies;
    }

    public void setAttackerDependencies(boolean attackerDependencies) {
        this.attackerDependencies = attackerDependencies;
    }

    public Optional<String> getAttackerDependencyPrompt() {
        return ofNullOrEmpty(attackerDependencyPrompt);
    }

    public void setAttackerDependencyPrompt(String attackerDependencyPrompt) {
        this.attackerDependencyPrompt = attackerDependencyPrompt;
    }

    public Optional<String> getResolveEquivalencePrompt() {
        return ofNullOrEmpty(resolveEquivalencePrompt);
    }

    public void setResolveEquivalencePrompt(String resolveEquivalencePrompt) {
        this.resolveEquivalencePrompt = resolveEquivalencePrompt;
    }

    public Optional<String> getDefenderPrompt() {
        return ofNullOrEmpty(defenderPrompt);
    }

    public void setDefenderPrompt(String defenderPrompt) {
        this.defenderPrompt = defenderPrompt;
    }

    public boolean isDefenderDependencies() {
        return defenderDependencies;
    }

    public void setDefenderDependencies(boolean defenderDependencies) {
        this.defenderDependencies = defenderDependencies;
    }

    public Optional<String> getDefenderDependencyPrompt() {
        return ofNullOrEmpty(defenderDependencyPrompt);
    }

    public void setDefenderDependencyPrompt(String defenderDependencyPrompt) {
        this.defenderDependencyPrompt = defenderDependencyPrompt;
    }

    public boolean isDefenderMethodFocus() {
        return defenderMethodFocus;
    }

    public void setDefenderMethodFocus(boolean defenderMethodFocus) {
        this.defenderMethodFocus = defenderMethodFocus;
    }

    public Optional<String> getDefenderMethodFocusPrompt() {
        return ofNullOrEmpty(defenderMethodFocusPrompt);
    }

    public void setDefenderMethodFocusPrompt(String defenderMethodFocusPrompt) {
        this.defenderMethodFocusPrompt = defenderMethodFocusPrompt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private Optional<String> ofNullOrEmpty(String s) {
        if (s != null && !s.trim().isEmpty()) {
            return Optional.of(s);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LlModel m) {
            return m.type == type && (m.getName() == null && name == null || m.getName().equals(name));
        } else return false;
    }

    /**
     * Loads all values except name and type from the other model.
     */
    public void copyValues(LlModel other) {
        active = other.active;

        attackerPrompt = other.attackerPrompt;
        attackerDependencies = other.attackerDependencies;
        attackerDependencyPrompt = other.attackerDependencyPrompt;

        defenderPrompt = other.defenderPrompt;
        defenderDependencies = other.defenderDependencies;
        defenderDependencyPrompt = other.defenderDependencyPrompt;
        defenderMethodFocus = other.defenderMethodFocus;
        defenderMethodFocusPrompt = other.defenderMethodFocusPrompt;
    }

    public Optional<String> getPrompt(PromptType type) {
        return Optional.ofNullable(switch (type) {
            case DEFEND_DEFAULT -> defenderPrompt;
            case DEFEND_DEPENDENCIES -> defenderDependencyPrompt;
            case DEFEND_FOCUS -> defenderMethodFocusPrompt;
            case ATTACK_DEFAULT -> attackerPrompt;
            case ATTACK_DEPENDENCIES -> attackerDependencyPrompt;
            case ATTACK_EQUIVALENCE -> resolveEquivalencePrompt;
        });
    }
}
