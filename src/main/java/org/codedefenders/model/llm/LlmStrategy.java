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

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.service.llm.LlmSubActionService;

public class LlmStrategy {

    /**
     * The time between LLM actions is reduced by this multiplier.
     */
    double timeModifier;

    private final String name;

    Map<LlmPromptType, String> customPrompts;

    final Class<? extends LlmSubActionService> service;

    private final LlmDefaultStrategy base;

    private LlmStrategy(String name, Map<LlmPromptType, String> customPrompts,
                        Class<? extends LlmSubActionService> service,
                        double timeModifier, LlmDefaultStrategy base) {
        this.name = name;
        this.customPrompts = customPrompts;
        this.service = service;
        this.timeModifier = timeModifier;
        this.base = base;
    }

    //    private LlmStrategy(String name, Map<ImmutablePair<String, String>, String> prompts, Class<LlmSubActionService> service) {
//        this(name, prompts, service, 1);
//    }
//
    public LlmStrategy(String name, LlmDefaultStrategy base) {
        this(name, new HashMap<>(), base.service, base.timeModifier, base);
    }

    public static LlmStrategy of(LlmDefaultStrategy base) {
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

    public String getHtmlPrompt(LlmPromptType promptType) {
        return getPrompt(promptType).replace("\r\n", "\n").replace("\n", "\\n");
    }

    public Map<LlmPromptType, String> getCustomPrompts() {
        return customPrompts;
    }

    public Class<? extends LlmSubActionService> getService() {
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

    public boolean isReadOnly() {
        return base.name().equals(name);
    }

    public LlmDefaultStrategy getBase() {
        return base;
    }
}
