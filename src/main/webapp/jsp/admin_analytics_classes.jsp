<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Classes</h3>

    <table id="tableUsers"
           class="table table-striped table-hover table-responsive">
        <thead>
            <tr>
                <th id="toggle-all-details"><span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span></th>
                <th>ID</th>
                <th>Name</th>
                <th>Games Played</th>
                <th>Test Submitted</th>
                <th>Mutants Submitted</th>
            </tr>
        </thead>
    </table>

    <div class="btn-group">
        <a download="classes-analytics.csv" href="<%=request.getContextPath()+Constants.API_ANALYTICS_USERS%>?type=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="classes-analytics.json" href="<%=request.getContextPath()+Constants.API_ANALYTICS_USERS%>?type=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>

    <script src="js/datatables-child-rows.js" type="text/javascript" ></script>

    <script>
        var table;

        function format(data) {
            return '' +
                '<table class="table-child-details">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutants Alive:</td>'+
                            '<td>'+data.mutantsAlive+'</td>'+
                            '<td>Mutants Equivalent:</td>'+
                            '<td>'+data.mutantsEquivalent+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>';
        }

        $(document).ready(function() {
            table = $('#tableUsers').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Constants.API_ANALYTICS_CLASSES%>",
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
                    { "data": "classname" },
                    { "data": "games" },
                    { "data": "testsSubmitted" },
                    { "data": "mutantsSubmitted" }
                ],
                "pageLength": 50,
                "order": [[ 1, "asc" ]]
            });

            setupChildRows("#tableUsers", table, format);
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
