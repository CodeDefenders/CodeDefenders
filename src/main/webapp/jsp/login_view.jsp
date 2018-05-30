<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<% String pageTitle = "Login"; %>

<%@ include file="/jsp/header_logout.jsp" %>

<%
    Object uid = request.getSession().getAttribute("uid");
    Object username = request.getSession().getAttribute("username");
    if (uid != null && username != null)
        response.sendRedirect(request.getContextPath() + "/games");
%>


<div id="login" class="container">
    <form action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
        <input type="hidden" name="formType" value="login">
        <h2 class="form-signin-heading">Sign in</h2>
        <label for="inputUsername" class="sr-only">Username/Email</label>
        <input type="text" id="inputUsername" name="username" class="form-control" placeholder="Username or Email"
               required autofocus>
        <label for="inputPassword" class="sr-only">Password</label>
        <input type="password" id="inputPassword" name="password" class="form-control" placeholder="Password" required>
        <div>
            <input type="checkbox" id="consentOK" style="margin-right:5px;" checked>I understand and consent that the
            mutants and tests I create in the game will be used for research purposes.
        </div>
        <button class="btn btn-lg btn-primary btn-block" id="signInButton" type="submit">Sign in</button>

        <%if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REGISTRATION).getBoolValue()) { %>
        <a id="createAccountToggle" href="#" class="text-center new-account" data-toggle="modal" data-target="#createAccountModal">Create an account</a>
        <%}%>

        <%
            if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue()) {
                // a newly generated password can only be sent to the user if mails are enabled
        %>
        <a href="#" class="text-center new-account" data-toggle="modal" data-target="#passwordResetModal"
           style=" float: right;" id="passwordForgotten">Password forgotten</a>
        <%}%>

    </form>
</div>

<% String resetPw = request.getParameter("resetPW");
    if (resetPw != null &&
            DatabaseAccess.getUserIDForPWResetSecret(resetPw) > 0) {
        int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();%>
<div id="changePasswordModal" class="fade in" role="dialog" style="
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 1050;
    background: #00000080;">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <a class="close" href='login'>&times;</a>
                <h4 class="modal-title">Change your password</h4>
            </div>
            <div class="modal-body">
                <form action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
                    <input type="hidden" name="resetPwSecret" id="resetPwSecret" value="<%=resetPw%>">

                    <input type="hidden" name="formType" value="changePassword">
                    <label for="inputPassword" class="sr-only">Password</label>
                    <input type="password" id="inputPasswordChange" name="inputPasswordChange" class="form-control"
                           onkeyup="validatePasswordChange()" placeholder="Password" required minlength="<%=pwMinLength%>">
                    <span class="label label-danger" style="color: white" id="pw_confirm_message_change"></span>
                    <label for="inputPassword" class="sr-only">Confirm Password</label>
                    <input type="password" id="inputConfirmPasswordChange" name="inputConfirmPasswordChange"
                           onkeyup="validatePasswordChange()" class="form-control" placeholder="Confirm Password" required
                           minlength="<%=pwMinLength%>">
                    <button id="submitChangePassword" disabled class="btn btn-lg btn-primary btn-block"
                            type="submit">Change Password
                    </button>

                    <script>
                        function validatePasswordChange() {
                            if (document.getElementById('inputPasswordChange').value ===
                                document.getElementById('inputConfirmPasswordChange').value) {
                                document.getElementById('pw_confirm_message_change').innerHTML = '';
                                document.getElementById('submitChangePassword').disabled = false;
                            } else {
                                document.getElementById('pw_confirm_message_change').innerHTML = 'Passwords don\'t match!';
                                document.getElementById('submitChangePassword').disabled = true;
                            }
                        }
                    </script>
                </form>
                <span style="margin-right:5px; font-size:small;">Valid password:
                    <%=pwMinLength%>
                    -20 alphanumeric characters, no whitespace or special character.</span>

            </div>
        </div>

    </div>
</div>
<%}%>

<div id="passwordResetModal" class="modal fade" role="dialog">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Reset your password</h4>
            </div>
            <div class="modal-body">
                <form action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
                    <input type="hidden" name="formType" value="resetPassword">
                    <label for="inputUsername" class="sr-only">Username</label>
                    <input type="text" id="accountUsername" name="accountUsername" class="form-control"
                           placeholder="Username" required autofocus>
                    <label for="inputEmail" class="sr-only">Email</label>
                    <input type="email" id="accountEmail" name="accountEmail" class="form-control" placeholder="Email"
                           required>
                    <button class="btn btn-lg btn-primary btn-block" type="submit">Reset Password</button>
                </form>
                <span style="margin-right:5px; font-size:small;">
                    This will send a mail with a link to change your password to your email account.
                </span>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>

    </div>
</div>

<!-- Modal -->
<div id="createAccountModal" class="modal fade" role="dialog">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Create new account</h4>
            </div>
            <%int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();%>
            <div class="modal-body">
                <div id="create">
                    <form action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
                        <input type="hidden" name="formType" value="create">
                        <label for="inputUsername" class="sr-only">Username</label>
                        <span class="label label-danger" id="invalid_username_message" style="color: white;visibility: hidden">3-20 alphanumerics starting with a letter (a-z), no space or special characters.</span>
                        <input type="text" id="inputUsernameCreate" name="username" class="form-control"
                               onkeyup="validateUsername()" placeholder="Username" required minlength="3" maxlength="20" autofocus>
                        <label for="inputEmail" class="sr-only">Email</label>
                        <input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
                        <label for="inputPassword" class="sr-only">Password</label>
                        <input type="password" id="inputPasswordCreate" name="password" class="form-control"
                               onkeyup="validatePassword()" placeholder="Password" required minlength="<%=pwMinLength%>" maxlength="20">
                        <span class="label label-danger" id="pw_confirm_message_create" style="color: white;visibility: hidden">Passwords do not match!</span>
                        <label for="inputPassword" class="sr-only">Password</label>
                        <input type="password" id="inputConfirmPasswordCreate" name="confirm" class="form-control"
                               onkeyup="validatePassword()" placeholder="Confirm Password" required>
                        <button class="btn btn-lg btn-primary btn-block" id="submitCreateAccount" type="submit">Create Account</button>
                    </form>
                    <span style="margin-right:5px; font-size:small;">Valid username: 3-20 alphanumerics starting with a letter (a-z), no space or special characters.<br>
                        Valid password: <%=AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue()%>-20 alphanumeric characters, no whitespace or special characters.</span>
                </div>
                <script type="text/javascript">
                    function validatePassword() {
                        if(document.getElementById('inputConfirmPasswordCreate').value === '') {
                            document.getElementById('pw_confirm_message_create').style.visibility = "hidden";
                            return;
                        }

                        if (document.getElementById('inputPasswordCreate').value === document.getElementById('inputConfirmPasswordCreate').value) {
                            document.getElementById('pw_confirm_message_create').style.visibility = "hidden";
                            document.getElementById('submitCreateAccount').disabled = false;
                        } else {
                            document.getElementById('pw_confirm_message_create').style.visibility = "visible";
                            document.getElementById('submitCreateAccount').disabled = true;
                        }
                    }
                    function validateUsername() {
                        var username = document.getElementById('inputUsernameCreate').value;
                        if (isValidUsername(username)) {
                            document.getElementById('invalid_username_message').style.visibility = "hidden";
                            document.getElementById('submitCreateAccount').disabled = false;
                        } else {
                            document.getElementById('invalid_username_message').style.visibility = "visible";
                            document.getElementById('submitCreateAccount').disabled = true;
                        }

                        function isValidUsername(username) {
                            // matches 1 a-z for the first char, 2-19 alphanumeric chars for the rest
                            var regExp = new RegExp('^[a-z][a-zA-Z0-9]{2,19}$');
                            return regExp.test(username);
                        }
                    }
                </script>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>

    </div>
</div>
<script>
    $('#consentOK').click(function () {
        document.getElementById("signInButton").disabled = !$(this).is(':checked');
    });
</script>

<%@ include file="/jsp/footer.jsp" %>
