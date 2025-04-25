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
package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * <p>Provides data for the mutant accordion game component.</p>
 * <p>Bean Name: {@code mutantAccordion}</p>
 */
@Named(value = "mutantAccordion")
@RequestScoped
public class MutantAccordionBean {
    private static final Logger logger = LoggerFactory.getLogger(MutantAccordionBean.class);

    private Integer gameId;

    private List<MutantDTO> mutantList;
    private List<MutantAccordionCategory> categories;


    @Inject
    public MutantAccordionBean(GameService gameService, CodeDefendersAuth login, GameProducer gameProducer,
                               TestRepository testRepo, UserService userService) {
        var game = gameProducer.getGame();
        if (game != null) {
            var mutantList = gameService.getMutants(login.getUserId(), game.getId());
            if (!(game instanceof PuzzleGame)) {
                mutantList = addExternalKillingTests(testRepo, userService, mutantList);
            }
            init(game.getCUT(), mutantList, game.getId());
        }
    }

    private List<MutantDTO> addExternalKillingTests(TestRepository testRepo, UserService userService,
                                                    List<MutantDTO> mutantList) {
        List<MutantDTO> newList = new ArrayList<>(mutantList.size());

        for (MutantDTO mutant : mutantList) {
            if (mutant.getState() == Mutant.State.EQUIVALENT) {
                var testExecOpt = testRepo.getExternalKillingTestExecutionForMutant(mutant.getId());
                if (testExecOpt.isPresent()) {
                    var testExec = testExecOpt.get();
                    var externalKillingTest = testRepo.getTestById(testExec.testId);
                    var killingTestCreator = userService.getSimpleUserByPlayerId(externalKillingTest.getPlayerId());

                    newList.add(mutant.copyWithKillingTest(
                            testExec.testId,
                            killingTestCreator.orElseThrow(),
                            testExec.message
                    ));
                    continue;
                }
            }

            newList.add(mutant);
        }

        return newList;
    }

    public void init(GameClass cut, List<MutantDTO> mutantList, Integer gameId) {
        this.mutantList = mutantList;
        this.gameId = gameId;

        List<MethodDescription> methodDescriptions = cut.getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methodDescriptions, mutantList);

        categories = new ArrayList<>();
        categories.add(
                new MutantAccordionCategory(
                        "All Mutants",
                        GameAccordionMapping.ALL_CATEGORY_ID,
                        mapping.allElements));
        categories.add(
                new MutantAccordionCategory(
                        "Mutants outside methods",
                        GameAccordionMapping.OUTSIDE_METHODS_CATEGORY_ID,
                        mapping.elementsOutsideMethods));

        for (int i = 0; i < methodDescriptions.size(); i++) {
            MethodDescription method = methodDescriptions.get(i);

            String description = method.getDescription();
            String id = Integer.toString(i);
            SortedSet<Integer> testIds = mapping.elementsPerMethod.get(method);

            categories.add(new MutantAccordionCategory(description, id, testIds));
        }
    }

    public Integer getGameId() {
        return gameId;
    }

    public List<MutantDTO> getMutants() {
        return mutantList;
    }

    public List<MutantAccordionCategory> getCategories() {
        return categories;
    }

    public String jsonFromCategories() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(categories);
    }

    public String jsonMutants() {
        Map<Integer, MutantDTO> mutants = this.mutantList.stream()
                .collect(Collectors.toMap(MutantDTO::getId, Function.identity()));
        // TODO If we try to sort the mutants according to the order they appear in the
        //  class we need to sort the Ids in the MutantAccordionCategory.
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        // We need to do the JavaScript escaping in the end, since otherwise {@code '} character don't get escaped
        // properly
        return StringEscapeUtils.escapeEcmaScript(gson.toJson(mutants));
    }

    public static class MutantAccordionCategory {
        @Expose
        private String description;
        @Expose
        private String id;
        @Expose
        private SortedSet<Integer> mutantIds;

        public MutantAccordionCategory(String description, String id, SortedSet<Integer> mutantIds) {
            this.description = description;
            this.id = id;
            this.mutantIds = mutantIds;
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getMutantIds() {
            return Collections.unmodifiableSet(mutantIds);
        }

        public String getId() {
            return id;
        }

    }
}
