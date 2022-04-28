<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- Attributes set in the servlet --%>

<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>
<%--@elvariable id="login" type="org.codedefenders.beans.user.LoginBean"--%>
<%--@elvariable id="leaderboardService" type="org.codedefenders.service.LeaderboardService"--%>

<c:choose>
    <c:when test="${login.loggedIn}">
        <jsp:include page="/jsp/header.jsp"/>
    </c:when>
    <c:otherwise>
        <jsp:include page="/jsp/header_logout.jsp"/>
    </c:otherwise>
</c:choose>

<div class="container">
    <h2 class="mb-4">${pageInfo.pageTitle}</h2>

    <table id="tableMPLeaderboard" class="table table-striped">
        <thead>
            <tr>
                <th>User</th>
                <th>Mutants</th>
                <th>Attacker Score</th>
                <th>Tests</th>
                <th>Defender Score</th>
                <th>Mutants Killed</th>
                <th>Total Score</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="entry" items="${leaderboardService.all}">
                <tr>
                    <td>${entry.username}</td>
                    <td>${entry.mutantsSubmitted}</td>
                    <td>${entry.attackerScore}</td>
                    <td>${entry.testsSubmitted}</td>
                    <td>${entry.defenderScore}</td>
                    <td>${entry.mutantsKilled}</td>
                    <td>${entry.totalPoints}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <script type="module">
        import DataTable from './js/datatables.mjs';

        new DataTable('#tableMPLeaderboard', {
            "order": [[6, "desc"]],
            "columnDefs": [
                {"searchable": false, "targets": [1, 2, 3, 4, 5, 6]}
            ],
            "pageLength": 50
        });
    </script>
</div>

<%@ include file="/jsp/footer.jsp" %>
