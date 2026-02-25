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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.Role;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.persistence.database.LlmRepository;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(Paths.ADMIN_LLM_CONFIG)
public class AdminLlmConfig extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminLlmConfig.class);

    @Inject
    LlmRepository llmRepo;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<LlModel> models = llmRepo.getAllModels();
        List<LlmStrategy> attackStrategies = llmRepo.getAttackStrategies();
        List<LlmStrategy> defendStrategies = llmRepo.getDefendStrategies();
        List<LlmStrategy> equivalenceStrategies = llmRepo.getEquivalenceStrategies();
        request.setAttribute("models", models);
        request.setAttribute("attackStrategies", attackStrategies);
        request.setAttribute("defendStrategies", defendStrategies);
        request.setAttribute("equivalenceStrategies", equivalenceStrategies);
        request.getRequestDispatcher(Constants.ADMIN_LLM_CONFIG_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    }
}
