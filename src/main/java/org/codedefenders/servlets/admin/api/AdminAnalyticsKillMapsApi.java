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
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.http.HttpStatus;
import org.codedefenders.api.analytics.KillmapDataDTO;
import org.codedefenders.database.AnalyticsDAO;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet({Paths.API_ANALYTICS_KILLMAP})
public class AdminAnalyticsKillMapsApi extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsKillMapsApi.class);

    /**
     * Returns a JSON or CSV file containing the killmap analytics data.
     * <p></p>
     * The URL parameter {@code type} specifies the type of data to return:<br>
     * {@code type=json} will return JSON, {@code type=CSV} will return CSV.<br>
     * If {@code type} is not specified, JSON will be returned.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String type = request.getParameter("fileType");
        if (type == null) {
            type = "json";
        }

        if (type.equalsIgnoreCase("json")) {
            doGetJSON(response);
        } else if (type.equalsIgnoreCase("csv")) {
            doGetCSV(response);
        } else {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Returns a JSON file containing the killmap analytics data.<br>
     * The returned JSON will have the following format:<br>
     * <pre>
     * {
     *     timestamp: ...,
     *     processingTime: ...,
     *     data: [
     *          ...
     *     ]
     * }
     * </pre>
     */
    private void doGetJSON(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        long timeStart = System.currentTimeMillis();
        List<KillmapDataDTO> classData = AnalyticsDAO.getAnalyticsKillMapData();
        long timeEnd = System.currentTimeMillis();

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("timestamp", gson.toJsonTree(Instant.now().getEpochSecond()));
        root.add("processingTime", gson.toJsonTree(timeEnd - timeStart));
        root.add("data", gson.toJsonTree(classData));

        out.print(gson.toJson(root));
        out.flush();
    }

    /**
     * Returns a CSV file containing the killmap analytics data.
     * The returned CSV will have a header.
     */
    private void doGetCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");

        List<KillmapDataDTO> killmapData = AnalyticsDAO.getAnalyticsKillMapData();

        String[] columns = new String[]{
            "userId",
            "userName",
            "classId",
            "className",
            "role",
            "usefulMutants",
            "usefulTests"
        };

        PrintWriter out = response.getWriter();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(columns).build());

        for (KillmapDataDTO k : killmapData) {
            for (String column : columns) {
                try {
                    csvPrinter.print(PropertyUtils.getProperty(k, column));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            csvPrinter.println();
        }

        csvPrinter.flush();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    }
}
