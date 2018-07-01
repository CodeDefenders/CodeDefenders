package org.codedefenders.servlets.admin.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.api.ApiUtils;
import org.codedefenders.api.analytics.ClassDataDTO;
import org.codedefenders.api.analytics.UserDataDTO;
import org.codedefenders.database.ApiDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class AdminAnalyticsClassesApi extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsClassesApi.class);

    /**
     * Returns a JSON or CSV file containing the class analytics data.
     * <p></p>
     * The URL parameter {@code type} specifies the type of data to return:<br>
     * {@code type=json} will return JSON, {@code type=CSV} will return CSV.<br>
     * If {@code type} is not specified, JSON will be returned.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String type = request.getParameter("type");
        if (type == null) {
            type = "json";
        }

        if (type.equalsIgnoreCase("json")) {
            getJSON(request, response);
        } else if (type.equalsIgnoreCase("csv")) {
            getCSV(request, response);
        } else {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
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
    private void getJSON(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        long timeStart = System.currentTimeMillis();
        List<ClassDataDTO> classData = ApiDAO.getAnalyticsClassData();
        long timeEnd = System.currentTimeMillis();

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("timestamp", gson.toJsonTree(System.currentTimeMillis()));
        root.add("processingTime", gson.toJsonTree(timeEnd - timeStart));
        root.add("data", gson.toJsonTree(classData));

        out.print(gson.toJson(root));
        out.flush();
    }

    /**
     * Returns a CSV file containing the user analytics data.
     * The returned CSV will have a header.
     */
    private void getCSV(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");

        List<ClassDataDTO> classData = ApiDAO.getAnalyticsClassData();

        String[] columns = new String[]{
            "id",
            "classname",
            "games",
            "testsSubmitted",
            "mutantsSubmitted",
            "mutantsAlive",
            "mutantsEquivalent",
        };

        PrintWriter out = response.getWriter();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(columns));

        for (ClassDataDTO clazz : classData) {
            for(String column : columns) {
                try {
                    csvPrinter.print(PropertyUtils.getProperty(clazz, column));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            csvPrinter.println();
        }

        csvPrinter.flush();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException { }
}