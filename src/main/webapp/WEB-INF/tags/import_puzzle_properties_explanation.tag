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
