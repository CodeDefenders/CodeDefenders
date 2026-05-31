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
<%@ taglib prefix="fn" uri="org.codedefenders.functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ attribute name="game" required="true" type="org.codedefenders.game.puzzle.PuzzleGame" %>

<span>
<c:choose>
    <c:when test="${game.state == 'SOLVED'}">
        <i class="fa fa-check"></i> ${i18n.trn('Puzzle solved in {0} attempt.', 'Puzzle solved in {0} attempts.', game.currentRound, game.currentRound)}
    </c:when>
    <c:otherwise>
        ${i18n.tr('This is your {0}{1} attempt.', game.currentRound, fn:ordinalSuffix(game.currentRound))}
    </c:otherwise>
</c:choose>
</span>
