<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.List" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminUserMgmt"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <%
        String editUser = request.getParameter("editUser");
        if (editUser != null && editUser.length() > 0 && StringUtils.isNumeric(editUser)) {
            User u = DatabaseAccess.getUser(Integer.parseInt(editUser));
            if (u != null) {
    %>
    <h3>Edit Info for User <%=u.getId()%>
    </h3>

    <form id="editUser" action="admin/users" method="post">
        <input type="hidden" name="formType" value="editUser">
        <input type="hidden" name="uid" value="<%=u.getId()%>">
        <div class="input-group">
            <span class="input-group-addon"><i class="glyphicon glyphicon-user"></i> </span>
            <input style ="padding-left:5px" id="name" type="text" class="form-control" name="name" value="<%=u.getUsername()%>">
        </div>
        <br>
        <div class="input-group">
            <span class="input-group-addon"><i class="glyphicon glyphicon-envelope"></i></span>
            <input style ="padding-left:5px" id="email" type="email" class="form-control" name="email" value="<%=u.getEmail()%>">
        </div>
        <br>
        <div class="input-group">
            <span class="input-group-addon"><i class="glyphicon glyphicon-lock"></i></span>
            <input style ="padding-left:5px" id="password" type="password" class="form-control"
                   name="password" placeholder="unchanged" onkeyup="check()">
        </div>
        <span class = "label label-danger" style = "color: white" id = "pw_confirm_message"></span>
        <br>
        <div class="input-group">
            <span class="input-group-addon"><i class="glyphicon glyphicon-lock"></i></span>
            <input style ="padding-left:5px" id="confirm_password" type="password" class="form-control"
                   name="confirm_password" placeholder="confirm password" onkeyup="check()">
        </div>
        <br>
        <button type="submit" class="btn btn-primary btn-block" name = "submit_edit_user" id = "submit_edit_user"> Submit</button>

        <script>
            function check() {
                if (document.getElementById('password').value ===
                    document.getElementById('confirm_password').value) {
                    document.getElementById('pw_confirm_message').innerHTML = '';
                    document.getElementById('submit_edit_user').disabled = false;
                } else {
                    document.getElementById('pw_confirm_message').innerHTML = 'Passwords don\'t match!';
                    document.getElementById('submit_edit_user').disabled = true;
                }
            }
        </script>
    </form>
    <%
            }
        }
    %>

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
                <th></th>
                <th></th>
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

            <tr id="<%="user_row_"+uid%>">
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
                <td style="padding-top:4px; padding-bottom:4px">
                    <button class="btn btn-sm btn-primary" id="<%="edit_user_"+uid%>" name="editUserInfo" type="submit" value="<%=uid%>">
                        <span class="glyphicon glyphicon-pencil"></span>
                    </button>
                </td>
                <td style="padding-top:4px; padding-bottom:4px">
                    <%if (currentUserID != uid) {%>
                    <button class="btn btn-sm btn-danger" id="<%="remove_user_"+uid%>" type="submit" value="<%=uid%>" name="deleteUserButton"
                            onclick="return confirm('Are you sure you want to permanently delete <%=username%>\'s ' +
                                    'account? \nThis will also delete all their games, mutants, tests, equivalences' +
                                    ' plus the games\'s mutants, tests and equivalences');" disabled>
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
            function reload() {
                location.reload();
            }

            $(document).ready(function () {
                $('[data-toggle="tooltip"]').tooltip();

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
            <label for="user_name_list">List of user credentials. Show help</label>
            <a data-toggle="collapse" href="#demo" style="color:black">
                <span class="glyphicon glyphicon-question-sign"></span>
            </a>
            <div id="demo" class="collapse">
                List of usernames, passwords and emails (optional).
                <br>
                Fields are separated by commas (<code>,</code>) or semicolons (<code>;</code>).
                Users are separated by new lines.
                <p>
                If an email is provided and sending emails is enabled, created users receive an email with their credentials.
                <p>
                Valid input examples:
                <br>
                <code>username,password
                    <br>
                    username2,password,example@mail.com
                    <br>
                    username3;password
                    <br>
                    username4;password;example@mail.com
                </code>

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
