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
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmDefaultStrategy;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.LlmRepository;
import org.codedefenders.service.llm.LlmInspectionService;
import org.codedefenders.service.llm.LlmManagerService;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

/**
 * This class should provide details and interactions with the Large Language Models uses by the
 * {@link org.codedefenders.service.llm} package, integrated to normal games. It is not affiliated with the
 * {@link org.codedefenders.servlets.api.llm} package.
 */
@WebServlet(Paths.API_LLM)
public class LlmApi extends HttpServlet {
    Logger logger = LoggerFactory.getLogger(LlmApi.class);

    @Inject
    LlmRepository llmRepo;

    @Inject
    LlmInspectionService inspectionService;

    @Inject
    GameRepository gameRepository;

    @Inject
    URLUtils url;

    @Inject
    private LlmManagerService llmService;

    /**
     * Send back information about LLMs or LLM players. Supported actions:
     *
     * <p>
     * - getall -> Send back a list of all Large Language Models. If the "mustBeActive" parameter is present,
     * only active models are sent back.
     * </p>
     *
     * <p>
     * - get -> Send back a single LLM identified by type and name
     * </p>
     *
     * <p>
     * - getLlmForGame -> Send back a single LLM that is active for the supplied game and role
     * </p>
     *
     * <p>
     * - getConversations -> Send back a list of conversations for the specified game and role.
     * </p>
     *
     * <p>
     * - error -> Send back the last error message produced by the llm player specified by gameId and role, or an
     * empty string if there is no error message.
     * </p>
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Optional<String> action = ServletUtils.getStringParameter(req, "action");
        Optional<LlmType> type = ServletUtils.getEnumParameter(req, LlmType.class, "type");
        Optional<String> name = ServletUtils.getStringParameter(req, "name");
        if (action.isPresent()) {
            String returnJson;
            Gson gson = new Gson();
            switch (action.get()) {
                case "getall" -> {
                    boolean mustBeActive = req.getParameter("mustBeActive") != null;
                    List<LlModel> models = llmRepo.getAllModels(mustBeActive);
                    Type typeOfSrc = new TypeToken<List<LlModel>>() {
                    }.getType();
                    returnJson = gson.toJson(models, typeOfSrc);
                }
                case "get" -> {
                    if (name.isPresent() && type.isPresent()) {
                        Optional<LlModel> model = llmRepo.getModelFromName(name.get(), type.get(), false);
                        if (model.isPresent()) {
                            returnJson = gson.toJson(model.get());
                        } else {
                            logger.warn("No model with name {} and type {}", name, type);
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }

                    } else {
                        logger.error("Missing name ({}) or type ({}) for get-action", name, type);
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
                case "getLlmForGame" -> {
                    Optional<Role> role = ServletUtils.getEnumParameter(req, Role.class, "role");
                    Optional<Integer> gameId = ServletUtils.gameId(req);
                    if (gameId.isPresent() && role.isPresent()) {
                        AbstractGame game = gameRepository.getGame(gameId.get());
                        Optional<LlModel> model = llmService.getModelForGame(game, role.get());
                        Optional<LlmStrategy> strategy = llmService.getStrategyForGame(game.getId(), role.get());
                        if (model.isPresent() && strategy.isPresent()) {
                            returnJson = gson.toJson(new LlmApiDTO(model.get(), strategy.get().getName()));
                        } else {
                            returnJson = gson.toJson(null);
                        }
                    } else {
                        logger.error("gameId ({}) or role ({}) are missing while trying to get an llm for a game.",
                                gameId, role);
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
                case "getConversations" -> {
                    Optional<Integer> gameId = ServletUtils.gameId(req);
                    if (gameId.isPresent()) {
                        AbstractGame game = gameRepository.getGame(gameId.get());
                        List<LlmConversation> messages = inspectionService.getConversations(game);
                        returnJson = inspectionService.toJson(messages);
                    } else {
                        logger.error("gameId is missing while trying to access conversations.");
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }

                case "error" -> {
                    Optional<Role> role = ServletUtils.getEnumParameter(req, Role.class, "role");
                    Optional<Integer> gameId = ServletUtils.gameId(req);
                    if (gameId.isPresent() && role.isPresent()) {
                        resp.setContentType("text/plain");
                        PrintWriter out = resp.getWriter();
                        AbstractGame game = gameRepository.getGame(gameId.get());
                        out.print(llmService.getErrorMessage(game, role.get()).orElse(""));
                        out.flush();
                    } else {
                        logger.error("gameId ({}) or role ({}) are missing while trying to get an error message.",
                                gameId, role);
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    return;
                }
                default -> {
                    logger.error("Unknown action: {} with type {} and name {}", action, type, name);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }

            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.print(returnJson);
            out.flush();
        } else {
            logger.error("No action argument provided.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }

    /**
     * Change aspects of a single Large Language Model or strategy.
     *
     * <p>
     * Supported actions (formTypes):
     *
     * <p>
     *     - createModel -> Make a new model available.
     * </p>
     *
     * <p>
     *     - deleteModel -> Remove a model.
     * </p>
     *
     * <p>
     * - setActive -> The model identified by type and name is set as active or inactive, depending on the attribute
     * in the JSON object.
     *
     *
     * <p>
     * - updatePrompts -> The prompts of the model are adjusted according to the attributes in the JSON object.
     * The 'active' attribute is ignored.
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Gson gson = new Gson();
        String action = ServletUtils.formType(req);
        switch (action) {
            case "createModel" -> {
                Optional<LlModel> model = getModelFromRequest(req, resp);
                if (model.isPresent()) {
                    llmRepo.addNewModel(model.get());
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
                }
            }

            case "deleteModel" -> {
                Optional<LlModel> model = getModelFromRequest(req, resp);
                if (model.isPresent()) {
                    llmService.closeModel(model.get());
                    llmRepo.deleteModel(model.get());
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
                }
            }

            case "setActive" -> {
                LlModel model = gson.fromJson(req.getReader(), LlModel.class);
                if (model.getType() != null && model.getName() != null) {
                    llmRepo.setActive(model.getName(), model.getType(), model.isActive());
                    if (!model.isActive()) {
                        llmService.closeModel(model);
                    }
                } else {
                    logger.error("Name ({}) or type ({}) is missing for setActive-action",
                            model.getName(), model.getType());
                }
            }
            case "updatePrompts" -> {
                Optional<String> oldCustomName = ServletUtils.getStringParameter(req, "oldCustomName");
                Optional<String> newCustomName = ServletUtils.getStringParameter(req, "newCustomName");
                if (newCustomName.isPresent() && !newCustomName.get().matches("[a-zA-Z0-9_\\-]+")) {
                    logger.error("{} contains illegal characters.", newCustomName.get());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
                    return;
                }
                Optional<LlmDefaultStrategy> baseStrategy = ServletUtils.getEnumParameter(
                        req,
                        LlmDefaultStrategy.class,
                        "baseStrategy");
                Map<LlmPromptType, String> customPrompts = new HashMap<>();
                Iterator<String> otherParams = req.getParameterNames().asIterator();
                while (otherParams.hasNext()) {
                    String promptName = otherParams.next();
                    if (promptName.equals("oldCustomName")
                            || promptName.equals("newCustomName")
                            || promptName.equals("baseStrategy")
                            || promptName.equals("formType")) {
                        continue;
                    }
                    LlmPromptType promptType;
                    try {
                        promptType = LlmPromptType.valueOf(promptName);
                        customPrompts.put(promptType, ServletUtils.getStringParameter(req, promptName).get());

                    } catch (IllegalArgumentException e) {
                        logger.error("No such prompt type: {}", promptName);
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
                        return;
                    }

                }

                if (baseStrategy.isEmpty() || newCustomName.isEmpty()) {
                    logger.error("Missing base strategy {} or newCustomName {}", baseStrategy, newCustomName);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
                    return;
                }

                LlmStrategy customStrategy = new LlmStrategy(newCustomName.get(), baseStrategy.get());
                customStrategy.setCustomPrompts(customPrompts);
                if (oldCustomName.isPresent()) {
                    llmRepo.saveCustomStrategy(customStrategy, oldCustomName.get());
                } else {
                    llmRepo.saveCustomStrategy(customStrategy);
                }
                resp.setStatus(HttpServletResponse.SC_OK);

                logger.info("Map: {}", customPrompts);

                //llmRepo.updatePrompts(model);TODO FÜR STRATS STATT PROMPTS
                resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
            }
            case "deleteCustomStrat" -> {
                Optional<String> stratName = ServletUtils.getStringParameter(req, "stratName");
                stratName.ifPresent(llmRepo::deleteCustomStrategy);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
            }
            default -> logger.error("Unknown formType: {}", action);
        }


    }

    private Optional<LlModel> getModelFromRequest(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, IllegalArgumentException {
        Optional<LlmType> type = ServletUtils.getEnumParameter(req, LlmType.class, "llmType");
        if (type.isEmpty()) {
            logger.error("Could not create model from request: No support for LLM types {}", ServletUtils.getStringParameter(req, "llmType"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
            return Optional.empty();
        }
        Optional<String> name = ServletUtils.getStringParameter(req, "llmName");
        if (name.isEmpty()) {
            logger.error("Could not create model from request: No name was provided.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.sendRedirect(url.forPath(Paths.ADMIN_LLM_CONFIG));
            return Optional.empty();
        }
        return Optional.of(new LlModel(name.get(), type.get()));
    }

    private record LlmApiDTO(LlModel model, String strategy) {
    }
}
