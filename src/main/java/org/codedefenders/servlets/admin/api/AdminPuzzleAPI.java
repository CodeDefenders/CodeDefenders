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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTP based JSON API for {@link Puzzle Puzzles} and {@link PuzzleChapter PuzzleChapters}.
 *
 * <ul>
 * <li>
 *     {@code GET /admin/api/puzzles} returns all puzzles and chapters.
 * </li>
 * <li>
 *     {@code GET /admin/api/puzzles/puzzle?id=<id>} returns only the requested puzzle, a 404 if the requested puzzle could
 *     be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * <li>
 *     {@code GET /admin/api/puzzles/chapter?id=<id>} returns only the requested puzzle chapter, a 404 if the requested
 *     puzzle chapter could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * </ul>
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet({"/admin/api/puzzles", "/admin/api/puzzles/puzzle", "/admin/api/puzzles/chapter"})
public class AdminPuzzleAPI extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getServletPath();
        String message;
        switch (url) {
            case Paths.API_ADMIN_PUZZLES_ALL: {
                handleAllPuzzlesRequest(response);
                return;
            }
            case Paths.API_ADMIN_PUZZLE: {
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isPresent()) {
                    handlePuzzleRequest(response, puzzleId.get());
                    return;
                }
                message = "Missing puzzleId parameter.";
                break;
            }
            case Paths.API_ADMIN_PUZZLECHAPTER: {
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isPresent()) {
                    handlePuzzleChapterRequest(response, puzzleChapterId.get());
                    return;
                }
                message = "Missing puzzleChapterId parameter.";
                break;
            }
            default: {
                message = "Requested URL not available.";
            }
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, json.toString());
    }

    private void handleAllPuzzlesRequest(HttpServletResponse response) throws IOException {
        final List<PuzzleChapter> puzzleChapters = PuzzleDAO.getPuzzleChapters();
        final List<Puzzle> puzzles = PuzzleDAO.getPuzzles();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();

        // Sadly I haven't found a way around the parser and forming the json array by hand.
        // The 'normal' way (gson.toJson(puzzles)) somehow didn't work.
        JsonParser parser = new JsonParser();

        JsonArray puzzleArray = new JsonArray();
        for (Puzzle puzzle : puzzles) {
            puzzleArray.add(parser.parse(gson.toJson(puzzle)));
        }
        JsonArray puzzleChapterArray = new JsonArray();
        for (PuzzleChapter chapter : puzzleChapters) {
            puzzleChapterArray.add(parser.parse(gson.toJson(chapter)));
        }

        JsonObject json = new JsonObject();
        json.add("puzzles", puzzleArray);
        json.add("puzzleChapters", puzzleChapterArray);

        response.setStatus(HttpServletResponse.SC_OK);
        writeJSONResponse(response, json.toString());
    }

    private void handlePuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = PuzzleDAO.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                .create();
        String json = gson.toJson(puzzle);

        response.setStatus(HttpServletResponse.SC_OK);
        writeJSONResponse(response, json);
    }

    private void handlePuzzleChapterRequest(HttpServletResponse response, int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = PuzzleDAO.getPuzzleChapterForId(puzzleChapterId);
        if (puzzleChapter == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();
        String json = gson.toJson(puzzleChapter);

        response.setStatus(HttpServletResponse.SC_OK);
        writeJSONResponse(response, json);
    }

    /**
     * Writes a given JSON string to a given {@link HttpServletResponse}.
     * Also sets the content type to {@code application/json}.
     *
     * @param response the response the JSON is written to, never {@code null}.
     * @param json the JSON as a {@link String}.
     * @throws IOException when writing the JSON fails.
     */
    private static void writeJSONResponse(@Nonnull HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();
    }

    /**
     * Custom {@link TypeAdapter} to convert {@link Puzzle Puzzles} to JSON.
     * Currently does not support to convert JSON to puzzles.
     */
    private static class PuzzleTypeAdapter extends TypeAdapter<Puzzle> {
        @Override
        public void write(JsonWriter out, Puzzle puzzle) throws IOException {
            out.beginObject()
                .name("id").value(puzzle.getPuzzleId())
                .name("position").value(puzzle.getPosition())
                .name("title").value(puzzle.getTitle())
                .name("description").value(puzzle.getDescription())
                .name("maxAssertionsPerTest").value(puzzle.getMaxAssertionsPerTest())
                .name("forceHamcrest").value(puzzle.isForceHamcrest())
                .name("editableLinesStart").value(puzzle.getEditableLinesStart())
                .name("editableLinesEnd").value(puzzle.getEditableLinesEnd())
                .name("chapterId").value(puzzle.getChapterId())
                .name("classId").value(puzzle.getClassId())
                .endObject();
            out.close();
        }

        @Override
        public Puzzle read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException("Currently not implemented.");
        }
    }

    /**
     * Custom {@link TypeAdapter} to convert {@link PuzzleChapter PuzzleChapters} to JSON.
     * Currently does not support to convert JSON to puzzle chapters.
     */
    private static class PuzzleChapterTypeAdapter extends TypeAdapter<PuzzleChapter> {
        @Override
        public void write(JsonWriter out, PuzzleChapter chapter) throws IOException {
            out.beginObject()
                .name("id").value(chapter.getChapterId())
                .name("position").value(chapter.getPosition())
                .name("title").value(chapter.getTitle())
                .name("description").value(chapter.getDescription())
                .endObject();
            out.close();
        }

        @Override
        public PuzzleChapter read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException("Currently not implemented.");
        }
    }
}
