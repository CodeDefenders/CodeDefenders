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
package org.codedefenders.model.llm.strategy;

import java.util.Optional;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;

public abstract class LlmStrategy {

    public static Optional<LlmStrategy> fromName(String name) {
        return switch (name) {
            case "DEFAULT" -> Optional.of(new DefaultStrategy());
            case "FULL_TEST_SUITE" -> Optional.of(new FullTestSuiteStrategy());
            case "SINGLE_METHOD_TEST" -> Optional.of(new SingleMethodTestStrategy());
            default -> Optional.empty();
        };
    }

    private final String name;

    protected LlmStrategy(String name) {
        this.name = name;
    }

    public int getNormalNumberOfTries() {
        return AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.LLM_NORMAL_PROMPT_NUMBER_OF_TRIES).getIntValue();
    }

    public int getEquivalenceNumberOfTries() {
        return AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.LLM_EQUIVALENCE_DUEL_NUMBER_OF_TRIES).getIntValue();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return getClass().equals(o.getClass());
    }
}
