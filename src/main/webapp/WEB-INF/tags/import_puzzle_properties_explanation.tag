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
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%@ tag pageEncoding="UTF-8" %>

<p>
    ${i18n.tr('The <code>puzzle.properties</code> file contains the configuration of the puzzle.')}
    ${i18n.tr('The text area below shows the supported properties for puzzles:')}
</p>

<pre class="mb-0 p-3 bg-light" style="line-height: 1.15;">${i18n.tr('# Type of the puzzle. Can be ATTACKER or DEFENDER.')}
type=ATTACKER

${i18n.tr('# Title of the puzzle.')}
title=Puzzle 1

${i18n.tr('# Description of the puzzle.')}
description=Write a mutant which evades all the tests.

${i18n.tr('# The level at which the puzzle games will be played at.')}
${i18n.tr('# EASY allows players to see all tests and mutants in the game.')}
${i18n.tr('# HARD hides tests from attacker players and hides mutants from defender players.')}
gameLevel=EASY

${i18n.tr('# (Optional) The fist line of the CUT that attackers are allowed to edit (inclusive).')}
${i18n.tr('# This is ignored for DEFENDER puzzles.')}
editableLinesStart=4

${i18n.tr('# (Optional) The fist last of the CUT, that attackers are allowed to edit (inclusive).')}
${i18n.tr('# This is ignored for DEFENDER puzzles.')}
editableLinesEnd=4</pre>
