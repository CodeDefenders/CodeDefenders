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
