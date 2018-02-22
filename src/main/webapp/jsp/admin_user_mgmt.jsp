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

        <table id="tableUsers"
               class="table table-hover table-responsive table-paragraphs games-table dataTable display">
            <thead>
            <tr>
                <th>ID</th>
                <th>User</th>
                <th>EMail</th>
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
                    String email = userInfo.get(2);
                    String lastLogin = userInfo.get(3);
                    String totalScore = userInfo.get(5);
            %>

            <tr>
                <td class="col-sm-1"><%= uid%>
                    <input type="hidden" name="added_uid" value=<%=uid%>>
                </td>
                <td class="col-sm-2"><%= username %>
                </td>
                <td class="col-sm-1"><%= email %>
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

    <form id="createUsers" action="admin/users" method="post">
        <input type="hidden" name="formType" value="createUsers">

        <div class="form-group">
            <label for="user_name_list">User Names or Email Addresses</label>
            <a data-toggle="collapse" href="#demo" style="color:black">
                <span class="glyphicon glyphicon-question-sign"></span>
            </a>
            <div id="demo" class="collapse">
                Newline seperated list of usernames or Email Addresses.
                <br/>Email Addresses are any strings with an @, usernames anything else.
                <br/>Usernames are generated from the Email Addresses (<i>name@domain.tld</i> -> <i>name</i>).
                <br/>Passwords are auto generated and shown as messages plus written to the logs.
                <br/><br/>You can also specify name, Email address (and password) in the form:
                <br/> <i>name mail</i>, <i>mail name</i>, <i>name mail password</i>, <i>mail name password</i>,
                delimited by spaces, semicolons or commas
            </div>
            <textarea class="form-control" rows="10" id="user_name_list" name="user_name_list"
                      oninput="document.getElementById('submit_users_btn').disabled = false;"></textarea>
        </div>

        <button class="btn btn-md btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled>
            Create Accounts
        </button>

    </form>
</div>
<%@ include file="/jsp/footer.jsp" %>
