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

<%
    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
%>

<div id="login" class="container">
    <div class="mx-auto" style="max-width: 25rem;">
        <h2>Sign in</h2>
        <form action="<%=request.getContextPath() + Paths.LOGIN%>" method="post" id="login-form" class="needs-validation">

            <div class="mb-3">
                <label for="login-username-input" class="form-label">Username</label>
                <input type="text" class="form-control" id="login-username-input" name="username" placeholder="Username"
                       required>
                <div class="invalid-feedback">
                    Please enter your username.
                </div>
            </div>

            <div class="mb-3">
                <label for="login-password-input" class="form-label">Password</label>
                <input type="password" class="form-control" id="login-password-input" name="password" placeholder="Password"
                       required>
                <div class="invalid-feedback">
                    Please enter your password.
                </div>
            </div>

            <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="login-consent-checkbox" checked
                       required>
                <label for="login-consent-checkbox" class="form-check-label">
                    I understand and consent that the mutants and tests I create in the game will be used for research purposes.
                </label>
            </div>

            <button id="login-button" type="submit" class="btn btn-primary btn-lg w-100 mb-3">Sign in</button>

            <div class="d-flex justify-content-between">
                <% if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REGISTRATION).getBoolValue()) { %>
                    <a id="createacc-link" href="#" data-bs-toggle="modal" data-bs-target="#createacc-modal">Create an account</a>
                <% } %>

                <% if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue()) { %>
                    <a id="password-forgotten-link" href="#" data-bs-toggle="modal" data-bs-target="#resetpw-modal">Password forgotten</a>
                <% } %>
            </div>

        </form>
    </div>
</div>

<div id="createacc-modal" class="modal fade" tabindex="-1" aria-labelledby="createacc-modal-title" aria-hidden="true">
    <div class="modal-dialog" style="max-width: 30rem;">
        <div class="modal-content">
            <form action="<%=request.getContextPath() + Paths.USER%>" method="post" class="needs-validation">
                <input type="hidden" name="formType" value="create">
                <div class="modal-header">
                    <h5 class="modal-title" id="createacc-modal-title">Create a new account</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">

                    <div class="mb-3">
                        <label for="createacc-username-input" class="form-label">Username</label>
                        <input type="text" class="form-control" id="createacc-username-input" name="username" placeholder="Username"
                               required  minlength="3" maxlength="20" pattern="[a-z][a-zA-Z0-9]*" autofocus>
                        <div class="invalid-feedback">
                            Please enter a valid username.
                        </div>
                        <div class="form-text">
                            3-20 alphanumerics starting with a lowercase letter (a-z), no space or special characters.
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="createacc-email-input" class="form-label">Email</label>
                        <input type="email" class="form-control" id="createacc-email-input" name="email" placeholder="Email"
                               required>
                        <div class="invalid-feedback">
                            Please enter a valid email address.
                        </div>
                    </div>

                    <div class="mb-2">
                        <label for="createacc-password-input" class="form-label">Password</label>
                        <input type="password" class="form-control" id="createacc-password-input" name="password" placeholder="Password"
                               required minlength="<%=pwMinLength%>" maxlength="20" pattern="[a-zA-Z0-9]*">
                        <div class="invalid-feedback">
                            Please enter a valid password.
                        </div>
                    </div>

                    <div class="mb-3">
                        <input type="password" class="form-control" id="createacc-confirm-password-input" name="confirm" placeholder="Confirm Password"
                               required>
                        <div class="invalid-feedback" id="createacc-confirm-password-feedback">
                            Please confirm your password.
                        </div>
                        <div class="form-text">
                            <%=pwMinLength%>-20 alphanumeric characters, no whitespace or special characters.
                        </div>
                    </div>

                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Create Account</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    $(document).ready(() => {
        const passwordInput = document.getElementById('createacc-password-input');
        const confirmPasswordInput = document.getElementById('createacc-confirm-password-input');
        const confirmPasswordFeedback = document.getElementById('createacc-confirm-password-feedback');

        const validateConfirmPassword = function () {
            if (confirmPasswordInput.validity.valueMissing) {
                confirmPasswordFeedback.innerText = 'Please confirm your password.';
            } else {
                if (passwordInput.value === confirmPasswordInput.value)  {
                    confirmPasswordInput.setCustomValidity('');
                    confirmPasswordFeedback.innerText = '';
                } else {
                    confirmPasswordInput.setCustomValidity('password-mismatch');
                    confirmPasswordFeedback.innerText = "Passwords don't match.";
                }
            }
        };

        passwordInput.addEventListener('input', validateConfirmPassword);
        confirmPasswordInput.addEventListener('input', validateConfirmPassword);
    });
</script>

<div id="resetpw-modal" class="modal fade" tabindex="-1" aria-labelledby="resetpw-modal-title" aria-hidden="true">
    <div class="modal-dialog" style="max-width: 30rem;">
        <div class="modal-content">
            <form action="<%=request.getContextPath() + Paths.PASSWORD%>" method="post" class="needs-validation">
                <input type="hidden" name="formType" value="resetPassword">
                <div class="modal-header">
                    <h5 class="modal-title" id="resetpw-modal-title">Reset your password</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">

                    <div class="mb-3">
                        <label for="resetpw-username-input" class="form-label">Username</label>
                        <input type="text" class="form-control" id="resetpw-username-input" name="accountUsername" placeholder="Username"
                               required autofocus>
                        <div class="invalid-feedback">
                            Please enter your username.
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="resetpw-email-input" class="form-label">Email</label>
                        <input type="email" class="form-control" id="resetpw-email-input" name="accountEmail" placeholder="Email"
                               required>
                        <div class="invalid-feedback" id="resetpw-email-feedback">
                            Please enter your email address.
                        </div>
                    </div>

                    <div class="mb-1">
                        <div class="form-text">
                            This will send a mail with a link to change your password to your email account.
                        </div>
                    </div>

                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Reset Password</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    $(document).ready(() => {
        const emailInput = document.getElementById('resetpw-email-input');
        const emailFeedback = document.getElementById('resetpw-email-feedback');

        emailInput.addEventListener('input', function () {
            if (emailInput.validity.valueMissing) {
                emailFeedback.innerText = 'Please enter your email address.';
            } else if (emailInput.validity.typeMismatch) {
                emailFeedback.innerText = 'Please enter a valid email address.';
            }
        });
    });
</script>


<%
    String resetPw = request.getParameter("resetPW");
    if (resetPw != null && DatabaseAccess.getUserIDForPWResetSecret(resetPw) > 0) {
%>

<div id="changepw-modal" class="modal fade" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="changepw-modal-title" aria-hidden="true">
    <div class="modal-dialog" style="max-width: 30rem;">
        <div class="modal-content">
            <form action="<%=request.getContextPath() + Paths.PASSWORD%>" method="post" class="needs-validation">
                <input type="hidden" name="resetPwSecret" id="resetPwSecret" value="<%=resetPw%>">
                <input type="hidden" name="formType" value="changePassword">
                <div class="modal-header">
                    <h5 class="modal-title" id="changepw-modal-title">Reset your password</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">

                    <div class="mb-2">
                        <label for="changepw-password-input" class="form-label">Password</label>
                        <input type="password" class="form-control" id="changepw-password-input" name="inputPasswordChange" placeholder="Password"
                               required minlength="<%=pwMinLength%>" maxlength="20" pattern="[a-zA-Z0-9]*">
                        <div class="invalid-feedback">
                            Please enter a valid password.
                        </div>
                    </div>

                    <div class="mb-3">
                        <input type="password" class="form-control" id="changepw-confirm-password-input" name="inputConfirmPasswordChange" placeholder="Confirm Password"
                               required>
                        <div class="invalid-feedback" id="changepw-confirm-password-feedback">
                            Please confirm your password.
                        </div>
                        <div class="form-text">
                            <%=pwMinLength%>-20 alphanumeric characters, no whitespace or special characters.
                        </div>
                    </div>

                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Change Password</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        const passwordInput = document.getElementById('changepw-password-input');
        const confirmPasswordInput = document.getElementById('changepw-confirm-password-input');
        const confirmPasswordFeedback = document.getElementById('changepw-confirm-password-feedback');

        const validateConfirmPassword = function () {
            if (confirmPasswordInput.validity.valueMissing) {
                confirmPasswordFeedback.innerText = 'Please confirm your password.';
            } else {
                if (passwordInput.value === confirmPasswordInput.value)  {
                    confirmPasswordInput.setCustomValidity('');
                    confirmPasswordFeedback.innerText = '';
                } else {
                    confirmPasswordInput.setCustomValidity('password-mismatch');
                    confirmPasswordFeedback.innerText = "Passwords don't match.";
                }
            }
        };

        passwordInput.addEventListener('input', validateConfirmPassword);
        confirmPasswordInput.addEventListener('input', validateConfirmPassword);

        new bootstrap.Modal('#changepw-modal').show();
    });
</script>

<%
    }
%>

<%@ include file="/jsp/footer.jsp" %>
