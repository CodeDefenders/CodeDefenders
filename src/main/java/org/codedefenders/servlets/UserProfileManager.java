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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.user.UserProfileBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameType;
import org.codedefenders.model.Achievement;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.AchievementService;
import org.codedefenders.service.UserStatsService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.URLUtils;

/**
 * This {@link HttpServlet} handles requests for viewing the currently logged
 * in {@link UserEntity}. This functionality may be private only, see
 * {@link #isProfilePublic()}.
 *
 * <p>Serves on path: {@code /profile}.
 *
 * @author <a href="https://github.com/timlg07">Tim Greller</a>
 */
@WebServlet(org.codedefenders.util.Paths.USER_PROFILE)
public class UserProfileManager extends HttpServlet {

    @Inject
    private UserRepository userRepo;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private UserStatsService userStatsService;

    @Inject
    private AchievementService achievementService;

    @Inject
    private UserProfileBean userProfileBean;

    @Inject
    private CodeDefendersFormAuthenticationFilter codedefendersFormAuthenticationFilter;

    @Inject
    private URLUtils url;

    /**
     * Checks whether users can view the profile of others.
     *
     * @return {@code true} if the profile page of someone can be visited, {@code false} otherwise.
     */
    public static boolean isProfilePublic() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.PUBLIC_USER_PROFILE).getBoolValue();
    }

    /**
     * Retrieves the user-parameter of a request. The Optional is empty if the parameter was not given or empty.
     * Due to URL-decoding the returned String might be blank.
     *
     * <p>URL-decoding is not strictly needed, as usernames can only contain letters or numbers
     * (see {@link org.codedefenders.validation.input.CodeDefendersValidator#validUsername(String)}), but is used
     * to offer support for special characters in usernames by default.
     *
     * @param request The HttpServletRequest with the desired parameter.
     * @return An Optional containing the name parameters value if given.
     */
    private static Optional<String> userParameter(HttpServletRequest request) {
        return ServletUtils.getStringParameter(request, "user");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final Optional<UserEntity> loggedInUser = login.isLoggedIn()
                ? userRepo.getUserById(login.getUserId()) : Optional.empty();
        final Optional<String> urlParam = userParameter(request);
        final Optional<UserEntity> urlParamUser = urlParam.flatMap(userRepo::getUserByName);

        final boolean explicitUserGiven = urlParamUser.isPresent();
        final boolean isLoggedIn = loggedInUser.isPresent();
        final boolean isSelf = (!explicitUserGiven // no URL-parameter given -> logged in user is used
                || isLoggedIn && loggedInUser.get().equals(urlParamUser.get())); // explicit user is self

        if (!isSelf && !isProfilePublic()) {
            // Someone tries to access a profile of someone else, but profiles are not public. Send user to homepage.
            response.sendRedirect(url.forPath("/"));
            return;
        }

        if (urlParam.isPresent() && !explicitUserGiven) {
            // Invalid URL parameter or user not found.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.getRequestDispatcher(Constants.USER_NOT_FOUND_JSP).forward(request, response);
            return;
        }

        if (!explicitUserGiven && !isLoggedIn) {
            // Enforce user to be logged in to view own profile without URL-parameter.
            codedefendersFormAuthenticationFilter.requireLogin(request, response);
            return;
        }

        /*
         * If logged in, the own profile page shows private data (isSelf == true).
         * The cache should be disabled in this case. This is now done by the CacheControlFilter.
         */

        // load stats
        final UserEntity user = urlParamUser.orElseGet(() -> userRepo.getUserById(login.getUserId()).get());
        final Map<GameType, UserStats> stats = userStatsService.getStatsByUserId(user.getId());
        final UserStats.PuzzleStats puzzleStats = userStatsService.getPuzzleStatsByUserId(user.getId());
        final Collection<Achievement> achievements = achievementService.getAchievementsForUser(user.getId());

        // Pass values to JSP page
        userProfileBean.setUser(user);
        userProfileBean.setSelf(isSelf);
        userProfileBean.setStats(stats);
        userProfileBean.setPuzzleStats(puzzleStats);
        userProfileBean.setAchievements(achievements);
        request.setAttribute("profile", userProfileBean);

        request.getRequestDispatcher(Constants.USER_PROFILE_JSP).forward(request, response);
    }
}
