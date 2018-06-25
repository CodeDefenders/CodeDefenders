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

    <script src="js/datatables-child-rows.js" type="text/javascript" ></script>

    <script>
        function format(data) {
            return '' +
                '<table class="table-child-details">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutants Submitted:</td>'+
                            '<td>'+data.mutantsSubmitted+'</td>'+
                            '<td>Equivalent:</td>'+
                            '<td>'+data.equivalentMutantsSubmitted+'</td>'+
                        '</tr>'+
                        '<tr/>'+
                        '<tr>'+
                            '<td>Tests Submitted:</td>'+
                            '<td>'+data.testsSubmitted+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Mutants Killed:</td>'+
                            '<td>'+data.mutantsKilled+'</td>'+
                            '<td>Per Test:</td>'+
                            '<td>'+(data.testsSubmitted === 0 ? 0 : (data.mutantsKilled/data.testsSubmitted).toFixed(2))+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>';
        }

        $(document).ready(function() {
            var table = $('#tableUsers').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Constants.API_ANALYTICS_USERS%>",
                    "dataSrc": ""
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
                    { "data": "totalScore" }
                ],
                "order": [[ 1, "asc" ]]
            });

            setupChildRows("#tableUsers", table, format);
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
