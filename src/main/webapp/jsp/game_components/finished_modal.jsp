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
<%@ page import="org.codedefenders.util.Constants" %>

<%--
    Shows a modal, which indicates that a game is finished.

    @param Boolean win
        Indicates that the player won.
    @param Boolean loss
        Indicates that the player lost.
--%>

<% { %>

<%
    Boolean win = (Boolean) request.getAttribute("win");
    Boolean loss = (Boolean) request.getAttribute("loss");
%>

<%
    String message;
    if (win) {
        message = Constants.WINNER_MESSAGE;
    } else if (loss) {
        message = Constants.LOSER_MESSAGE;
    } else {
        message = Constants.DRAW_MESSAGE;
    }
%>

<div id="finishedModal" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Game Over</h4>
            </div>
            <div class="modal-body">
                <p><%= message %></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

<script>
    $('#finishedModal').modal('show');
</script>

<% } %>
