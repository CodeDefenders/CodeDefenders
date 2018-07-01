package org.codedefenders.servlets.admin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysql.cj.xdevapi.JsonString;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.time.DateUtils;
import org.codedefenders.api.analytics.UserDataDTO;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.servlets.util.Redirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class AdminAnalyticsUsersApi extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsUsersApi.class);

    /**
     * Returns a JSON or CSV file containing the user analytics data.
     * <p></p>
     * The URL parameter {@code type} specifies the type of data to return:<br>
     * {@code type=json} will return JSON, {@code type=CSV} will return CSV.<br>
     * If {@code type} is not specified, JSON will be returned.
     * <p></p>
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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String type = request.getParameter("type");
        if (type == null) {
            type = "json";
        }

        if (type.equalsIgnoreCase("csv")) {
            response.setContentType("text/csv");

            List<UserDataDTO> users = DatabaseAccess.getAnalyticsUserData();

            String[] header = new String[]{
                "id",
                "username",
                "gamesPlayed",
                "attackerScore",
                "defenderScore",
                "mutantsSubmitted",
                "mutantsAlive",
                "mutantsEquivalent",
                "testsSubmitted",
                "mutantsKilled"
            };

            PrintWriter out = response.getWriter();
            CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(header));

            for (UserDataDTO user : users) {
                for(String column : header) {
                    try {
                        csvPrinter.print(PropertyUtils.getProperty(user, column));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
                csvPrinter.println();
            }

            csvPrinter.flush();

        } else if (type.equalsIgnoreCase("json")) {
            response.setContentType("application/json");

            long timeStart = System.currentTimeMillis();
            List<UserDataDTO> users = DatabaseAccess.getAnalyticsUserData();
            long timeEnd = System.currentTimeMillis();

            PrintWriter out = response.getWriter();
            Gson gson = new Gson();

            JsonObject root = new JsonObject();
            root.add("timestamp", gson.toJsonTree(System.currentTimeMillis()));
            root.add("processingTime", gson.toJsonTree(timeEnd - timeStart));
            root.add("data", gson.toJsonTree(users));

            out.print(gson.toJson(root));
            out.flush();

        } else {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException { }
}