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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.ClassViewerBean;
import org.codedefenders.beans.game.MutantAccordionBean;
import org.codedefenders.beans.game.TestAccordionBean;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Paths;

import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.util.Constants.ADMIN_PUZZLE_MANAGEMENT_JSP;
import static org.codedefenders.util.Constants.ADMIN_PUZZLE_PREVIEW_JSP;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This {@link HttpServlet} handles admin management of puzzles.
 *
 * <p>{@code GET} requests redirect to the admin puzzle management page.
 * and {@code POST} requests handle puzzle related management.
 *
 * <p>Serves under {@code /admin/puzzles} and {@code /admin/puzzles/management}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet({Paths.ADMIN_PUZZLE_OVERVIEW, Paths.ADMIN_PUZZLE_MANAGEMENT})
public class AdminPuzzleManagement extends HttpServlet {

    @Inject
    private GameClassRepository gameClassRepo;

    @Inject
    private PuzzleRepository puzzleRepo;

    @Inject
    private TestRepository testRepo;

    @Inject
    private TestSmellRepository testSmellRepo;

    @Inject
    private UserService userService;

    @Inject
    private TestAccordionBean testAccordion;

    @Inject
    private MutantAccordionBean mutantAccordion;

    @Inject
    private ClassViewerBean classViewer;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        var puzzleId = getIntParameter(request, "previewPuzzleId");
        if (puzzleId.isPresent()) {
            displayPuzzlePreview(request, response, puzzleId.get());
        } else {
            request.getRequestDispatcher(ADMIN_PUZZLE_MANAGEMENT_JSP).forward(request, response);
        }
    }

    /**
     * Displays a full-page puzzle preview.
     */
    private void displayPuzzlePreview(HttpServletRequest request, HttpServletResponse response, int puzzleId)
            throws ServletException, IOException {

        Puzzle puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        if (puzzle == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            response.getWriter().println("Puzzle not found.");
            return;
        }

        var attacker = userService.getSimpleUserById(DUMMY_ATTACKER_USER_ID).orElseThrow();
        var defender = userService.getSimpleUserById(DUMMY_DEFENDER_USER_ID).orElseThrow();

        GameClass cut = gameClassRepo.getClassForId(puzzle.getClassId()).orElseThrow();
        List<Test> tests = gameClassRepo.getMappedTestsForClassId(cut.getId());
        List<Mutant> mutants = gameClassRepo.getMappedMutantsForClassId(cut.getId());

        List<TestDTO> testDTOs = tests.stream()
                .map(test -> convertTest(test, defender, mutants))
                .toList();

        List<MutantDTO> mutantDTOs = mutants.stream()
                .map(mutant -> convertMutant(mutant, attacker, tests))
                .toList();

        classViewer.init(cut);
        testAccordion.init(cut, testDTOs);
        mutantAccordion.init(cut, mutantDTOs, -1);

        request.setAttribute("puzzle", puzzle);
        request.setAttribute("mutants", mutants);
        request.setAttribute("tests", tests);
        request.getRequestDispatcher(ADMIN_PUZZLE_PREVIEW_JSP).forward(request, response);
    }

    private TestDTO convertTest(Test test, SimpleUser creator, List<Mutant> mutants) {
        List<Integer> killedMutantIds = testRepo.getKilledMutantsForTestId(test.getId()).stream()
                .map(Mutant::getId)
                .toList();
        return new TestDTO(
                test.getId(),
                creator,
                test.getScore(),
                true,
                test.getCoveredMutants(mutants).stream().map(Mutant::getId).toList(),
                killedMutantIds,
                testSmellRepo.getDetectedTestSmellsForTest(test.getId()),
                -1,
                test.getPlayerId(),
                test.getLineCoverage().getLinesCovered(),
                test.getAsString());
    }

    private MutantDTO convertMutant(Mutant mutant, SimpleUser creator, List<Test> tests) {
        Test killingTest = testRepo.getKillingTestForMutantId(mutant.getId());
        SimpleUser killedBy = null;
        int killedByTestId = -1;
        String killMessage = null;
        if (killingTest != null) {
            killedBy = userService.getSimpleUserByPlayerId(killingTest.getPlayerId()).orElse(null);
            killedByTestId = killingTest.getId();
            killMessage = testRepo.findKillMessageForMutant(mutant.getId()).orElse(null);
        }
        if (killingTest == null) {
            // Mapped mutants are set to dead by default.
            mutant.setAlive(true);
        }
        return new MutantDTO(
                mutant.getId(),
                creator,
                killingTest == null ? Mutant.State.ALIVE : Mutant.State.KILLED,
                mutant.getScore(),
                mutant.getHTMLReadout().stream()
                        .filter(Objects::nonNull).collect(Collectors.joining("<br>")),
                mutant.getLines().stream().map(String::valueOf).collect(Collectors.joining(",")),
                tests.stream().anyMatch(test -> test.isMutantCovered(mutant)),
                true,
                false,
                killedBy,
                killedByTestId,
                killMessage,
                mutant.getGameId(),
                mutant.getPlayerId(),
                mutant.getLines(),
                mutant.getPatchString());
    }
}
