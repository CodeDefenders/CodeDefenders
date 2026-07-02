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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.llm.AbstractStrategy;

import com.google.gson.annotations.Expose;

/**
 * A concrete strategy that can be either a default or a customized strategy.
 */
public class LlmStrategy implements Serializable {

    /**
     * The time between LLM actions is reduced by this multiplier.
     */
    double timeModifier;

    @Expose
    private final String name;

    Map<LlmPromptType, String> customPrompts;

    final transient Class<? extends AbstractStrategy> service;

    private final LlmDefaultStrategies base;

    private LlmStrategy(String name, Map<LlmPromptType, String> customPrompts,
                        Class<? extends AbstractStrategy> service,
                        double timeModifier, LlmDefaultStrategies base) {
        this.name = name;
        this.customPrompts = customPrompts;
        this.service = service;
        this.timeModifier = timeModifier;
        this.base = base;
    }

    public LlmStrategy(String name, LlmDefaultStrategies base) {
        this(name, new HashMap<>(), base.service, base.timeModifier, base);
    }

    public static LlmStrategy of(LlmDefaultStrategies base) {
        return new LlmStrategy(base.name(), base);
    }

    public void setTimeModifier(double timeModifier) {
        this.timeModifier = timeModifier;
    }

    public double getTimeModifier() {
        return timeModifier;
    }

    public void setCustomPrompts(Map<LlmPromptType, String> customPrompts) {
        this.customPrompts = customPrompts;
    }

    public void setPrompt(LlmPromptType promptType, String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            customPrompts.put(promptType, prompt);
        }
    }

    public void removePrompt(LlmPromptType promptType) {
        customPrompts.remove(promptType);
    }

    /**
     * Returns the correct prompt for a specified prompt type. Returns the customized prompt type, if specified,
     * or falls back to the default prompt of this type for this strategy.
     * @throws IllegalArgumentException If the prompt type is not specified for this strategy.
     */
    public String getPrompt(LlmPromptType promptType) {
        if (base.promptTypes.contains(promptType)) {
            if (customPrompts.containsKey(promptType)) {
                return customPrompts.get(promptType);
            } else {
                return promptType.getDefaultPrompt();
            }
        } else {
            throw new IllegalArgumentException("The prompt type " + promptType + " is not specified for a strategy "
                    + "based on " + base.name());
        }
    }

    public Map<LlmPromptType, String> getCustomPrompts() {
        return customPrompts;
    }

    public Class<? extends AbstractStrategy> getService() {
        return service;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name
                .replaceFirst("TEST", "")
                .replaceFirst("MUTANT", "")
                .replace('_', ' ')
                .toLowerCase();
    }

    /**
     * Returns true if this strategy is a base strategy, returns false if it is a custom strategy.
     */
    public boolean isReadOnly() {
        return base.name().equals(name);
    }

    public LlmDefaultStrategies getBase() {
        return base;
    }
}
