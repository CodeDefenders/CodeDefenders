/*
 * Copyright (C) 2021 Code Defenders contributors
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
package org.codedefenders.service;

import java.util.List;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.persistence.database.LeaderboardRepository;
import org.codedefenders.persistence.entity.LeaderboardEntryEntity;

@SuppressWarnings("unused") // Used in leaderboards.jsp
@Named
@ApplicationScoped
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepo;

    @Inject
    public LeaderboardService(LeaderboardRepository leaderboardRepo) {
        this.leaderboardRepo = leaderboardRepo;
    }

    @Nonnull
    public List<LeaderboardEntryEntity> getAll() {
        return leaderboardRepo.getLeaderboard();
    }

}
