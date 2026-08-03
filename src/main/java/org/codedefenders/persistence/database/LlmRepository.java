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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.llm.NoSuchModelException;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmDefaultStrategies;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.intellij.lang.annotations.Language;

@ApplicationScoped
public class LlmRepository {

    @Inject
    Configuration config;

    @Inject
    QueryRunner queryRunner;

    /**
     * Adds a new model to the DB. If it already exists, nothing happens.
     * A new model is automatically active.
     * @param model representation of the model to be added.
     */
    public void addNewModel(LlModel model) {
        @Language("SQL")
        String sql = "INSERT IGNORE INTO llm_models(type, model_name, active) VALUES (?,?, TRUE)";
        queryRunner.execute(sql, model.getType().name(), model.getName());
    }

    public void deleteModel(LlModel model) {
        @Language("SQL")
        String sql = "DELETE FROM llm_models where type = ? and model_name = ?";
        queryRunner.execute(sql, model.getType().name(), model.getName());
    }

    /**
     * Set the model as active or inactive in the DB.
     */
    public void setActive(String name, LlmType type, boolean active) {
        @Language("SQL")
        String sql = "UPDATE llm_models SET active = ? WHERE model_name = ? and type = ?";
        queryRunner.update(sql, active, name, type.name());
    }

    /**
     * Update the values of an existing {@link LlModel}. It is identified by type and name, all other values are filled
     * up from DB.
     *
     * @throws NoSuchModelException If there is no model with this type and name
     *                                                            in the database.
     */
    public void loadModel(LlModel model) throws NoSuchModelException {
        LlModel fromDB = getModelFromName(model.getName(), model.getType(), false).orElseThrow(
                () -> new NoSuchModelException(model.getType(), model.getName())
        );
        model.copyValues(fromDB);
    }

    public Optional<LlModel> getModelFromName(String name, LlmType type, boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                         WHERE llm_models.model_name = ? AND llm_models.type = ?
                """
                + (mustBeActive ? " AND active = true;" : ";");
        return queryRunner.query(sql, LlmRepository::oneModelFromRs, name, type.name());
    }

    /**
     * Returns a list of all models in the DB.
     */
    public List<LlModel> getAllModels() {
        return getAllModels(false);
    }

    /**
     * Returns a list of models in the database.
     * @param mustBeActive If false, all models are returned, of true, only active ones are returned.
     */
    public List<LlModel> getAllModels(boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                """ + (mustBeActive ? " WHERE active = true" : "")
                + " ORDER BY llm_models.type, llm_models.model_name";

        return queryRunner.query(
                sql, LlmRepository::modelsFromRs);
    }

    /**
     * Returns an Optional containing the strategy with the supplied name, or an empty optional if
     * no such strategy exists. Can return both Default strategies and custom strategies.
     */
    public Optional<LlmStrategy> getStrategyByName(String name) {
        for (LlmDefaultStrategies s : LlmDefaultStrategies.values()) {
            if (s.name().equals(name)) {
                return Optional.of(LlmStrategy.of(s));
            }
        }
        return getCustomStrategy(name);
    }

    /**
     * Returns a list of all strategies.
     */
    public List<LlmStrategy> getAllStrategies() {
        List<LlmStrategy> result = new ArrayList<>();
        for (LlmDefaultStrategies s : LlmDefaultStrategies.values()) {
            result.add(LlmStrategy.of(s));
        }
        @Language("SQL")
        String customSql = """
                SELECT Strategy_Name, Base_name, Time_modifier, Prompt_type, Prompt
                FROM llm_custom_strategies
                LEFT JOIN llm_custom_prompts
                ON llm_custom_strategies.Strategy_ID = llm_custom_prompts.Strategy_ID;

                """;


        Collection<LlmStrategy> customStrats = queryRunner.query(customSql,
                rs -> {
                    Map<String, LlmStrategy> strats = new HashMap<>();
                    while (rs.next()) {
                        String name = rs.getString("Strategy_Name");
                        LlmDefaultStrategies base = LlmDefaultStrategies.valueOf(rs.getString("Base_name"));
                        if (!strats.containsKey(name)) {
                            LlmStrategy s = new LlmStrategy(name, base);
                            s.setTimeModifier(rs.getDouble("Time_modifier"));
                            strats.put(name, s);
                        }
                        LlmStrategy strat = strats.get(name);
                        String typeName = rs.getString("Prompt_type");
                        String prompt = rs.getString("Prompt");
                        if (typeName != null && prompt != null) {
                            LlmPromptType type = LlmPromptType.valueOf(rs.getString("Prompt_type"));
                            strat.setPrompt(type, prompt);
                        }
                    }
                    return strats.values();
                }
        );
        result.addAll(customStrats);

        return result;
    }


    public List<LlmStrategy> getAttackStrategies() {
        return getStrategiesForRole("MUTANT");
    }

    public List<LlmStrategy> getDefendStrategies() {
        return getStrategiesForRole("TEST");
    }

    public List<LlmStrategy> getEquivalenceStrategies() {
        return getStrategiesForRole("EQUIVALENCE");
    }


    public List<LlmStrategy> getStrategiesForRole(String prefix) {
        return getAllStrategies()
                .stream()
                .filter(s -> s.getBase().name().startsWith(prefix))
                .toList();
    }

    private static Optional<LlModel> oneModelFromRs(ResultSet rs) throws SQLException {
        List<LlModel> results = modelsFromRs(rs);
        if (results.isEmpty()) {
            return Optional.empty();
        } else if (results.size() > 1) {
            throw new RuntimeException("Found two results, when there should be only one.");
        } else {
            return Optional.of(results.get(0));
        }
    }

    private static List<LlModel> modelsFromRs(ResultSet rs) throws SQLException {
        List<LlModel> result = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString("llm_models.model_name");
            LlmType type = LlmType.valueOf(rs.getString("llm_models.type"));
            boolean active = rs.getBoolean("llm_models.active");
            result.add(new LlModel(name, type, active));
        }

        return result;
    }

    private static Optional<LlmStrategy> strategyFromRs(ResultSet rs) throws SQLException {
        LlmStrategy result = null;
        if (!rs.isBeforeFirst()) {
            return Optional.empty();
        }

        while (rs.next()) {
            if (result == null) {
                String name = rs.getString("Strategy_Name");
                LlmDefaultStrategies base = LlmDefaultStrategies.valueOf(rs.getString("Base_name"));
                result = new LlmStrategy(name, base);
            }
            String promptTypeName = rs.getString("Prompt_type");
            String promptContent = rs.getString("Prompt");
            if (promptTypeName != null && promptContent != null) {
                result.setPrompt(LlmPromptType.valueOf(promptTypeName), promptContent);
            }
        }
        return Optional.ofNullable(result);
    }


    public Optional<LlmStrategy> getCustomStrategy(String strategyName) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_custom_strategies
                    left join llm_custom_prompts prompts
                        on llm_custom_strategies.Strategy_ID = prompts.Strategy_ID
                WHERE Strategy_Name = ?
                """;
        return queryRunner.query(sql, LlmRepository::strategyFromRs, strategyName);
    }

    public void saveCustomStrategy(LlmStrategy customStrategy) {
        saveCustomStrategy(customStrategy, customStrategy.getName());
    }


    /**
     * Save a custom strategy to the DB.
     * @param customStrategy The strategy to save. It's content will be written to the DB.
     * @param oldName The old name of the strategy. This allows for the renaming of strategies.
     */
    public void saveCustomStrategy(LlmStrategy customStrategy, String oldName) {
        Optional<LlmStrategy> oldStrategy = getCustomStrategy(oldName);
        boolean alreadyExisting;
        if (oldStrategy.isEmpty()) {
            alreadyExisting = false;
        } else if (oldStrategy.get().getBase().equals(customStrategy.getBase())) {
            alreadyExisting = true;
        } else {
            // This should normally be caught at the UI level
            throw new IllegalArgumentException("A custom strategy with this name already exists.");
        }


        if (Arrays.stream(LlmDefaultStrategies.values())
                .anyMatch(strat -> strat.name().equals(customStrategy.getName()))) {
            // This should normally be caught at the UI level
            throw new IllegalArgumentException("You tried to name a custom strategy "
                    + customStrategy + "after a default strategy");
        }

        @Language("SQL") String strategySql;

        if (alreadyExisting) {
            strategySql = """
                    UPDATE llm_custom_strategies
                    SET Strategy_Name = ?,
                        Time_modifier = ?
                    WHERE Strategy_Name = ?
                    """;
            queryRunner.execute(strategySql, customStrategy.getName(), customStrategy.getTimeModifier(), oldName);
        } else {
            strategySql = """
                    INSERT INTO llm_custom_strategies(Strategy_Name, Base_name, Time_modifier)
                       VALUES (?, ?, ?)
                    """;
            queryRunner.execute(strategySql,
                    customStrategy.getName(),
                    customStrategy.getBase().name(),
                    customStrategy.getTimeModifier());
        }

        @Language("SQL")
        String getIdSql = """
                SELECT Strategy_ID from llm_custom_strategies where Strategy_Name = ?;
                """;
        int newId = queryRunner.query(getIdSql,
                ResultSetUtils.oneFromRS(rs -> rs.getInt("Strategy_ID")),
                customStrategy.getName()).orElseThrow();

        //delete old prompts
        @Language("SQL")
        String deletePromptSql = """
                DELETE FROM llm_custom_prompts
                WHERE Strategy_ID = ?;
                """;
        queryRunner.execute(deletePromptSql, newId);

        if (!customStrategy.getCustomPrompts().isEmpty()) {
            @Language("SQL")
            String insertNewPromptsSql = """
                        INSERT INTO llm_custom_prompts(Strategy_ID, Prompt_type, Prompt) values
                    """ + Stream.generate(() -> ("(?,?,?)"))
                    .limit(customStrategy.getCustomPrompts().size())
                    .collect(Collectors.joining(","));

            List<Object> params = new ArrayList<>();
            for (Map.Entry<LlmPromptType, String> entry : customStrategy.getCustomPrompts().entrySet()) {
                params.add(newId);
                params.add(entry.getKey().name());
                params.add(entry.getValue());
            }

            queryRunner.execute(insertNewPromptsSql, params.toArray());
        }

    }

    public void deleteCustomStrategy(String strategyName) {
        @Language("SQL")
        String promptSql = """
                DELETE FROM llm_custom_prompts WHERE Strategy_ID IN (
                    SELECT Strategy_ID FROM llm_custom_strategies
                        WHERE Strategy_Name = ?
                )
                """;
        queryRunner.execute(promptSql, strategyName);

        @Language("SQL")
        String stratSql = "DELETE FROM llm_custom_strategies WHERE Strategy_Name = ?";
        queryRunner.execute(stratSql, strategyName);
    }
}
