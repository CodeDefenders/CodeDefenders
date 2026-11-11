/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.codedefenders.arcade.mutle.model.MutlePublication;
import org.codedefenders.arcade.mutle.service.MutleCatalog;
import org.codedefenders.arcade.mutle.service.MutleFixtureView;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(Paths.ADMIN_MUTLE)
@MultipartConfig(maxFileSize = 8 * 1024 * 1024, maxRequestSize = 9 * 1024 * 1024)
public class AdminMutle extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminMutle.class);
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private MutleCatalog catalog;

    @Inject
    private MessagesBean messages;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<MutlePublication> publications = catalog.publications();
        Instant now = Instant.now();
        request.setAttribute("mutleFixtures", catalog.inventory());
        request.setAttribute("mutleFixtureEntries", catalog.allEntries().stream()
                .map(entry -> new MutleFixtureView(entry.classKey() + "/" + entry.mutantKey(), entry.series(),
                        catalog.fixtureClass(entry.classKey()).title()))
                .toList());
        request.setAttribute("mutlePublications", publications.stream()
                .map(publication -> new PublicationRow(publication, now))
                .toList());
        request.setAttribute("mutleNeedsFutureCase", !publications.isEmpty()
                && !publications.get(publications.size() - 1).releaseAt().isAfter(now));
        request.setAttribute("mutleHasFutureCases", !publications.isEmpty()
                && publications.get(publications.size() - 1).releaseAt().isAfter(now));
        request.setAttribute("mutleNextSeries", publications.size() + 1L);
        if (!publications.isEmpty()) {
            Instant last = publications.get(publications.size() - 1).releaseAt();
            Instant nextWeek = last.plus(Duration.ofDays(7));
            Instant earliest = now.plus(Duration.ofMinutes(2));
            request.setAttribute("mutleNextReleaseAt", inputDate(nextWeek.isAfter(earliest) ? nextWeek : earliest));
        }
        request.getRequestDispatcher("/jsp/admin_mutle.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String action = request.getParameter("formType");
            if (action == null) {
                throw new IllegalArgumentException("Missing Mutle action");
            }
            switch (action) {
                case "uploadFixture" -> upload(request);
                case "scheduleInitial" -> catalog.scheduleInitial(parseDate(request.getParameter("firstReleaseAt")));
                case "savePublication" -> catalog.savePublication(
                        Long.parseLong(request.getParameter("series")),
                        request.getParameter("fixtureKey"),
                        parseDate(request.getParameter("releaseAt")));
                case "removeLastPublication" -> catalog.removeLastFuturePublication(
                        Long.parseLong(request.getParameter("series")));
                case "clearFuturePublications" -> catalog.clearFuturePublications();
                default -> throw new IllegalArgumentException("Unknown Mutle action");
            }
            messages.add("Mutle changes saved.");
        } catch (IllegalArgumentException | IllegalStateException | IOException | ServletException exception) {
            logger.warn("Could not update Mutle catalog", exception);
            messages.add("Could not update Mutle: " + exception.getMessage());
        }
        response.sendRedirect(request.getContextPath() + Paths.ADMIN_MUTLE);
    }

    private void upload(HttpServletRequest request) throws IOException, ServletException {
        Part part = request.getPart("fixtureZip");
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("Select a fixture ZIP file");
        }
        try (var input = part.getInputStream()) {
            catalog.importFixture(input.readAllBytes());
        }
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enter a release date and time in UTC");
        }
        try {
            return LocalDateTime.parse(value, INPUT_DATE).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid release date and time in UTC", exception);
        }
    }

    private static String inputDate(Instant instant) {
        return INPUT_DATE.withZone(ZoneOffset.UTC).format(instant);
    }

    public record PublicationRow(long series, String fixtureKey, String releaseAtInput,
                                  String releaseAtLabel, boolean locked) {
        private PublicationRow(MutlePublication publication, Instant now) {
            this(publication.series(), publication.fixtureKey(), inputDate(publication.releaseAt()),
                    DISPLAY_DATE.withZone(ZoneOffset.UTC).format(publication.releaseAt()),
                    !publication.releaseAt().isAfter(now));
        }

        public long getSeries() {
            return series;
        }

        public String getFixtureKey() {
            return fixtureKey;
        }

        public String getReleaseAtInput() {
            return releaseAtInput;
        }

        public String getReleaseAtLabel() {
            return releaseAtLabel;
        }

        public boolean isLocked() {
            return locked;
        }
    }
}
