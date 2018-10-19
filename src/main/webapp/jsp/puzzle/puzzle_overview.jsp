<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<% String pageTitle = null; %>

<%@ include file="/jsp/header_main.jsp" %>

<div class="w-100">
    <h2 class="full-width page-title">Puzzles</h2>
    <table id="puzzles" class="table table-striped table-hover table-responsive table-paragraphs games-table">
        <tr>
            <th>Lecture</th>
            <th>Attacking Levels</th>
            <th>Defending Levels</th>
        </tr>
        <tr>
            <td>Statement Coverage</td>
            <td>
                <a class="btn btn-xs" href="${pageContext.request.contextPath}/puzzlegame?puzzleId=1">1</a>
                <a class="btn btn-xs" href="${pageContext.request.contextPath}/puzzlegame?puzzleId=2">2</a>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
        </tr>
        <tr>
            <td>Branch Coverage</td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
        </tr>
        <tr>
            <td>Testing Loops</td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
        </tr>
        <tr>
            <td>Boundary Value Testing</td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
            <td>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
                <p class="glyphicon glyphicon-lock"></p>
            </td>
        </tr>
    </table>
</div>

<%@include file="../footer.jsp" %>