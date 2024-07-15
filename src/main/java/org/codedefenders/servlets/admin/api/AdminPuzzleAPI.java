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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.model.PuzzleInfo;
import org.codedefenders.persistence.database.GameClassRepository;
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

    @Inject
    private GameClassRepository gameClassRepo;

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
                if (request.getParameter("create") != null) {
                    handleCreatePuzzleChapter(request, response);
                    return;
                } else {
                    final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                    if (puzzleChapterId.isPresent()) {
                        handleUpdatePuzzleChapter(request, response, puzzleChapterId.get());
                        return;
                    }
                }
                message = "Missing puzzleChapterId or create parameter.";
                break;
            }
            case Paths.API_ADMIN_PUZZLES_ALL: {
                handleBatchUpdatePuzzlePositions(request, response);
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
                final Optional<String> action = ServletUtils.getStringParameter(request, "action");
                if (puzzleId.isPresent() && action.isPresent()) {
                    if (action.get().equals("archive")) {
                        handleArchivePuzzleRequest(response, puzzleId.get());
                    } else if (action.get().equals("delete")) {
                        handleDeletePuzzleRequest(response, puzzleId.get());
                    }
                    return;
                }
                message = "Missing parameter.";
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

    private void handleArchivePuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String message;
        if (puzzleRepo.setPuzzleActive(puzzle, false)) {
            response.setStatus(HttpServletResponse.SC_OK);
            message = "Archived puzzle " + puzzleId;
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            message = "Failed to archive puzzle " + puzzleId;
        }

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

        if (puzzleRepo.gamesExistsForPuzzle(puzzle)) {
            JsonObject json = new JsonObject();
            json.add("message",
                    new JsonPrimitive("Cannot delete puzzle " + puzzleId + " because games exist for that puzzle."));
            writeJSONResponse(response, json.toString());
            return;
        }

        GameClass parentGameClass = puzzleRepo.getParentGameClass(puzzle.getClassId());

        // Uses 'cascade delete'. Deleted the puzzles, too.
        boolean classRemoved = gameClassRepo.forceRemoveClassForId(puzzle.getClassId());
        if (classRemoved) {
            if (puzzleRepo.classSourceUsedForPuzzleClasses(parentGameClass.getId())) {
                logger.info("Puzzle class {} removed, but parent class {} is still used for other puzzles.",
                        puzzle.getClassId(), parentGameClass.getId());
            } else {
                logger.info("Puzzle class {} removed and since parent class {} is not used for other puzzles,"
                                + "it's removed too.",
                        puzzle.getClassId(), parentGameClass.getId());
                gameClassRepo.forceRemoveClassForId(parentGameClass.getId());

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

        String message;
        if (classRemoved) {
            response.setStatus(HttpServletResponse.SC_OK);
            message = "Removed puzzle " + puzzleId + " successfully";
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            message = "Failed to remove puzzle " + puzzleId;
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
        final List<AdminPuzzleInfo> puzzles = puzzleRepo.getAdminPuzzleInfos();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .registerTypeAdapter(AdminPuzzleInfo.class, new AdminPuzzleInfoTypeAdapter())
                .serializeNulls()
                .create();
        JsonArray puzzleArray = new JsonArray();
        for (AdminPuzzleInfo puzzleInfo : puzzles) {
            puzzleArray.add(gson.toJsonTree(puzzleInfo));
        }
        JsonArray puzzleChapterArray = new JsonArray();
        for (PuzzleChapter chapter : puzzleChapters) {
            puzzleChapterArray.add(gson.toJsonTree(chapter));
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

    private void handleCreatePuzzleChapter(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<JsonObject> body = readJSONBody(request);
        if (body.isEmpty()) {
            JsonObject json = new JsonObject();
            json.add("message", new JsonPrimitive("No valid request body provided"));
            writeJSONResponse(response, json.toString());
            return;
        }

        JsonObject json = body.get();
        Gson gson = new GsonBuilder().create();
        var data = gson.fromJson(json, AdminCreateChapterData.class);

        int maxPosition = puzzleRepo.getPuzzleChapters().stream()
                    .mapToInt(PuzzleChapter::getPosition)
                    .max().orElse(0);

        puzzleRepo.storePuzzleChapter(new PuzzleChapter(-1, maxPosition + 1, data.title, data.description));
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.add("message", new JsonPrimitive("Successfully create puzzle chapter."));
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

    private void handleBatchUpdatePuzzlePositions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<JsonObject> optBody = readJSONBody(request);
        if (optBody.isEmpty()) {
            JsonObject json = new JsonObject();
            json.add("message", new JsonPrimitive("No valid request body provided"));
            writeJSONResponse(response, json.toString());
            return;
        }

        JsonObject body = optBody.get();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();

        AdminPuzzlePositions positions = gson.fromJson(body, AdminPuzzlePositions.class);
        puzzleRepo.batchUpdatePuzzlePositions(positions);

        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive("Successfully updated puzzle and chapter positions."));
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

    private static class AdminPuzzleInfoTypeAdapter extends TypeAdapter<AdminPuzzleInfo> {
        @Override
        public void write(JsonWriter out, AdminPuzzleInfo info) throws IOException {
            out.beginObject()
                    .name("id").value(info.puzzle.getPuzzleId())
                    .name("position").value(info.puzzle.getPosition())
                    .name("title").value(info.puzzle.getTitle())
                    .name("description").value(info.puzzle.getDescription())
                    .name("maxAssertionsPerTest").value(info.puzzle.getMaxAssertionsPerTest())
                    .name("editableLinesStart").value(info.puzzle.getEditableLinesStart())
                    .name("editableLinesEnd").value(info.puzzle.getEditableLinesEnd())
                    .name("chapterId").value(info.puzzle.getChapterId())
                    .name("classId").value(info.puzzle.getClassId())
                    .name("activeRole").value(info.puzzle.getActiveRole().name())
                    .name("gameCount").value(info.gameCount)
                    .name("active").value(info.active)
                    .endObject();
        }

        @Override
        public AdminPuzzleInfo read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private static class AdminPuzzlePositionsTypeAdapter extends TypeAdapter<AdminPuzzleInfo> {
        @Override
        public void write(JsonWriter out, AdminPuzzleInfo info) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public AdminPuzzleInfo read(JsonReader in) throws IOException {
            JsonObject json = JsonParser.parseReader(in).getAsJsonObject();


            return null;
        }
    }

        /**
     * Custom {@link TypeAdapter} to convert {@link PuzzleChapter PuzzleChapters} to JSON.
     * Currently does not support to convert JSON to puzzle chapters.
     */
    private static class PuzzleChapterTypeAdapter extends TypeAdapter<PuzzleChapter> {
        @Override
        public void write(JsonWriter out, PuzzleChapter chapter) throws IOException {
            if (chapter == null) {
                out.nullValue();
                return;
            }
            out.beginObject()
                    .name("id").value(chapter.getChapterId())
                    .name("position").value(chapter.getPosition())
                    .name("title").value(chapter.getTitle())
                    .name("description").value(chapter.getDescription())
                    .endObject();
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

    public record AdminPuzzleInfo(Puzzle puzzle, int gameCount, boolean active) {}

    public record AdminPuzzlePositions(
            List<Integer> unassignedPuzzles,
            List<Integer> archivedPuzzles,
            List<AdminPuzzlePositionsChapter> chapters) {
        public record AdminPuzzlePositionsChapter(
                int id,
                List<Integer> puzzles) {}
    }

    public record AdminCreateChapterData(String title, String description) {}
}
