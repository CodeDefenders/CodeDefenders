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

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.arcade.mutle.service.MutleGuestSession;
import org.codedefenders.arcade.mutle.service.MutleService;
import org.codedefenders.arcade.mutle.service.MutleSettings;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.util.Paths;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

@WebServlet(Paths.API_MUTLE + "/*")
public class MutleApiServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    @Inject
    private MutleService mutleService;

    @Inject
    private MutleSettings settings;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MutleGuestSession guest;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!settings.enabled() || !mutleService.isReady()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (request.getPathInfo() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            JsonObject body = JsonParser.parseReader(request.getReader()).getAsJsonObject();
            switch (request.getPathInfo()) {
                case "/start" -> {
                    String fixtureKey = optionalString(body, "fixtureKey");
                    if (fixtureKey != null && !login.isAdmin()) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    write(response, !login.isLoggedIn() ? guest.start() : fixtureKey == null
                            ? mutleService.start(login.getUserId())
                            : mutleService.startPractice(login.getUserId(), fixtureKey));
                }
                case "/state" -> write(response, login.isLoggedIn()
                        ? mutleService.state(login.getUserId(), integer(body, "gameId", false))
                        : guest.state(integer(body, "gameId", false)));
                case "/guess" -> write(response, login.isLoggedIn()
                        ? mutleService.submit(login.getUserId(), integer(body, "gameId", true), string(body, "source"))
                        : guest.submit(integer(body, "gameId", true), string(body, "source")));
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (MutleService.NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (MutleService.SubmissionInProgressException e) {
            response.sendError(HttpServletResponse.SC_CONFLICT);
        } catch (MutleService.RejectedSubmissionException e) {
            response.setStatus(422);
            write(response, new ErrorView("rejected", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException | JsonParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private static Integer integer(JsonObject body, String name, boolean required) {
        if (!body.has(name) || body.get(name).isJsonNull()) {
            if (required) {
                throw new IllegalArgumentException(name + " is missing");
            }
            return null;
        }
        if (!body.get(name).isJsonPrimitive() || !body.get(name).getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        try {
            int value = body.get(name).getAsBigDecimal().intValueExact();
            if (value < 1) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " must be an integer", e);
        }
    }

    private static String string(JsonObject body, String name) {
        if (!body.has(name) || !body.get(name).isJsonPrimitive() || !body.get(name).getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        String value = body.get(name).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String optionalString(JsonObject body, String name) {
        if (!body.has(name) || body.get(name).isJsonNull()) {
            return null;
        }
        return string(body, name);
    }

    private static void write(HttpServletResponse response, Object value) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(GSON.toJson(value));
    }

    private record ErrorView(String error, String message) {
    }
}
