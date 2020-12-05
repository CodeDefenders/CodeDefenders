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
<%--@elvariable id="leaderbordEntries" type="java.util.List<org.codedefenders.game.leaderboard.Entry>"--%>


<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Leaderboard"); %>


<jsp:include page="/jsp/header_main.jsp"/>

<div class="w-100">
    <h3>Battlegrounds</h3>
    <table id="tableMPLeaderboard"
           class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
        <thead>
        <tr>
            <th class="col-sm-2">User</th>
            <th class="col-sm-1">Mutants</th>
            <th class="col-sm-2">Attacker Score</th>
            <th class="col-sm-1">Tests</th>
            <th class="col-sm-2">Defender Score</th>
            <th class="col-sm-2">Mutants Killed</th>
            <th class="col-sm-2">Total Score</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="entry" items="${leaderbordEntries}">
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

    <script>
        (function () {

            $(document).ready(function () {
                $.fn.dataTable.moment('DD/MM/YY HH:mm');
                $('#tableMPLeaderboard').DataTable({
                    "order": [[6, "desc"]],
                    "columnDefs": [
                        {"searchable": false, "targets": [1, 2, 3, 4, 5, 6]}
                    ],
                    "pageLength": 50
                });
            });

        })();
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
