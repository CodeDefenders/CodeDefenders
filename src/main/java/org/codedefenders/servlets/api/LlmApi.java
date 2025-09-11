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
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.model.LLMType;
import org.codedefenders.model.LLModel;
import org.codedefenders.persistence.database.LLMRepository;
import org.codedefenders.service.LlmService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

/**
 * This class should provide details and interactions with the Large Language Models uses by
 * {@link org.codedefenders.service.LlmService}, integrated to normal games. It is not affiliated with the
 * {@link org.codedefenders.servlets.api.llm} package.
 */
@WebServlet(Paths.API_LLM)
public class LlmApi extends HttpServlet {
    Logger logger = LoggerFactory.getLogger(LlmApi.class);

    @Inject
    LLMRepository llmRepo;

    @Inject
    URLUtils url;

    @Inject
    private LlmService llmService;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO: Authentication

        Optional<String> action = ServletUtils.getStringParameter(req, "action");
        Optional<LLMType> type = ServletUtils.getEnumParameter(req, LLMType.class, "type");
        Optional<String> name = ServletUtils.getStringParameter(req, "name");
        if (action.isEmpty()) {
            logger.error("No action argument provided.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } else {
            String returnJson;
            Gson gson = new Gson();
            switch (action.get()) {
                case "getall" -> {
                    List<LLModel> models = llmRepo.getAllModels();
                    Type typeOfSrc = new TypeToken<List<LLModel>>() {
                    }.getType();
                    returnJson = gson.toJson(models, typeOfSrc);
                }
                case "get" -> {
                    if (name.isPresent() && type.isPresent()) {
                        Optional<LLModel> model = llmRepo.getModelFromName(name.get(), type.get(), false);
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
                default -> {
                    logger.error("Unknown action: {} with type {} and name {}", action, type, name);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }

            logger.info(returnJson);//TODO Entfernen!
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.print(returnJson);
            out.flush();
        }

    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        Gson gson = new Gson();
        LLModel model = gson.fromJson(req.getReader(), LLModel.class);
        String action = ServletUtils.formType(req);
        switch (action) {
            case "setActive" -> {
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
                llmRepo.updatePrompts(model);
                Redirect.redirectBack(req, resp);
                resp.sendRedirect(url.forPath(Paths.ADMIN_LLM));
            }
            case "resetDefault" -> {
                llmRepo.resetDefaultModel();
                resp.sendRedirect(url.forPath(Paths.ADMIN_LLM));
            }
            default -> logger.error("Unknown formType: {}", action);
        }


    }
}
