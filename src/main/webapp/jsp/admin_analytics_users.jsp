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
                <th>User</th>
                <th>EMail</th>
                <th>Total Score</th>
            </tr>
        </thead>
    </table>

    <script src="js/datatables-child-rows.js" type="text/javascript" ></script>

    <script>
        function format(data) {
            return ''+
                '<table class="table-child-details">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td class="child-details-left">User ID:</td>'+
                            '<td>'+data.uid+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td class="child-details-left">Last Login:</td>'+
                            '<td>'+data.lastLogin+'</td>'+
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
                    { "data": "username" },
                    { "data": "email" },
                    { "data": "totalScore" }
                ],
                "order": [[ 1, "asc" ]]
            });

            setupChildRows("#tableUsers", table, format);
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
