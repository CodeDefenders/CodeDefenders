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
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<% String pageTitle="Duel Creation"; %>
<%@ include file="/jsp/header_main.jsp" %>


<div id="creategame" class="container">
	<form id="create" action="<%=request.getContextPath() %>/games" method="post" class="form-creategame">
		<h2>Create Game</h2>
		<input type="hidden" name="formType" value="createGame">
		<table class="tableform">
			<tr>
				<td>Java Class</td>
				<td id="classTd">
					<select id="class" name="class" class="form-control selectpicker" data-size="large" >
						<% for (GameClass c : GameClassDAO.getAllClasses()) { %>
						<option value="<%=c.getId()%>"><%=c.getAlias()%></option>
						<%}%>
					</select>
				</td>
				<td>
					<a href="games/upload" class="text-center new-account">Upload Class</a>
				</td>
			</tr>
			<%-- Disable mode selection for release.
			<tr>
				<td>Mode</td>
				<td>
					<select name="mode" class="form-control selectpicker" data-size="large">
						<option value="duel">Duel</option>
						<option value="sing">Singleplayer</option>
						<option value="utst">Unit Testing</option>
					</select>
				</td>
			</tr>
			--%>
			<tr>
				<td>Role</td> <td id="roleTd"><input type="checkbox" id="role" name="role" class="form-control" data-size="large" data-toggle="toggle" data-on="Attacker" data-off="Defender" data-onstyle="success" data-offstyle="primary"></td>
			</tr>
			<tr>
				<td>Rounds</td> <td id="roundsTd"><input class="form-control" type="number" id="rounds" name="rounds" value="3" min="1" max="10"></td>
			</tr>
			<tr>
            <td>Level</td> <td id="levelTd"><input type="checkbox" id="level" name="level" class="form-control" data-size="large" data-toggle="toggle" data-on="Easy" data-off="Hard" data-onstyle="info" data-offstyle="warning">
			</tr>
		</table>
		<button id="createButton" class="btn btn-lg btn-primary btn-block" type="submit" value="Create">Create</button>
	</form>
</div>
<%@ include file="/jsp/footer.jsp" %>