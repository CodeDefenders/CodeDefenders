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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.LLMType;
import org.codedefenders.model.LLModel;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LLMRepository {
    private static final Logger logger = LoggerFactory.getLogger(LLMRepository.class);

    private static final String DEFAULT_MODEL_NAME = "DEFAULT_MODEL";

    private static final String DEFAULT_ATTACKER_PROMPT =
            """
                    Change the first class of the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code.
                    Make sure to introduce at least one change.""".trim().stripIndent();

    private static final String DEFAULT_DEFENDER_PROMPT =
            """
                    Write a single test for the following Java code using a maximum of 2 assertions.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_ATTACKER_DEPS_PROMPT =
            """
                    Change the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code of the first class. don't change any
                    other classes.
                    Make sure to introduce at least one change.""".trim().stripIndent();

    private static final String DEFAULT_DEFENDER_DEPS_PROMPT =
            """
                    Write a single test for the first class of the following Java code using a maximum of 2 assertions.
                    The other classes are dependencies of the first class, you don't need to test them.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_ATTACKER_FOCUS_PROMPT =
            """
                    Change the the method %s of the following java code in a significant way.
                    The behaviour of the program should change.
                    Write the changed code of the entire first class, but not anything else.
                    Make sure to introduce at least one change.""".trim().stripIndent();

    private static final String DEFAULT_DEFENDER_FOCUS_PROMPT =
            """
                    Write a single test for the method %s of the following Java code using a maximum of 2 assertions.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    @Inject
    Configuration config;

    @Inject
    QueryRunner queryRunner;

    /**
     * Add models from the config file to DB. Duplicates are ignored.
     */
    public void addNewModels() {
        List<String> openaiModels = config.getLlmOpenaiModels();
        List<String> ollamaModels = config.getLlmOllamaModels();
        String[] modelNames = new String[openaiModels.size() + ollamaModels.size()];
        for (int i = 0; i < openaiModels.size(); i++) {
            modelNames[i] = openaiModels.get(i);
        }
        for (int i = 0; i < ollamaModels.size(); i++) {
            modelNames[openaiModels.size() + i] = ollamaModels.get(i);
        }

        @Language("SQL")
        String sql = "INSERT IGNORE INTO llm_models(model_name, type) values "
                + "(?, 'OPENAI'), ".repeat(openaiModels.size())
                + "(?, 'OLLAMA'), ".repeat(ollamaModels.size());
        sql = sql.substring(0, sql.length() - 2) + ";";

        queryRunner.update(sql, (Object[]) modelNames);

        boolean defaultValuesExists =
                queryRunner.query("SELECT model_name FROM llm_models WHERE model_name = ?",
                        ResultSet::isBeforeFirst, DEFAULT_MODEL_NAME);
        if (!defaultValuesExists) {
            resetDefaultModel();
        }
    }

    public void resetDefaultModel() {
        LLModel defaultModel = new LLModel(DEFAULT_MODEL_NAME, LLMType.DEFAULT);
        defaultModel.setAttackerPrompt(DEFAULT_ATTACKER_PROMPT);
        defaultModel.setAttackerDependencyPrompt(DEFAULT_ATTACKER_DEPS_PROMPT);
        defaultModel.setAttackerMethodFocusPrompt(DEFAULT_ATTACKER_FOCUS_PROMPT);
        defaultModel.setDefenderPrompt(DEFAULT_DEFENDER_PROMPT);
        defaultModel.setDefenderDependencyPrompt(DEFAULT_DEFENDER_DEPS_PROMPT);
        defaultModel.setDefenderMethodFocusPrompt(DEFAULT_DEFENDER_FOCUS_PROMPT);

        updateModel(defaultModel);

    }

    /**
     * Saves the values of the model in the database. If the model already exists, update its values,
     * if it doesn't exist, create a new entry.
     */
    public void updateModel(LLModel model) {
        boolean alreadyExists =
                queryRunner.query("SELECT model_name FROM llm_models WHERE model_name = ? AND type = ?",
                        ResultSet::isBeforeFirst, model.getName(), model.getType().name());

        @Language("SQL")
        String sql;
        if (alreadyExists) {
            sql = """
                    update llm_models set
                    defender_prompt = ?,
                    defender_dependencies = ?,
                    defender_dependencies_prompt = ?,
                    defender_method_focus = ?,
                    defender_method_focus_prompt = ?,
                    attacker_prompt = ?,
                    attacker_dependencies = ?,
                    attacker_dependencies_prompt = ?,
                    attacker_method_focus = ?,
                    attacker_method_focus_prompt = ?,
                    active = ?
                    WHERE model_name = ? AND type = ?;
                    """;
        } else {
            sql = """
                    insert into llm_models(
                                           defender_prompt,
                                           defender_dependencies, defender_dependencies_prompt,
                                           defender_method_focus, defender_method_focus_prompt,
                                           attacker_prompt,
                                           attacker_dependencies, attacker_dependencies_prompt,
                                           attacker_method_focus, attacker_method_focus_prompt,
                                           active, model_name, type)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        }
        queryRunner.update(sql, model.getDefenderPrompt().orElse(null),
                model.isDefenderDependencies(), model.getDefenderDependencyPrompt().orElse(null),
                model.isDefenderMethodFocus(), model.getDefenderMethodFocusPrompt().orElse(null),
                model.getAttackerPrompt().orElse(null),
                model.isAttackerDependencies(), model.getAttackerDependencyPrompt().orElse(null),
                model.isAttackerMethodFocus(), model.getAttackerMethodFocusPrompt().orElse(null),
                model.isActive(), model.getName(), model.getType().name());
    }

    public void setActive(String name, LLMType type, boolean active) {
        @Language("SQL")
        String sql = "UPDATE llm_models SET active = ? WHERE model_name = ? and type = ?";
        queryRunner.update(sql, active, name, type.name());
    }

    public Optional<LLModel> getDefaultModel() {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE type = ? LIMIT 1;";
        return queryRunner.query(sql, ResultSetUtils.oneFromRS(LLMRepository::fromRS), LLMType.DEFAULT.name());
    }

    public Optional<LLModel> getModelFromName(String name, LLMType type) {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE model_name = ? AND type = ?;";
        return queryRunner.query(sql, ResultSetUtils.oneFromRS(LLMRepository::fromRS), name, type.name());
    }

    public List<LLModel> getAllModels() {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE type != ?;";

        return queryRunner.query(sql, ResultSetUtils.listFromRS(LLMRepository::fromRS), LLMType.DEFAULT.name());
    }

    public List<LLModel> getAllModelsOfType(LLMType type) {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE type = ?;";

        return queryRunner.query(sql, ResultSetUtils.listFromRS(LLMRepository::fromRS), type.name());
    }

    private static LLModel fromRS(ResultSet rs) throws SQLException {
        String name = rs.getString("model_name");
        LLMType type = LLMType.valueOf(rs.getString("type"));
        LLModel result = new LLModel(name, type);
        result.setActive(rs.getBoolean("active"));

        result.setDefenderPrompt(rs.getString("defender_prompt"));
        result.setDefenderDependencies(rs.getBoolean("defender_dependencies"));
        result.setDefenderDependencyPrompt(rs.getString("defender_dependencies_prompt"));
        result.setDefenderMethodFocus(rs.getBoolean("defender_method_focus"));
        result.setDefenderMethodFocusPrompt(rs.getString("defender_method_focus_prompt"));

        result.setAttackerPrompt(rs.getString("attacker_prompt"));
        result.setAttackerDependencies(rs.getBoolean("attacker_dependencies"));
        result.setAttackerDependencyPrompt(rs.getString("attacker_dependencies_prompt"));
        result.setAttackerMethodFocus(rs.getBoolean("attacker_method_focus"));
        result.setAttackerMethodFocusPrompt(rs.getString("attacker_method_focus_prompt"));

        return result;
    }


}
