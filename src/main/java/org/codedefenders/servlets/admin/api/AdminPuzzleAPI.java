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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.model.PuzzleInfo;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP based JSON API for {@link Puzzle Puzzles} and {@link PuzzleChapter PuzzleChapters}.
 *
 * <ul>
 * <li>
 *     {@code GET /admin/api/puzzles} returns all puzzles and chapters.
 * </li>
 * <li>
 *     {@code GET /admin/api/puzzles/puzzle?id=<id>} returns only the requested puzzle, a 404 if the requested puzzle
 *     could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * <li>
 *     {@code GET /admin/api/puzzles/chapter?id=<id>} returns only the requested puzzle chapter, a 404 if the requested
 *     puzzle chapter could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * <li>
 *     {@code PUT /admin/api/puzzles/puzzle?id=<id>} updates the requested puzzle with the content of the request body
 *     and returns a 404 if the requested puzzle could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * <li>
 *     {@code PUT /admin/api/puzzles/chapter?id=<id>} updates the requested puzzle with the content of the request body
 *     and returns a 404 if the requested puzzle could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * <li>
 *     {@code PUT /admin/api/puzzles/chapter?id=<id>} updates the requested puzzles with the content of the request body
 *     and returns a 404 if the requested puzzle chapter could be found or a 400 if the {@code id} parameter is missing.
 * </li>
 * </ul>
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet({Paths.API_ADMIN_PUZZLES_ALL, Paths.API_ADMIN_PUZZLE, Paths.API_ADMIN_PUZZLECHAPTER})
public class AdminPuzzleAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminPuzzleAPI.class);

    @Inject
    private PuzzleRepository puzzleRepo;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String url = request.getServletPath();
        String message;
        switch (url) {
            case Paths.API_ADMIN_PUZZLES_ALL: {
                handleGetAllPuzzlesRequest(response);
                return;
            }
            case Paths.API_ADMIN_PUZZLE: {
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isPresent()) {
                    handleGetPuzzleRequest(response, puzzleId.get());
                    return;
                }
                message = "Missing puzzleId parameter.";
                break;
            }
            case Paths.API_ADMIN_PUZZLECHAPTER: {
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isPresent()) {
                    handleGetPuzzleChapterRequest(response, puzzleChapterId.get());
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

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getServletPath();
        String message;
        switch (url) {
            case Paths.API_ADMIN_PUZZLE: {
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isPresent()) {
                    handleUpdatePuzzle(request, response, puzzleId.get());
                    return;
                }
                message = "Missing puzzleId parameter.";
                break;
            }
            case Paths.API_ADMIN_PUZZLECHAPTER: {
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isPresent()) {
                    handleUpdatePuzzleChapter(request, response, puzzleChapterId.get());
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

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getServletPath();
        String message;
        switch (url) {
            case Paths.API_ADMIN_PUZZLE: {
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isPresent()) {
                    handleDeletePuzzleRequest(response, puzzleId.get());
                    return;
                }
                message = "Missing puzzleId parameter.";
                break;
            }
            case Paths.API_ADMIN_PUZZLECHAPTER: {
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isPresent()) {
                    handleDeletePuzzleChapterRequest(response, puzzleChapterId.get());
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

    private void handleDeletePuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String message;
        if (puzzleRepo.gamesExistsForPuzzle(puzzle)) {
            if (puzzleRepo.setPuzzleActive(puzzle, false)) {
                response.setStatus(HttpServletResponse.SC_OK);
                message = "Set puzzle " + puzzleId + " as inactive";
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                message = "Failed to set puzzle " + puzzleId + " as inactive";
            }
        } else {
            GameClass parentGameClass = puzzleRepo.getParentGameClass(puzzle.getClassId());

            // Uses 'cascade delete'. Deleted the puzzles, too.
            boolean classRemoved = GameClassDAO.forceRemoveClassForId(puzzle.getClassId());
            if (classRemoved) {
                if (puzzleRepo.classSourceUsedForPuzzleClasses(parentGameClass.getId())) {
                    logger.info("Puzzle class {} removed, but parent class {} is still used for other puzzles.",
                            puzzle.getClassId(), parentGameClass.getId());
                } else {
                    logger.info("Puzzle class {} removed and since parent class {} is not used for other puzzles,"
                                    + "it's removed too.",
                            puzzle.getClassId(), parentGameClass.getId());
                    GameClassDAO.forceRemoveClassForId(parentGameClass.getId());

                    // Remove the whole class folder
                    final String javaFile = parentGameClass.getJavaFile();
                    final Path parent = java.nio.file.Paths.get(javaFile).getParent();
                    try {
                        org.apache.commons.io.FileUtils.forceDelete(parent.toFile());
                    } catch (IOException e) {
                        logger.error("Failed to remove folder for deleted class " + parentGameClass.getAlias(), e);
                    }
                }
            }

            if (classRemoved) {
                response.setStatus(HttpServletResponse.SC_OK);
                message = "Removed puzzle " + puzzleId + " successfully";
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                message = "Failed to remove puzzle " + puzzleId;
            }
        }

        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, json.toString());
    }

    private void handleDeletePuzzleChapterRequest(HttpServletResponse response,
                                                  int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
        if (puzzleChapter == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String message;
        if (puzzleRepo.removePuzzleChapter(puzzleChapter)) {
            response.setStatus(HttpServletResponse.SC_OK);
            message = "Removed puzzle chapter " + puzzleChapterId + " successfully";
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            message = "Failed to remove puzzle chapter " + puzzleChapterId;
        }

        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, json.toString());
    }

    private void handleGetAllPuzzlesRequest(HttpServletResponse response) throws IOException {
        final List<PuzzleChapter> puzzleChapters = puzzleRepo.getPuzzleChapters();
        final List<Puzzle> puzzles = puzzleRepo.getPuzzles();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();

        // Sadly I haven't found a way around the parser and creating the json array by hand.
        // The 'normal' way (gson.toJson(puzzles)) somehow didn't work.

        JsonArray puzzleArray = new JsonArray();
        for (Puzzle puzzle : puzzles) {
            puzzleArray.add(JsonParser.parseString(gson.toJson(puzzle)));
        }
        JsonArray puzzleChapterArray = new JsonArray();
        for (PuzzleChapter chapter : puzzleChapters) {
            puzzleChapterArray.add(JsonParser.parseString(gson.toJson(chapter)));
        }

        JsonObject json = new JsonObject();
        json.add("puzzles", puzzleArray);
        json.add("puzzleChapters", puzzleChapterArray);

        response.setStatus(HttpServletResponse.SC_OK);
        writeJSONResponse(response, json.toString());
    }

    private void handleGetPuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
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

    private void handleGetPuzzleChapterRequest(HttpServletResponse response, int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
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

    private void handleUpdatePuzzle(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int puzzleId) throws IOException {
        Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Optional<JsonObject> body = readJSONBody(request);

        String message;
        if (body.isPresent()) {
            JsonObject json = body.get();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                    .create();
            Puzzle parsedPuzzle = gson.fromJson(json, Puzzle.class);

            if (puzzleId != parsedPuzzle.getPuzzleId()) {
                message = "Identifier from URL and body do not match.";
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                boolean updateSuccess = puzzleRepo.updatePuzzle(PuzzleInfo.of(parsedPuzzle));

                if (updateSuccess) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    message = "Updated puzzle " + puzzleId + " successfully.";
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    message = "Failed to update puzzle " + puzzleId;
                }
            }
        } else {
            message = "No valid request body provided";
        }

        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, json.toString());
    }

    private void handleUpdatePuzzleChapter(HttpServletRequest request,
                                           HttpServletResponse response,
                                           int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
        if (puzzleChapter == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Optional<JsonObject> body = readJSONBody(request);

        String message;
        if (body.isPresent()) {
            JsonObject json = body.get();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                    .create();
            PuzzleChapter parsedPuzzleChapter = gson.fromJson(json, PuzzleChapter.class);
            if (puzzleChapterId != parsedPuzzleChapter.getChapterId()) {
                message = "Identifier from URL and body do not match.";
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                boolean updateSuccess = puzzleRepo.updatePuzzleChapter(parsedPuzzleChapter);
                if (updateSuccess) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    message = "Updated puzzle chapter " + puzzleChapterId + " successfully.";
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    message = "Failed to update puzzle chapter " + puzzleChapterId;
                }
            }
        } else {
            message = "No valid request body provided";
        }

        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, json.toString());
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
     * Reads and returns the body content of a given request, iff it contains any AND the body is valid JSON.
     *
     * @param request the request which body is read and returned.
     * @return  An {@link Optional} holding either the request body content as a
     *          {@link JsonObject}, or an instance of {@link Optional#empty()}.
     * @throws IOException when reading the request body fails.
     */
    private static Optional<JsonObject> readJSONBody(@Nonnull HttpServletRequest request) throws IOException {
        String json = request.getReader().lines().collect(Collectors.joining());

        JsonObject obj;
        try {
            obj = JsonParser.parseString(json)
                .getAsJsonObject();
        } catch (JsonSyntaxException e) {
            obj = null;
        }
        return Optional.ofNullable(obj);
    }

    /**
     * Custom {@link TypeAdapter} to convert {@link PuzzleInfo Puzzle information} to JSON.
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
                    .name("editableLinesStart").value(puzzle.getEditableLinesStart())
                    .name("editableLinesEnd").value(puzzle.getEditableLinesEnd())
                    .name("chapterId").value(puzzle.getChapterId())
                    .name("classId").value(puzzle.getClassId())
                    .endObject();
            out.close();
        }

        @Override
        public Puzzle read(JsonReader in) throws IOException {
            JsonObject json = JsonParser.parseReader(in)
                    .getAsJsonObject();

            int puzzleId = json.get("id").getAsInt();
            Integer chapterId = json.get("chapterId").isJsonNull() ? null : json.get("chapterId").getAsInt();
            Integer position = json.get("position").isJsonNull() ? null : json.get("position").getAsInt();
            String title = json.get("title").getAsString();
            String description = json.get("description").getAsString();
            int maxAssertionsPerTest = json.get("maxAssertionsPerTest").getAsInt();
            Integer editableLinesStart =
                    json.get("editableLinesStart").isJsonNull() ? null : json.get("editableLinesStart").getAsInt();
            Integer editableLinesEnd =
                    json.get("editableLinesEnd").isJsonNull() ? null : json.get("editableLinesEnd").getAsInt();

            return Puzzle.forPuzzleInfo(puzzleId, chapterId, position, title, description, maxAssertionsPerTest,
                    editableLinesStart, editableLinesEnd);
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
            JsonObject json = JsonParser.parseReader(in)
                    .getAsJsonObject();

            int chapterId = json.get("id").getAsInt();
            Integer position = json.get("position").isJsonNull() ? null : json.get("position").getAsInt();
            String title = json.get("title").getAsString();
            String description = json.get("description").getAsString();

            return new PuzzleChapter(chapterId, position, title, description);
        }
    }
}
