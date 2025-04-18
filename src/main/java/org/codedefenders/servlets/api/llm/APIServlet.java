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
