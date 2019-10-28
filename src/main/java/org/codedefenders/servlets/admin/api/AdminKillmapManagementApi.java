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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.KillmapDAO.KillMapProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;

@WebServlet("/admin/api/killmapmanagement")
public class AdminKillmapManagementApi extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(AdminKillmapManagementApi.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String dataType = request.getParameter("dataType"); // available, queue
        if (dataType == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            logger.warn("Invalid request to killmap api: dataType = null");
            return;
        }

        String killmapType = request.getParameter("killmapType"); // class, game
        if (killmapType == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            logger.warn("Invalid request to killmap api: killmapType = null");
            return;
        }

        String fileType = request.getParameter("fileType"); // json, csv
        if (fileType == null) {
            fileType = "json";
        }

        if (fileType.equalsIgnoreCase("json")) {
            doGetJSON(response, dataType, killmapType);
        } else if (fileType.equalsIgnoreCase("csv")) {
            doGetCSV(response, dataType, killmapType);
        } else {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            logger.warn("Invalid request to killmap api: fileType = " + fileType);
        }
    }

    /**
     * Returns a JSON file containing the class analytics data.<br>
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
    private void doGetJSON(HttpServletResponse response, String dataType, String killmapType) throws IOException {
        response.setContentType("application/json");

        long timeStart = System.currentTimeMillis();

        List <? extends KillMapProgress> progresses = getData(dataType, killmapType);
        if (progresses == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        long timeEnd = System.currentTimeMillis();

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("timestamp", gson.toJsonTree(Instant.now().getEpochSecond()));
        root.add("processingTime", gson.toJsonTree(timeEnd - timeStart));
        root.add("data", gson.toJsonTree(progresses));

        out.print(gson.toJson(root));
        out.flush();
    }

    /**
     * Returns a CSV file containing the user analytics data.
     * The returned CSV will have a header.
     */
    private void doGetCSV(HttpServletResponse response, String dataType, String killmapType) throws IOException {
        response.setContentType("text/csv");

        List <? extends KillMapProgress> progresses = getData(dataType, killmapType);
        if (progresses == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        String[] columns;
        if (killmapType.equalsIgnoreCase("class")) {
            columns = new String[]{
                "classId",
                "className",
                "classAlias",
                "nrTests",
                "nrMutants",
                "nrEntries"
            };
        } else {
            columns = new String[]{
                "gameId",
                "gameMode",
                "nrTests",
                "nrMutants",
                "nrEntries"
            };
        }

        PrintWriter out = response.getWriter();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(columns));

        for (KillMapProgress progress : progresses) {
            try {
                for (String column : columns) {
                    csvPrinter.print(PropertyUtils.getProperty(progress, column));
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            csvPrinter.println();
        }

        csvPrinter.flush();
    }

    public List<? extends KillMapProgress> getData(String dataType, String killmapType) {
        if (dataType.equalsIgnoreCase("available")) {
            if (killmapType.equalsIgnoreCase("class")) {
                return KillmapDAO.getNonQueuedKillMapClassProgress();
            } else if (killmapType.equalsIgnoreCase("game")) {
                return KillmapDAO.getNonQueuedKillMapGameProgress();
            } else {
                logger.warn("Invalid request to killmap api: killmapType = " + killmapType);
            }
        } else if (dataType.equalsIgnoreCase("queue")) {
            if (killmapType.equalsIgnoreCase("class")) {
                return KillmapDAO.getQueuedKillMapClassProgress();
            } else if (killmapType.equalsIgnoreCase("game")) {
                return KillmapDAO.getQueuedKillMapGameProgress();
            } else {
                logger.warn("Invalid request to killmap api: killmapType = " + killmapType);
            }
        } else {
            logger.warn("Invalid request to killmap api: dataType = " + dataType);
        }

        return null;
    }
}
