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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.http.HttpStatus;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.KillmapDAO.KillMapProgress;
import org.codedefenders.execution.KillMap.KillMapType;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet(Paths.API_KILLMAP_MANAGEMENT)
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

        String killmapTypeStr = request.getParameter("killmapType"); // class, game
        if (killmapTypeStr == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            logger.warn("Invalid request to killmap api: killmapType = null");
            return;
        }
        KillMapType killmapType;
        try {
            killmapType = KillMapType.valueOf(killmapTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            logger.warn("Invalid request to killmap api: killmapType = " + killmapTypeStr);
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
    private void doGetJSON(HttpServletResponse response, String dataType, KillMapType killmapType) throws IOException {
        response.setContentType("application/json");

        long timeStart = System.currentTimeMillis();

        List<? extends KillMapProgress> progresses = getData(dataType, killmapType);
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
    private void doGetCSV(HttpServletResponse response, String dataType, KillMapType killmapType) throws IOException {
        response.setContentType("text/csv");

        List<? extends KillMapProgress> progresses = getData(dataType, killmapType);
        if (progresses == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        String[] columns;
        switch (killmapType) {
            case CLASS:
                columns = new String[]{
                        "classId",
                        "className",
                        "classAlias",
                        "nrTests",
                        "nrMutants",
                        "nrEntries",
                        "nrExpectedEntries"
                };
                break;
            case GAME:
                columns = new String[]{
                        "gameId",
                        "gameMode",
                        "nrTests",
                        "nrMutants",
                        "nrEntries",
                        "nrExpectedEntries"
                };
                break;
            case CLASSROOM:
                columns = new String[]{
                        "classroomId",
                        "classroomName",
                        "nrTests",
                        "nrMutants",
                        "nrEntries",
                        "nrExpectedEntries"
                };
                break;
            default:
                logger.error("Unknown killmapType value: " + killmapType);
                return;
        }

        PrintWriter out = response.getWriter();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(columns).build());

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

    public List<? extends KillMapProgress> getData(String dataType, KillMapType killmapType) {
        if (dataType.equalsIgnoreCase("available")) {
            switch (killmapType) {
                case CLASS:
                    return KillmapDAO.getNonQueuedKillMapClassProgress();
                case GAME:
                    return KillmapDAO.getNonQueuedKillMapGameProgress();
                case CLASSROOM:
                    return KillmapDAO.getNonQueuedKillMapClassroomProgress();
                default:
                    logger.error("Unknown killmap type: " + killmapType);
                    return null;
            }
        } else if (dataType.equalsIgnoreCase("queue")) {
            switch (killmapType) {
                case CLASS:
                    return KillmapDAO.getQueuedKillMapClassProgress();
                case GAME:
                    return KillmapDAO.getQueuedKillMapGameProgress();
                case CLASSROOM:
                    return KillmapDAO.getQueuedKillMapClassroomProgress();
                default:
                    logger.error("Unknown killmap type: " + killmapType);
                    return null;
            }
        } else {
            logger.warn("Invalid request to killmap api: dataType = " + dataType);
        }

        return null;
    }
}
