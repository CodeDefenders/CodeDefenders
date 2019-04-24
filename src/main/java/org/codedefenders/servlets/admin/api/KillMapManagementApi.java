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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;

public class KillMapManagementApi extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(KillMapManagementApi.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String page = request.getParameter("pageType"); // manual, available, queue
        if (page == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST); // TODO return an error message
            return;
        }

        String killmapType = request.getParameter("killmapType"); // class, game
        if (killmapType == null) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST); // TODO return an error message
            return;
        }

        String filetype = request.getParameter("fileType");
        if (filetype == null) {
            filetype = "json";
        }

        if (filetype.equalsIgnoreCase("json")) { // TODO: case sensitive or case insensitive
            doGetJSON(response, page, killmapType);
        } else if (filetype.equalsIgnoreCase("csv")) {
            doGetCSV(response, page, killmapType);
        } else {
            response.setStatus(HttpStatus.SC_BAD_REQUEST); // TODO return an error message
        }
    }

    private void doGetJSON(HttpServletResponse response, String page, String killmapType) throws IOException {
        response.setContentType("application/json");

        long timeStart = System.currentTimeMillis();

        List <? extends KillMapProgress> progresses = getData(page, killmapType);
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

    private void doGetCSV(HttpServletResponse response, String page, String killmapType) throws IOException {
        response.setContentType("text/csv");

        List <? extends KillMapProgress> progresses = getData(page, killmapType);
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

    public List<? extends KillMapProgress> getData(String page, String killmapType) {
        if (page.equalsIgnoreCase("available")) {
            if (killmapType.equalsIgnoreCase("class")) {
                return KillmapDAO.getNonQueuedKillMapClassProgress();
            } else if (killmapType.equalsIgnoreCase("game")) {
                return KillmapDAO.getNonQueuedKillMapGameProgress();
            }
        } else if (page.equalsIgnoreCase("queue")) {
            if (killmapType.equalsIgnoreCase("class")) {
                return KillmapDAO.getQueuedKillMapClassProgress();
            } else if (killmapType.equalsIgnoreCase("game")) {
                return KillmapDAO.getQueuedKillMapGameProgress();
            }
        }

        return null;
    }
}
