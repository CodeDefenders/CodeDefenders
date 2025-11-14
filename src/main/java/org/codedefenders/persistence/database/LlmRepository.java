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
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.codedefenders.service.llm.NoSuchModelException;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LlmRepository {
    private static final Logger logger = LoggerFactory.getLogger(LlmRepository.class);

    private static final String DEFAULT_MODEL_NAME = "DEFAULT_MODEL";

    private static final String DEFAULT_ATTACKER_PROMPT =
            """
                    Change the first class of the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code.
                    Make sure to introduce at least one change.

                    You might see some diffs of mutants at the end of the user message.
                    Those mutants already exist, create different ones.""".trim().stripIndent();

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
                    Make sure to introduce at least one change.

                    You might see some diffs of mutants at the end of the user message.
                    Those mutants already exist, create different ones.""".trim().stripIndent();

    private static final String DEFAULT_RESOLVE_EQUIVALENT_PROMPT =
            """
                    You will see the code of a java class and then a git diff of a change. Write a test using
                    JUnit 4 that succeeds on the old version, but fails after the diff is applied.
                    Use a maximum of 2 assertions.
                     Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_DEFENDER_DEPS_PROMPT =
            """
                    Write a single test for the first class of the following Java code using a maximum of 2 assertions.
                    The other classes are dependencies of the first class, you don't need to test them.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

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
        LlModel defaultModel = new LlModel(DEFAULT_MODEL_NAME, LlmType.DEFAULT);
        defaultModel.setAttackerPrompt(DEFAULT_ATTACKER_PROMPT);
        defaultModel.setAttackerDependencyPrompt(DEFAULT_ATTACKER_DEPS_PROMPT);
        defaultModel.setResolveEquivalencePrompt(DEFAULT_RESOLVE_EQUIVALENT_PROMPT);
        defaultModel.setDefenderPrompt(DEFAULT_DEFENDER_PROMPT);
        defaultModel.setDefenderDependencyPrompt(DEFAULT_DEFENDER_DEPS_PROMPT);
        defaultModel.setDefenderMethodFocusPrompt(DEFAULT_DEFENDER_FOCUS_PROMPT);

        saveModel(defaultModel);

    }

    /**
     * Saves the values of the model in the database. If the model already exists, update its values,
     * if it doesn't exist, create a new entry.
     */
    public void saveModel(LlModel model) {
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
                    attacker_resolve_equivalence_prompt = ?,
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
                                           attacker_resolve_equivalence_prompt,
                                           active, model_name, type)
                    values (?,?,?,?,?,?,?,?,?,?,?,?)""";
        }
        queryRunner.update(sql, model.getDefenderPrompt().orElse(null),
                model.isDefenderDependencies(), model.getDefenderDependencyPrompt().orElse(null),
                model.isDefenderMethodFocus(), model.getDefenderMethodFocusPrompt().orElse(null),
                model.getAttackerPrompt().orElse(null),
                model.isAttackerDependencies(), model.getAttackerDependencyPrompt().orElse(null),
                model.getResolveEquivalencePrompt().orElse(null),
                model.isActive(), model.getName(), model.getType().name());
    }

    /**
     * Only change the prompts of an existing model. Throws an exception if that model doesn't exist, keeps
     * 'active' datum unchanged.
     * @param model The model, identified by type and name, containing the new prompts.
     */
    public void updatePrompts(LlModel model) {
        @Language("SQL")
        String sql = """
                    update llm_models set
                    defender_prompt = ?,
                    defender_dependencies = ?,
                    defender_dependencies_prompt = ?,
                    defender_method_focus = ?,
                    defender_method_focus_prompt = ?,
                    attacker_prompt = ?,
                    attacker_dependencies = ?,
                    attacker_dependencies_prompt = ?,
                    attacker_resolve_equivalence_prompt = ?
                    WHERE model_name = ? AND type = ?;
                    """;
        int updated = queryRunner.update(sql, model.getDefenderPrompt().orElse(null),
                model.isDefenderDependencies(), model.getDefenderDependencyPrompt().orElse(null),
                model.isDefenderMethodFocus(), model.getDefenderMethodFocusPrompt().orElse(null),
                model.getAttackerPrompt().orElse(null),
                model.isAttackerDependencies(), model.getAttackerDependencyPrompt().orElse(null),
                model.getResolveEquivalencePrompt().orElse(null),
                model.getName(), model.getType().name());
        if (updated == 0) {
            logger.error("Trying to update non-existing model: {} {}", model.getType(), model.getName());
            throw new IllegalArgumentException("Trying to update non-existing model: "
                    + model.getType() + ", " +  model.getName());
        }
    }

    public void setActive(String name, LlmType type, boolean active) {
        @Language("SQL")
        String sql = "UPDATE llm_models SET active = ? WHERE model_name = ? and type = ?";
        queryRunner.update(sql, active, name, type.name());
    }

    /**
     * Update the values of an existing {@link LlModel}. It is identified by type and name, all other values are filled
     * up from DB.
     * @throws org.codedefenders.service.llm.NoSuchModelException If there is no model with this type and name
     * in the database.
     */
    public void loadModel(LlModel model) throws NoSuchModelException {
        LlModel fromDB = getModelFromName(model.getName(), model.getType(), false).orElseThrow(
                () -> new NoSuchModelException(model.getType(), model.getName())
        );
        model.copyValues(fromDB);
    }

    public Optional<LlModel> getDefaultModel() {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE type = ? LIMIT 1;";
        return queryRunner.query(sql, ResultSetUtils.oneFromRS(LlmRepository::fromRS), LlmType.DEFAULT.name());
    }

    public Optional<LlModel> getModelFromName(String name, LlmType type, boolean mustBeActive) {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE model_name = ? AND type = ?;";
        Optional<LlModel> result = queryRunner.query(sql, ResultSetUtils.oneFromRS(LlmRepository::fromRS), name, type.name());
        if (!mustBeActive || result.isPresent() && result.get().isActive()) {
            return result;
        } else {
            return Optional.empty();
        }
    }

    public List<LlModel> getAllModels() {
        return getAllModels(false);
    }

    public List<LlModel> getAllModels(boolean mustBeActive) {
        @Language("SQL")
        String sql = "SELECT * FROM llm_models WHERE type != ? " + (mustBeActive ? "AND active = true;" : ";");

        List<LlModel> modelsInDB =  queryRunner.query(
                sql, ResultSetUtils.listFromRS(LlmRepository::fromRS), LlmType.DEFAULT.name());

        return modelsInDB.stream().filter(this::modelIsInConfig).toList();
    }

    private boolean modelIsInConfig(LlModel m) {
        return m.getType() == LlmType.OLLAMA && config.getLlmOllamaModels().contains(m.getName())
                || m.getType() == LlmType.OPENAI && config.getLlmOpenaiModels().contains(m.getName());
    }

    private static LlModel fromRS(ResultSet rs) throws SQLException {
        String name = rs.getString("model_name");
        LlmType type = LlmType.valueOf(rs.getString("type"));
        LlModel result = new LlModel(name, type);
        result.setActive(rs.getBoolean("active"));

        result.setDefenderPrompt(rs.getString("defender_prompt"));
        result.setDefenderDependencies(rs.getBoolean("defender_dependencies"));
        result.setDefenderDependencyPrompt(rs.getString("defender_dependencies_prompt"));
        result.setDefenderMethodFocus(rs.getBoolean("defender_method_focus"));
        result.setDefenderMethodFocusPrompt(rs.getString("defender_method_focus_prompt"));

        result.setAttackerPrompt(rs.getString("attacker_prompt"));
        result.setAttackerDependencies(rs.getBoolean("attacker_dependencies"));
        result.setAttackerDependencyPrompt(rs.getString("attacker_dependencies_prompt"));
        result.setResolveEquivalencePrompt(rs.getString("attacker_resolve_equivalence_prompt"));

        return result;
    }


}
