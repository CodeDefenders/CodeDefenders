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
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.servlets.admin.api.AdminPuzzleAPI.GetPuzzlesData.PuzzleData;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
        switch (url) {
            case Paths.API_ADMIN_PUZZLES_ALL:
                handleGetAllPuzzlesRequest(response);
                return;
            case Paths.API_ADMIN_PUZZLE:
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing puzzleId parameter.");
                    return;
                }
                handleGetPuzzleRequest(response, puzzleId.get());
                return;
            case Paths.API_ADMIN_PUZZLECHAPTER:
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing puzzleChapterId parameter.");
                    return;
                }
                handleGetPuzzleChapterRequest(response, puzzleChapterId.get());
                return;
            default:
                writeJSONMessage(response, 404, "Requested URL not available.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getServletPath();
        switch (url) {
            case Paths.API_ADMIN_PUZZLE:
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing puzzleId parameter.");
                    return;
                }
                handleUpdatePuzzle(request, response, puzzleId.get());
                return;
            case Paths.API_ADMIN_PUZZLECHAPTER:
                if (request.getParameter("create") != null) {
                    handleCreatePuzzleChapter(request, response);
                    return;
                }

                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing puzzleChapterId or create parameter.");
                    return;
                }
                handleUpdatePuzzleChapter(request, response, puzzleChapterId.get());
                return;
            case Paths.API_ADMIN_PUZZLES_ALL:
                handleBatchUpdatePuzzlePositions(request, response);
                return;
            default:
                writeJSONMessage(response, 404, "Requested URL not available.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getServletPath();
        switch (url) {
            case Paths.API_ADMIN_PUZZLE:
                final Optional<Integer> puzzleId = ServletUtils.getIntParameter(request, "id");
                if (puzzleId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing parameter.");
                    return;
                }
                handleDeletePuzzleRequest(response, puzzleId.get());
                return;
            case Paths.API_ADMIN_PUZZLECHAPTER:
                final Optional<Integer> puzzleChapterId = ServletUtils.getIntParameter(request, "id");
                if (puzzleChapterId.isEmpty()) {
                    writeJSONMessage(response, 400, "Missing puzzleChapterId parameter.");
                    return;
                }
                handleDeletePuzzleChapterRequest(response, puzzleChapterId.get());
                return;
            default:
                writeJSONMessage(response, 404, "Requested URL not available.");
        }
    }

    private void handleDeletePuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            writeJSONMessage(response, 404, "Puzzle not found.");
            return;
        }

        if (puzzleRepo.gamesExistsForPuzzle(puzzle)) {
            writeJSONMessage(response, 400, "Cannot delete puzzle " + puzzleId + " because games exist for that puzzle.");
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

        if (classRemoved) {
            writeJSONMessage(response, 200, "Removed puzzle " + puzzleId + ".");
        } else {
            writeJSONMessage(response, 500, "Couldn't update puzzle" + puzzleId);
        }

    }

    private void handleDeletePuzzleChapterRequest(HttpServletResponse response,
                                                  int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
        if (puzzleChapter == null) {
            writeJSONMessage(response, 404, "Puzzle chapter not found.");
            return;
        }
        if (!puzzleRepo.removePuzzleChapter(puzzleChapter)) {
            writeJSONMessage(response, 500, "Failed to remove puzzle chapter.");
            return;
        }
        writeJSONMessage(response, 200, "Removed puzzle chapter.");
    }

    private void handleGetAllPuzzlesRequest(HttpServletResponse response) throws IOException {
        final List<PuzzleChapter> puzzleChapters = puzzleRepo.getPuzzleChapters();
        final List<PuzzleData> puzzles = puzzleRepo.getAdminPuzzleInfos();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleData.class, new PuzzleDataTypeAdapter())
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .serializeNulls()
                .create();
        JsonElement json = gson.toJsonTree(new GetPuzzlesData(puzzles, puzzleChapters));
        writeJSONResponse(response, 200, json);
    }

    private void handleGetPuzzleRequest(HttpServletResponse response, int puzzleId) throws IOException {
        final Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            writeJSONMessage(response, 404, "Puzzle not found.");
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                .create();
        JsonElement json = gson.toJsonTree(puzzle);
        writeJSONResponse(response, 200, json);
    }

    private void handleGetPuzzleChapterRequest(HttpServletResponse response, int puzzleChapterId) throws IOException {
        final PuzzleChapter puzzleChapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
        if (puzzleChapter == null) {
            writeJSONMessage(response, 404, "Puzzle chapter not found.");
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();
        JsonElement json = gson.toJsonTree(puzzleChapter);
        writeJSONResponse(response, 200, json);
    }

    private void handleUpdatePuzzle(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int puzzleId) throws IOException {
        Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            writeJSONMessage(response, 404, "Puzzle not found.");
            return;
        }
        Optional<JsonObject> body = readJSONBody(request);

        if (body.isEmpty()) {
            writeJSONMessage(response, 400, "No valid request body provided");
            return;
        }

        Gson gson = new GsonBuilder().create();
        UpdatePuzzleData puzzleData = gson.fromJson(body.get(), UpdatePuzzleData.class);

        // TODO: This should be done in a service.
        String title = puzzleData.title.strip();
        if (title.isEmpty() || title.length() > 100) {
            writeJSONMessage(response, 400, "Invalid title.");
            return;
        }
        String description = puzzleData.description.strip();
        if (description.length() > 1000) {
            writeJSONMessage(response, 400, "Invalid description.");
            return;
        }
        int maxAssertionsPerTest = puzzleData.maxAssertionsPerTest;
        if (maxAssertionsPerTest < 0) {
            writeJSONMessage(response, 400, "Invalid maxAssertionsPerTest.");
            return;
        }
        Integer editableLinesStart = puzzleData.editableLinesStart;
        if (editableLinesStart != null && editableLinesStart < 0) {
            writeJSONMessage(response, 400, "Invalid editableLinesStart.");
            return;
        }
        Integer editableLinesEnd = puzzleData.editableLinesEnd;
        if (editableLinesEnd != null && editableLinesEnd < 0) {
            writeJSONMessage(response, 400, "Invalid editableLinesEnd.");
            return;
        }
        if (editableLinesStart == null ^ editableLinesEnd == null) {
            writeJSONMessage(response, 400, "Invalid editable lines.");
            return;
        }

        boolean success = puzzleRepo.updatePuzzle(new org.codedefenders.model.PuzzleInfo(
                puzzle.getPuzzleId(),
                puzzle.getChapterId(),
                puzzle.getPosition(),
                title,
                description,
                maxAssertionsPerTest,
                editableLinesStart,
                editableLinesEnd
        ));
        if (!success) {
            writeJSONMessage(response, 500, "Failed to update puzzle.");
            return;
        }

        puzzle.setTitle(title);
        puzzle.setDescription(description);
        puzzle.setMaxAssertionsPerTest(maxAssertionsPerTest);
        puzzle.setEditableLinesStart(editableLinesStart);
        puzzle.setEditableLinesEnd(editableLinesEnd);
        JsonObject answer = new JsonObject();
        gson = new GsonBuilder()
                .registerTypeAdapter(Puzzle.class, new PuzzleTypeAdapter())
                .create();
        answer.add("message", new JsonPrimitive("Updated puzzle."));
        answer.add("puzzle", gson.toJsonTree(puzzle));
        writeJSONResponse(response, 200, answer);
    }

    private void handleCreatePuzzleChapter(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<JsonObject> body = readJSONBody(request);
        if (body.isEmpty()) {
            writeJSONMessage(response, 200, "No valid request body provided");
            return;
        }

        Gson gson = new GsonBuilder().create();
        var data = gson.fromJson(body.get(), CreateChapterData.class);

        // TODO: This should be done in a service.
        int maxPosition = puzzleRepo.getPuzzleChapters().stream()
                    .mapToInt(PuzzleChapter::getPosition)
                    .max().orElse(0);
        int position = maxPosition + 1;
        String title = data.title.strip();
        if (title.isEmpty() || title.length() > 100) {
            writeJSONMessage(response, 400, "Invalid title.");
            return;
        }
        String description = data.description.strip();
        if (description.length() > 1000) {
            writeJSONMessage(response, 400, "Invalid description.");
            return;
        }

        PuzzleChapter chapter = new PuzzleChapter(-1, position, title, description);
        int id = puzzleRepo.storePuzzleChapter(chapter);
        chapter.setChapterId(id);


        gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();
        JsonObject answer = new JsonObject();
        answer.add("message", new JsonPrimitive("Created puzzle chapter."));
        answer.add("chapter", gson.toJsonTree(chapter));
        writeJSONResponse(response, 200, answer);
    }

    private void handleUpdatePuzzleChapter(HttpServletRequest request,
                                           HttpServletResponse response,
                                           int puzzleChapterId) throws IOException {
        final PuzzleChapter chapter = puzzleRepo.getPuzzleChapterForId(puzzleChapterId);
        if (chapter == null) {
            writeJSONMessage(response, 404, "Puzzle chapter not found.");
            return;
        }

        Optional<JsonObject> body = readJSONBody(request);
        if (body.isEmpty()) {
            writeJSONMessage(response, 400, "No valid request body provided");
            return;
        }

        Gson gson = new GsonBuilder().create();
        UpdateChapterData data = gson.fromJson(body.get(), UpdateChapterData.class);

        // TODO: This should be done in a service.
        String title = data.title.strip();
        if (title.isEmpty() || title.length() > 100) {
            writeJSONMessage(response, 400, "Invalid title.");
            return;
        }
        String description = data.description.strip();
        if (description.length() > 1000) {
            writeJSONMessage(response, 400, "Invalid description.");
            return;
        }

        boolean success = puzzleRepo.updatePuzzleChapter(chapter);
        if (!success) {
            writeJSONMessage(response, 500, "Failed to update puzzle chapter.");
            return;
        }

        chapter.setTitle(title);
        chapter.setDescription(description);
        gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();
        JsonObject answer = new JsonObject();
        answer.add("message", new JsonPrimitive("Updated puzzle chapter."));
        answer.add("chapter", gson.toJsonTree(chapter));
        writeJSONResponse(response, 200, answer);
    }

    private void handleBatchUpdatePuzzlePositions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<JsonObject> body = readJSONBody(request);
        if (body.isEmpty()) {
            writeJSONMessage(response, 400, "No valid request body provided");
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PuzzleChapter.class, new PuzzleChapterTypeAdapter())
                .create();

        UpdatePuzzlePositionsData positions = gson.fromJson(body.get(), UpdatePuzzlePositionsData.class);
        puzzleRepo.batchUpdatePuzzlePositions(positions);

        writeJSONMessage(response, 200, "Updated puzzle and chapter positions.");
    }

    /**
     * Writes a given JSON string to a given {@link HttpServletResponse}.
     * Also sets the content type to {@code application/json}.
     *
     * @param response the response the JSON is written to, never {@code null}.
     * @param json     the JSON as a {@link String}.
     * @throws IOException when writing the JSON fails.
     */
    private static void writeJSONResponse(@Nonnull HttpServletResponse response, int statusCode, JsonElement json)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
        writer.flush();
    }

    private static void writeJSONMessage(@Nonnull HttpServletResponse response, int statusCode, String message)
            throws IOException {
        JsonObject json = new JsonObject();
        json.add("message", new JsonPrimitive(message));
        writeJSONResponse(response, statusCode, json);
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
     * Custom {@link TypeAdapter} to convert {@link org.codedefenders.model.PuzzleInfo Puzzle information} to JSON.
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

    private static class PuzzleDataTypeAdapter extends TypeAdapter<GetPuzzlesData.PuzzleData> {
        @Override
        public void write(JsonWriter out, PuzzleData info) throws IOException {
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
        public GetPuzzlesData.PuzzleData read(JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    public record GetPuzzlesData(
            List<PuzzleData> puzzles,
            List<PuzzleChapter> chapters) {
        public record PuzzleData(Puzzle puzzle, int gameCount, boolean active) {}
    }
    public record UpdatePuzzlePositionsData(
            List<Integer> unassignedPuzzles,
            List<Integer> archivedPuzzles,
            List<ChapterData> chapters) {
        public record ChapterData(int id, List<Integer> puzzles) {}
    }
    public record CreateChapterData(String title, String description) {}
    public record UpdateChapterData(String title, String description) {}
    public record UpdatePuzzleData(
            String title,
            String description,
            int maxAssertionsPerTest,
            Integer editableLinesStart,
            Integer editableLinesEnd) {}
}
