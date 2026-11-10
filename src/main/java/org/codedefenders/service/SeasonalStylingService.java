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
package org.codedefenders.service;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.persistence.database.PuzzleRepository;

@ApplicationScoped
@Named("seasonStyle")
public class SeasonalStylingService {
    final PuzzleRepository puzzleRepository;

    @Inject
    public SeasonalStylingService(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    /**
     * Returns an Optional with a single seasonal chapter that is currently active,
     * or an empty Optional
     * if no such chapter exists.
     */
    public Optional<PuzzleChapter> getCurrentSeasonalChapter() {
        for (PuzzleChapter chapter : puzzleRepository.getPuzzleChapters()) {
            if (chapter.getCalendarDate() != null && chapter.isVisible()) {
                return Optional.of(chapter);
            }
        }
        return Optional.empty();
    }
}
