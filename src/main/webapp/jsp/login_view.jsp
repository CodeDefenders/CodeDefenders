<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.Paths" %>

<jsp:include page="/jsp/header_logout.jsp"/>

<div id="login" class="container">
    <div style="padding: 15px; margin: 0 auto; max-width: 25rem;">
        <h2 class="form-signin-heading">Sign in</h2>
        <form action="<%=request.getContextPath() + Paths.LOGIN%>" method="post" id="form-signin">

            <div class="form-group">
                <label for="inputUsername">Username</label>
                <input type="text" class="form-control" id="inputUsername" name="username" placeholder="Username">
            </div>

            <div class="form-group">
                <label for="inputPassword">Password</label>
                <input type="password" class="form-control" id="inputPassword" name="password" placeholder="Password">
            </div>

            <div class="checkbox">
                <label>
                    <input type="checkbox" id="consentOK" checked>
                    I understand and consent that the mutants and tests I create in the game will be used for research purposes.
                </label>
            </div>

            <button id="signInButton" type="submit" class="btn btn-primary btn-lg"
                    style="width: 100%; margin-bottom: .5rem;">
                Sign in
            </button>

            <% if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REGISTRATION).getBoolValue()) { %>
                <a id="createAccountToggle" href="#" class="text-center new-account" data-toggle="modal" data-target="#createAccountModal">Create an account</a>
            <% } %>

            <% if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue()) {
                    /* a newly generated password can only be sent to the user if mails are enabled */ %>
                <a href="#" class="text-center new-account" data-toggle="modal" data-target="#passwordResetModal"
                   style="float: right;" id="passwordForgotten">Password forgotten</a>
            <% } %>
        </form>
    </div>
</div>

<!-- TODO ARE THOSE EVEN USED ? -->
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
    <div class="modal-dialog" style="max-width: 30rem;">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <a class="close" href='login'>&times;</a>
                <h4 class="modal-title">Change your password</h4>
            </div>
            <div class="modal-body">
                <form action="<%=request.getContextPath()  + Paths.PASSWORD%>" method="post" class="form-signin">
                    <input type="hidden" name="resetPwSecret" id="resetPwSecret" value="<%=resetPw%>">
                    <input type="hidden" name="formType" value="changePassword">

                    <div class="form-group" id="group-change-password">
                        <label class="control-label" for="inputUsername">Password</label>
                        <input type="password" class="form-control" id="inputPasswordChange" name="inputPasswordChange" placeholder="Password"
                               onchange="validatePasswordChange()" required minlength="<%=pwMinLength%>">
                    </div>

                    <div class="form-group" id="group-change-password-confirm">
                        <label class="control-label" for="inputPassword">Confirm Password</label>
                        <input type="password" class="form-control" id="inputConfirmPasswordChange" name="inputConfirmPasswordChange" placeholder="Confirm Password"
                               onchange="validatePasswordChange()" required minlength="<%=pwMinLength%>">
                        <span id="reset-password-help" class="help-block" style="display: none;">Passwords don't match.</span>
                    </div>

                    <button id="submitChangePassword" class="btn btn-lg btn-primary" type="submit" disabled
                            style="width: 100%; margin-bottom: .5rem;">
                        Change Password
                    </button>

                    <script>
                        function validatePasswordChange() {
                            const password = document.getElementById('inputPasswordChange').value;
                            const passwordConfirm = document.getElementById('inputConfirmPasswordChange').value;

                            if (password === passwordConfirm || passwordConfirm === '') {
                                document.getElementById('reset-password-help').style.display = 'none';
                                document.getElementById('group-change-password').classList.remove('has-error');
                                document.getElementById('group-change-password-confirm').classList.remove('has-error');
                                document.getElementById('submitChangePassword').disabled = false;
                            } else {
                                document.getElementById('reset-password-help').style.display = null;
                                document.getElementById('group-change-password').classList.add('has-error');
                                document.getElementById('group-change-password-confirm').classList.add('has-error');
                                document.getElementById('submitChangePassword').disabled = true;
                            }
                        }
                    </script>
                </form>
                <span style="font-size: small;">Valid password:
                    <%=pwMinLength%>
                    -20 alphanumeric characters, no whitespace or special character.
                </span>

            </div>
        </div>

    </div>
</div>
<%}%>

<div id="passwordResetModal" class="modal fade" role="dialog">
    <div class="modal-dialog" style="max-width: 30rem;">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Reset your password</h4>
            </div>
            <div class="modal-body">
                <form action="<%=request.getContextPath()  + Paths.PASSWORD%>" method="post" class="form-signin">
                    <input type="hidden" name="formType" value="resetPassword">

                    <div class="form-group">
                        <label for="accountUsername">Username</label>
                        <input type="text" class="form-control" id="accountUsername" name="accountUsername" placeholder="Username"
                               required autofocus>
                    </div>

                    <div class="form-group">
                        <label for="accountEmail">Email</label>
                        <input type="email" class="form-control" id="accountEmail" name="accountEmail" placeholder="Email"
                               required>
                    </div>

                    <button type="submit" class="btn btn-primary btn-lg"
                            style="width: 100%; margin-bottom: .5rem;">
                        Reset Password
                    </button>
                </form>
                <span style="font-size: small;">
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
    <div class="modal-dialog" style="max-width: 30rem;">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Create new account</h4>
            </div>
            <%int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();%>
            <div class="modal-body">
                <div id="create">
                    <form action="<%=request.getContextPath()  + Paths.USER%>" method="post" class="form-signin">
                        <input type="hidden" name="formType" value="create">

                        <div class="form-group" id="group-create-username">
                            <label class="control-label" for="inputUsernameCreate">Username</label>
                            <input type="text" class="form-control" id="inputUsernameCreate" name="username" placeholder="Username"
                                   onkeyup="validateCreateAccountForm()" required minlength="3" maxlength="20" autofocus>
                            <span id="create-username-help" class="help-block" style="display: none;">
                                3-20 alphanumerics starting with a letter (a-z), no space or special characters.
                            </span>
                        </div>

                        <div class="form-group">
                            <label for="inputEmailCreate">Email</label>
                            <input type="email" class="form-control" id="inputEmailCreate" name="email" placeholder="Email"
                                   required>
                        </div>

                        <div class="form-group" id="group-create-password">
                            <label class="control-label" for="inputPasswordCreate">Password</label>
                            <input type="password" class="form-control" id="inputPasswordCreate" name="password" placeholder="Password"
                                   onkeyup="validateCreateAccountForm()" required minlength="<%=pwMinLength%>">
                        </div>

                        <div class="form-group" id="group-create-password-confirm">
                            <label class="control-label" for="inputConfirmPasswordCreate">Confirm Password</label>
                            <input type="password" class="form-control" id="inputConfirmPasswordCreate" name="confirm" placeholder="Confirm Password"
                                   onkeyup="validateCreateAccountForm()" required>
                            <span id="create-password-help" class="help-block" style="display: none;">Passwords don't match.</span>
                        </div>

                        <button class="btn btn-lg btn-primary" id="submitCreateAccount" type="submit"
                                style="width: 100%; margin-bottom: .5rem;">
                            Create Account
                        </button>
                    </form>
                    <span style="font-size: small;">
                        Valid username: 3-20 alphanumerics starting with a letter (a-z), no space or special characters.<br>
                        Valid password: <%=AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue()%>-20 alphanumeric characters, no whitespace or special characters.
                    </span>
                </div>
                <script type="text/javascript">
                    function validateCreateAccountForm() {
                        const valid = validatePassword() && validateUsername();
                        document.getElementById('submitChangePassword').disabled = !valid;
                    }

                    function validatePassword() {
                        const password = document.getElementById('inputPasswordCreate').value;
                        const passwordConfirm = document.getElementById('inputConfirmPasswordCreate').value;

                        if (password === passwordConfirm || passwordConfirm === '') {
                            document.getElementById('create-password-help').style.display = 'none';
                            document.getElementById('group-create-password').classList.remove('has-error');
                            document.getElementById('group-create-password-confirm').classList.remove('has-error');
                            return true;
                        } else {
                            document.getElementById('create-password-help').style.display = null;
                            document.getElementById('group-create-password').classList.add('has-error');
                            document.getElementById('group-create-password-confirm').classList.add('has-error');
                            return false;
                        }
                    }

                    function validateUsername() {
                        const username = document.getElementById('inputUsernameCreate').value;

                        if (isValidUsername(username)) {
                            document.getElementById('create-username-help').style.display = 'none';
                            document.getElementById('group-create-username').classList.remove('has-error');
                            return true;
                        } else {
                            document.getElementById('create-username-help').style.display = null;
                            document.getElementById('group-create-username').classList.add('has-error');
                            return false;
                        }

                        function isValidUsername(username) {
                            // matches 1 a-z for the first char, 2-19 alphanumeric chars for the rest
                            const regExp = new RegExp('^[a-z][a-zA-Z0-9]{2,19}$');
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
