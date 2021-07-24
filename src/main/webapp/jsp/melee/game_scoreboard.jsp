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
<%@page import="org.codedefenders.beans.game.ScoreItem"%>

<jsp:useBean id="meleeScoreboardBean" class="org.codedefenders.beans.game.MeleeScoreboardBean" scope="request" />


<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request" />

<div id="scoreboard" class="modal fade" role="dialog">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title">Scoreboard</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
			</div>
			<div class="modal-body">
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
                        <!--  Use the tag library to go over the players instead of using java snippets -->
                        <%
                            for (ScoreItem scoreItem : meleeScoreboardBean.getSortedScoreItems()) {
                                // Highlight the row of the current player. Apparently, embedding the rendering in the class tag breaks it ?
                                if (login.getUserId() == scoreItem.getUser().getId()) {
                        %>
                            <tr class="bg-warning bg-gradient">
                        <%
                                } else {
                        %>
                            <tr>
                        <%
                                }
                        %>
                                <td><%=scoreItem.getUser().getUsername()%></td>
                                <td><%=scoreItem.getAttackScore().getTotalScore()%></td>
                                <td><%=scoreItem.getDefenseScore().getTotalScore()%></td>
                                <td><%=scoreItem.getDuelScore().getTotalScore()%></td>
                                <td><%=scoreItem.getAttackScore().getTotalScore() + scoreItem.getDefenseScore().getTotalScore() + scoreItem.getDuelScore().getTotalScore()%></td>
                            </tr>
                        <%
                            }
                        %>
                    </tbody>
				</table>
			</div>
			<div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>
