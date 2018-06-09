<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="java.util.List" %>

<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Users</h3>

    <table id="tableUsers"
           class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
        <thead>
        <tr>
            <th>ID</th>
            <th>User</th>
            <th>EMail</th>
            <th>Total Score</th>
            <th>Last Login</th>
            <th style="padding-top:4px; padding-bottom:4px">
                <button class="btn btn-sm btn-primary" id="toggle-all-user-infos">
                    Toggle all
                </button>
            </th>
        </tr>
        </thead>
        <tbody>

        <%
            List<List<String>> unassignedUsersInfo = AdminDAO.getAllUsersInfo();
            if (unassignedUsersInfo.isEmpty()) {
        %>

        <div class="panel panel-default">
            <div class="panel-body" style="    color: gray;    text-align: center;">
                There are currently no created users.
            </div>
        </div>

        <%
        } else {
            int currentUserID = (Integer) session.getAttribute("uid");
            for (List<String> userInfo : unassignedUsersInfo) {
                int uid = Integer.valueOf(userInfo.get(0));
                String username = userInfo.get(1);
                String email = userInfo.get(2);
                String lastLogin = userInfo.get(3);
                String totalScore = userInfo.get(5);
        %>

        <tr class="short-user-info" user-id="<%=uid%>" id="<%="short-user-info-"+uid%>">
            <td class="col-sm-1"><%= uid%>
            </td>
            <td class="col-sm-2"><%= username %>
            </td>
            <td class="col-sm-1"><%= email %>
            </td>
            <td class="col-sm-1"><%= totalScore %>
            </td>
            <td class="col-sm-2"><%= lastLogin %>
            </td>
            <td style="padding-top:4px; padding-bottom:4px">
                <button class="btn btn-sm btn-primary toggle-user-info" user-id="<%=uid%>" id="<%="toggle-user-info-"+uid%>">
                    <span class="glyphicon glyphicon-chevron-down"></span>
                </button>
            </td>
        </tr>

        <tr class="long-user-info" user-id="<%=uid%>" id="<%="long-user-info-"+uid%>" style="display: none;">
            <td class="col-sm-1">
            </td>
            <td class="col-sm-2">
            </td>
            <td class="col-sm-1"><%= email %>
            </td>
            <td class="col-sm-1"><%= totalScore %>
            </td>
            <td class="col-sm-2"><%= lastLogin %>
            </td>
            <td style="padding-top:4px; padding-bottom:4px">
            </td>
        </tr>

        <%
                }
            }
        %>
        </tbody>
    </table>

    <script>
        $(document).ready(function () {
            /*$('#tableUsers').DataTable({
                pagingType: "full_numbers",
                lengthChange: false,
                searching: true,
                order: [[4, "desc"]],
                "columnDefs": [{
                    "targets": 5,
                    "orderable": false
                }, {
                    "targets": 6,
                    "orderable": false
                }]
            });*/

            $('.toggle-user-info').on("click", function() {
                $(".long-user-info[user-id=" + $(this).attr("user-id") + "]").toggle();
                $(this).children("span").toggleClass("glyphicon-chevron-down").toggleClass("glyphicon-chevron-up")
            });

            $('#toggle-all-user-infos').on("click", function() {
                $(".long-user-info").toggle();
                $(".toggle-user-info span").toggleClass("glyphicon-chevron-down").toggleClass("glyphicon-chevron-up")
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>
