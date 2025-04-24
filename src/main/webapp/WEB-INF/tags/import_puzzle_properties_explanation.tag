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

<p>
    The <code>puzzle.properties</code> file contains the configuration of the puzzle.
    The text area below shows the supported properties for puzzles:
</p>

<pre class="mb-0 p-3 bg-light" style="line-height: 1.15;"># Type of the puzzle. Can be ATTACKER or DEFENDER.
type=ATTACKER

# Title of the puzzle.
title=Puzzle 1

# Description of the puzzle.
description=Write a mutant which evades all the tests.

# The level at which the puzzle games will be played at.
# EASY allows players to see all tests and mutants in the game.
# HARD hides tests from attacker players and hides mutants from defender players.
gameLevel=EASY

# (Optional) The fist line of the CUT that attackers are allowed to edit (inclusive).
# This is ignored for DEFENDER puzzles.
editableLinesStart=4

# (Optional) The fist last of the CUT, that attackers are allowed to edit (inclusive).
# This is ignored for DEFENDER puzzles.
editableLinesEnd=4</pre>
