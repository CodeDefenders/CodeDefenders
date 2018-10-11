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
                <a class="btn btn-xs" href="<%=request.getContextPath() %>/jsp/singleplayer_attacker_view.jsp">1</a>
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

<%@include file="footer.jsp" %>