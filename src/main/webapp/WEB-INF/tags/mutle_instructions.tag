<%--

    Copyright (C) 2016-2025 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<div class="mutle-instructions">
    <p>${i18n.tr('Like Wordle or Mastermind, Mutle gives you feedback after each guess. Here, your guesses are Java tests, and the hidden answer is a small change to the code: a mutant.')}</p>
    <ol class="mb-0">
        <li>${i18n.tr('Read the original class and its specification. The mutant is hidden until the game ends.')}</li>
        <li>${i18n.tr('Use the test editor to write a JUnit test that passes on the original class but fails on the mutant. This kills the mutant and solves the puzzle.')}</li>
        <li>${i18n.tr('If the mutant survives, use the feedback to narrow down the change: no relevant coverage, the mutated method covered, or the changed line covered. Select an attempt to see its test and coverage, then refine your next test. You have six attempts.')}</li>
    </ol>
</div>
