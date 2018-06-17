<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Users</h3>

    <style>
        .table-details {
            margin-left: 50px;
        }

        #toggle-all-details {
            cursor: pointer;
        }
    </style>

    <table id="tableUsers"
           class="table table-striped table-hover table-responsive">
        <thead>
        <tr>
            <th><span id="toggle-all-details" class="glyphicon glyphicon-chevron-right text-muted"></span></th>
            <th>User</th>
            <th>EMail</th>
            <th>Total Score</th>
        </tr>
        </thead>
    </table>

    <script>
        var shown = false;

        function format(d) {
            return ''+
                '<table class="table-child-details">'+
                    '<tbody>'+
                        '<tr>'+
                            '<td class="child-details-left">User ID:</td>'+
                            '<td>'+d.uid+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td class="child-details-left">Last Login:</td>'+
                            '<td>'+d.lastLogin+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>';
        }

        $(document).ready(function() {
            var table = $('#tableUsers').DataTable( {
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
            } );

            $('#tableUsers tbody').on('click', '.toggle-details', function () {
                var tr = $(this).closest('tr');
                var row = table.row(tr);

                /* Toggle the child of the row. */
                if (row.child.isShown()) {
                    row.child.hide();
                    tr.removeClass('shown');
                    $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-up').addClass('glyphicon-chevron-right');
                } else {
                    row.child(format(row.data())).show();
                    tr.addClass('shown');
                    $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-right').addClass('glyphicon-chevron-up');
                }
            });

            $('#toggle-all-details').on('click', function () {
                shown = !shown;
                $(this).toggleClass('glyphicon-chevron-right').toggleClass('glyphicon-chevron-up');

                $('#tableUsers tbody .toggle-details').each(function(toggle) {
                    var tr = $(this).closest('tr');
                    var row = table.row(tr);

                    if (row.child.isShown() && !shown) {
                        row.child.hide();
                        tr.removeClass('shown');
                        $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-up').addClass('glyphicon-chevron-right');
                    } else if (!row.child.isShown() && shown){
                        row.child(format(row.data())).show();
                        tr.addClass('shown');
                        $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-right').addClass('glyphicon-chevron-up');
                    }
                });
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
