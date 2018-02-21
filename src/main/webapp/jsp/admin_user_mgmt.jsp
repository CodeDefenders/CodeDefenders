<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.AdminDAO" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header.jsp" %>

<div class="full-width">
    <ul class="nav nav-tabs">
        <li><a href="<%=request.getContextPath()%>/admin/games"> Manage Games</a></li>
        <li class="active"><a href="#">Manage Users</a></li>
    </ul>

    <h3>Users</h3>

    <form id="manageUsers" action="admin/users" method="post">
        <input type="hidden" name="formType" value="manageUsers">

        <h3>Unassigned Users</h3>
        <table id="tableUsers"
               class="table table-hover table-responsive table-paragraphs games-table dataTable display">
            <thead>
            <tr>
                <th>ID</th>
                <th>User</th>
                <th>Last Role</th>
                <th>Total Score</th>
                <th>Last Login</th>
                <th>Reset Password</th>
                <th>Delete</th>
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
                    String lastLogin = userInfo.get(2);
                    String lastRole = userInfo.get(3);
                    String totalScore = userInfo.get(4);
            %>

            <tr>
                <td class="col-sm-1"><%= uid%>
                    <input type="hidden" name="added_uid" value=<%=uid%>>
                </td>
                <td class="col-sm-2"><%= username %>
                </td>
                <td class="col-sm-1"><%= lastRole %>
                </td>
                <td class="col-sm-1"><%= totalScore %>
                </td>
                <td class="col-sm-2"><%= lastLogin %>
                </td>
                <td class="col-sm-1" style="padding-top:4px; padding-bottom:4px">
                    <%if (currentUserID != uid) {%>
                    <button class="btn btn-sm btn-warning" type="submit" value="<%=uid%>" name="resetPasswordButton"
                            onclick="return confirm('Are you sure you want to reset <%=username%>\'s password?');">
                        <span class="glyphicon glyphicon-repeat"></span>
                    </button>
                    <%}%>
                </td>
                <td class="col-sm-1" style="padding-top:4px; padding-bottom:4px">
                    <%if (currentUserID != uid) {%>
                    <button class="btn btn-sm btn-danger" type="submit" value="<%=uid%>" name="deleteUserButton"
                            onclick="return confirm('Are you sure you want to permanently delete <%=username%>\'s account?');">
                        <span class="glyphicon glyphicon-trash"></span>
                    </button>
                    <%}%>
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
                $('#tableUsers').DataTable({
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
                });
            })
            ;
        </script>

    </form>

    <h3>Create Accounts</h3>
</div>
<%@ include file="/jsp/footer.jsp" %>
