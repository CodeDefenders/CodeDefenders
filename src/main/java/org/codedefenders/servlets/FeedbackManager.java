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
package org.codedefenders.servlets;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.FeedbackDAO;
import org.codedefenders.model.Feedback;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/feedback")
public class FeedbackManager extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Inject
    private LoginBean login;

    private static final Logger logger = LoggerFactory.getLogger(FeedbackManager.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No valid gameId parameter found");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = gameIdOpt.get();

        switch (request.getParameter("formType")) {
            case "sendFeedback":
                if (!saveFeedback(request, login.getUserId(), gameId)) {
                    messages.add("Could not save your feedback. Please try again later!");
                }
        }

        Redirect.redirectBack(request, response);
    }

    private boolean saveFeedback(HttpServletRequest request, int userId, int gameId) {
        List<Integer> ratingsList = new ArrayList<>();
        for (Feedback.Type f : Feedback.Type.values()) {
            String rating = request.getParameter("rating" + f.name());
            ratingsList.add(rating == null ? 0 : Integer.parseInt(rating));
        }
        return FeedbackDAO.storeFeedback(gameId, userId, ratingsList);
    }
}
