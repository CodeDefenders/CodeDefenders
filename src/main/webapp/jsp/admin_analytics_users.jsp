<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Users</h3>

    <table id="tableUsers"
           class="table table-striped table-hover table-responsive">
        <thead>
            <tr>
                <th id="toggle-all-details"><span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span></th>
                <th>ID</th>
                <th>Username</th>
                <th>Games Played</th>
                <th>Attacker Score</th>
                <th>Defender Score</th>
                <th>Total Score</th>
            </tr>
        </thead>
    </table>

    <div class="btn-group">
        <a download="user-analytics.csv" href="<%=request.getContextPath()+Constants.API_ANALYTICS_USERS%>?type=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="user-analytics.json" href="<%=request.getContextPath()+Constants.API_ANALYTICS_USERS%>?type=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>

    <script src="js/datatables-utils.js" type="text/javascript" ></script>

    <script>
        var table;

        function format(data) {
            return '' +
                '<table class="table-child-details indented">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Games as Attacker:</td>'+
                            '<td>'+data.attackerGamesPlayed+'</td>'+
                            '<td>Games as Defender:</td>'+
                            '<td>'+data.defenderGamesPlayed+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>'+
                '<table class="table-child-details indented">'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Mutants</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutants Submitted:</td>'+
                            '<td>'+data.mutantsSubmitted+'</td>'+
                            '<td>Per Game (as Attacker):</td>'+
                            '<td>'+(data.attackerGamesPlayed === 0 ? 0 : (data.mutantsSubmitted/data.attackerGamesPlayed).toFixed(2))+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Alive Mutants:</td>'+
                            '<td>'+data.mutantsAlive+'</td>'+
                            '<td>Per Game (as Attacker):</td>'+
                            '<td>'+(data.attackerGamesPlayed === 0 ? 0 : (data.mutantsAlive/data.attackerGamesPlayed).toFixed(2))+'</td>'+
                            '<td>Per Mutant:</td>'+
                            '<td>'+(data.mutantsSubmitted === 0 ? 0 : (data.mutantsAlive/data.mutantsSubmitted).toFixed(2))+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Equivalent Mutants:</td>'+
                            '<td>'+data.mutantsSubmitted+'</td>'+
                            '<td>Per Game (as Attacker):</td>'+
                            '<td>'+(data.attackerGamesPlayed === 0 ? 0 : (data.mutantsEquivalent/data.attackerGamesPlayed).toFixed(2))+'</td>'+
                            '<td>Per Mutant</td>'+
                            '<td>'+(data.mutantsSubmitted === 0 ? 0 : (data.mutantsEquivalent/data.mutantsSubmitted).toFixed(2))+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>'+
                '<table class="table-child-details indented">'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Tests</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr/>'+
                        '<tr>'+
                            '<td>Tests Submitted:</td>'+
                            '<td>'+data.testsSubmitted+'</td>'+
                            '<td>Per Game (as Defender):</td>'+
                            '<td>'+(data.defenderGamesPlayed === 0 ? 0 : (data.testsSubmitted/data.defenderGamesPlayed).toFixed(2))+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Mutants Killed:</td>'+
                            '<td>'+data.mutantsKilled+'</td>'+
                            '<td>Per Game (as Defender):</td>'+
                            '<td>'+(data.defenderGamesPlayed === 0 ? 0 : (data.mutantsKilled/data.defenderGamesPlayed).toFixed(2))+'</td>'+
                            '<td>Per Test:</td>'+
                            '<td>'+(data.testsSubmitted === 0 ? 0 : (data.mutantsKilled/data.testsSubmitted).toFixed(2))+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>';
        }

        $(document).ready(function() {
            table = $('#tableUsers').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Constants.API_ANALYTICS_USERS%>",
                    "dataSrc": "data"
                },
                "columns": [
                    {
                        "className":      'toggle-details',
                        "orderable":      false,
                        "data":           null,
                        "defaultContent": '<span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span>'
                    },
                    { "data": "id" },
                    { "data": "username" },
                    { "data": "gamesPlayed" },
                    { "data": "attackerScore" },
                    { "data": "defenderScore" },
                    { "data":
                        function(row, type, val, meta) {
                            return row.attackerScore + row.defenderScore;
                        }
                    }
                ],
                "pageLength": 50,
                "order": [[ 1, "asc" ]]
            });

            setupChildRows("#tableUsers", table, format);
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
