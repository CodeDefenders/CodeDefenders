<%--

    Copyright (C) 2016-2023 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="meleeScoreboard" type="org.codedefenders.beans.game.MeleeScoreboardBean"--%>

<table class="table">
    <thead>
    <tr>
        <th>User</th>
        <th>Attack</th>
        <th>Defense</th>
        <th>Duels</th>
        <th>Total Points</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${meleeScoreboard.scoreItems}" var="scoreItem">
        <tr class="${login.userId eq scoreItem.user.id ? "bg-warning bg-gradient" : ""}">
            <td>${scoreItem.user.name}</td>
            <td>${scoreItem.attackScore.totalScore}</td>
            <td>${scoreItem.defenseScore.totalScore}</td>
            <td>${scoreItem.duelScore.totalScore}</td>
            <td>${scoreItem.attackScore.totalScore + scoreItem.defenseScore.totalScore + scoreItem.duelScore.totalScore}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>
