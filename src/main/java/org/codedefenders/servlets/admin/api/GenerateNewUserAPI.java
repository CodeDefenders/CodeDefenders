/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets.admin.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.Test;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.APIUtils;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MissingRequiredPropertiesException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/admin/api/auth/newUser")
public class GenerateNewUserAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GenerateNewUserAPI.class);
    final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>() {
        {
            put("name", String.class);
        }
    };
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Map<String, Object> params;
        try {
            params = APIUtils.getParametersOrRespondJsonError(request, response, parameterTypes);
        } catch (MissingRequiredPropertiesException e) {
            return;
        }
        String name = (String) params.get("name");
        CodeDefendersValidator validator = new CodeDefendersValidator();
        if (!(validator.validUsername(name))) {
            APIUtils.respondJsonError(response,
                    "Could not create user. Invalid username. Use 3-20 alphanumerics starting with a lowercase letter (a-z), no space or special characters.");
            return;
        }

        String prefix = login.getUser().getName();
        String newName;
        Random random = new Random();
        String email;
        //Generate new name in format prefix_name_NNNN
        do {
            String num = String.format("%1$4s", random.nextInt(9999)).replace(' ', '0');
            newName = prefix + "_" + name + "_" + num;
            email = name + "_" + num + "@" + prefix;
        } while (userRepository.getUserByName(newName).isPresent() || userRepository.getUserByEmail(email).isPresent());
        PrintWriter out = response.getWriter();
        UserEntity newUser = new UserEntity(newName, "EXTERNAL_USER", email, true);
        newUser.setToken(userRepository.generateNewUserToken());
        try {
            userRepository.insert(newUser).get(); //If present operation succeeded, so if operation failed this throws NoSuchElementException
            newUser = userRepository.getUserByName(newName).get();
            response.setContentType("application/json");
            Gson gson = new Gson();
            JsonObject root = new JsonObject();
            root.add("userId", gson.toJsonTree(newUser.getId(), Integer.class));
            root.add("username", gson.toJsonTree(newUser.getUsername(), String.class));
            root.add("token", gson.toJsonTree(newUser.getToken(), String.class));
            out.print(new Gson().toJson(root));
            out.flush();
        } catch (NoSuchElementException e) {
            APIUtils.respondJsonError(response, "Could not create user", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
