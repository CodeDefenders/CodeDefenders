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
package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.util.JSONUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class APIServlet extends HttpServlet {
    protected void writeResponse(HttpServletResponse response, int statusCode, Object responseBody) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JSONUtils.OptionalTypeAdapterFactory())
                .serializeNulls()
                .create();
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setStatus(statusCode);
        gson.toJson(responseBody, out);
        out.flush();
    }
}
