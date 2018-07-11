<%@ page import="org.codedefenders.servlets.FeedbackManager" %>
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
                <th>Tests per Game</th>
                <th>Mutants Submitted</th>
                <th>Mutants per Game</th>
            </tr>
        </thead>
    </table>

    <div class="btn-group">
        <a download="classes-analytics.csv" href="<%=request.getContextPath()+Constants.API_ANALYTICS_CLASSES%>?type=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="classes-analytics.json" href="<%=request.getContextPath()+Constants.API_ANALYTICS_CLASSES%>?type=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>

    <script src="js/datatables-utils.js" type="text/javascript" ></script>

    <script>
        var table;

        function format(data) {
            var rating1 = data.ratings.cutMutationDifficulty;
            var rating2 = data.ratings.cutTestDifficulty;
            var rating3 = data.ratings.gameEngaging;

            return '' +
                '<table class="table-child-details indented">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutants Alive:</td>'+
                            '<td>'+data.mutantsAlive+'</td>'+
                            '<td>Per Game:</td>'+
                            '<td>'+(data.nrGames === 0 ? 0 : (data.mutantsAlive/data.nrGames).toFixed(2))+'</td>'+
                            '<td>Per Mutant:</td>'+
                            '<td>'+(data.mutantsSubmitted === 0 ? 0 : (data.mutantsAlive/data.mutantsSubmitted).toFixed(2))+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Mutants Equivalent:</td>'+
                            '<td>'+data.mutantsEquivalent+'</td>'+
                            '<td>Per Game:</td>'+
                            '<td>'+(data.nrGames === 0 ? 0 : (data.mutantsEquivalent/data.nrGames).toFixed(2))+'</td>'+
                            '<td>Per Mutant:</td>'+
                            '<td>'+(data.mutantsSubmitted === 0 ? 0 : (data.mutantsEquivalent/data.mutantsSubmitted)).toFixed(2)+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>'+
                '<table class="table-child-details indented">'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Feedback</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutation Difficulty:</td>'+
                            '<td>'+(rating1.count === 0 ? 'NA' : (rating1.sum/rating1.count).toFixed(2))+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating1.count+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Test Difficulty:</td>'+
                            '<td>'+(rating2.count === 0 ? 'NA' : (rating2.sum/rating2.count).toFixed(2))+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating2.count+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Game is engaging:</td>'+
                            '<td>'+(rating3.count === 0 ? 'NA' : (rating3.sum/rating3.count).toFixed(2))+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating3.count+'</td>'+
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
                    { "data": "nrGames" },
                    { "data": "testsSubmitted" },
                    { "data":
                            function(row, type, val, meta) {
                                return row.nrGames === 0 ? 0 : (row.testsSubmitted/row.nrGames).toFixed(2);
                            }
                    },
                    { "data": "mutantsSubmitted" },
                    { "data":
                            function(row, type, val, meta) {
                                return row.nrGames === 0 ? 0 : (row.mutantsSubmitted/row.nrGames).toFixed(2);
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
