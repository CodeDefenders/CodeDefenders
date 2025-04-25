<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%
    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
    boolean registrationEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REGISTRATION).getBoolValue();
    boolean emailEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue();
    String resetPw = (String) request.getAttribute("resetPW");

    pageContext.setAttribute("pwMinLength", pwMinLength);
    pageContext.setAttribute("registrationEnabled", registrationEnabled);
    pageContext.setAttribute("emailEnabled", emailEnabled);
    pageContext.setAttribute("resetPw", resetPw);
%>

<p:main_page title="Login">
    <div id="login" class="container" style="max-width: 25rem;">
        <h2>Sign in</h2>
        <form action="${url.forPath(Paths.LOGIN)}" method="post" id="login-form" class="needs-validation">

            <div class="row g-3">
                <div class="col-12">
                    <label for="login-username-input" class="form-label">Username</label>
                    <input type="text" class="form-control" id="login-username-input" name="username" placeholder="Username"
                           required>
                    <div class="invalid-feedback">
                        Please enter your username.
                    </div>
                </div>

                <div class="col-12">
                    <label for="login-password-input" class="form-label">Password</label>
                    <input type="password" class="form-control" id="login-password-input" name="password" placeholder="Password"
                           required>
                    <div class="invalid-feedback">
                        Please enter your password.
                    </div>
                </div>

                <div class="col-12">
                    <div class="form-check">
                        <input type="checkbox" class="form-check-input" id="login-consent-checkbox" checked
                               required>
                        <label for="login-consent-checkbox" class="form-check-label">
                            I understand and consent that the mutants and tests I create in the game will be used for research purposes.
                        </label>
                    </div>
                </div>

                <div class="col-12">
                    <button id="login-button" type="submit" class="btn btn-primary btn-lg w-100">Sign in</button>
                </div>

                <div class="col-12 d-flex justify-content-between">
                    <c:if test="${registrationEnabled}">
                        <a id="createacc-link" href="#" data-bs-toggle="modal" data-bs-target="#createacc-modal">Create an account</a>
                    </c:if>

                    <c:if test="${emailEnabled}">
                        <a id="password-forgotten-link" href="#" data-bs-toggle="modal" data-bs-target="#resetpw-modal">Password forgotten</a>
                    </c:if>
                </div>
            </div>

        </form>
    </div>

    <div id="createacc-modal" class="modal fade" tabindex="-1" aria-labelledby="createacc-modal-title" aria-hidden="true">
        <div class="modal-dialog" style="max-width: 30rem;">
            <div class="modal-content">
                <form action="${url.forPath(Paths.USER)}" method="post" class="needs-validation" autocomplete="off">
                    <input type="hidden" name="formType" value="create">

                    <div class="modal-header">
                        <h5 class="modal-title" id="createacc-modal-title">Create a new account</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">

                        <div class="row g-3">
                            <div class="col-12">
                                <label for="createacc-username-input" class="form-label">Username</label>
                                <input type="text" class="form-control" id="createacc-username-input" name="username" placeholder="Username"
                                       required minlength="3" maxlength="20" pattern="[a-z][a-zA-Z0-9]*" autofocus>
                                <div class="invalid-feedback">
                                    Please enter a valid username.
                                </div>
                                <div class="form-text">
                                    3-20 alphanumerics starting with a lowercase letter (a-z), no space or special characters.
                                </div>
                            </div>

                            <div class="col-12">
                                <label for="createacc-email-input" class="form-label">Email</label>
                                <input type="email" class="form-control" id="createacc-email-input" name="email" placeholder="Email"
                                       required>
                                <div class="invalid-feedback">
                                    Please enter a valid email address.
                                </div>
                            </div>

                            <div class="col-12">
                                <div class="mb-2">
                                    <label for="createacc-password-input" class="form-label">Password</label>
                                    <input type="password" class="form-control" id="createacc-password-input" name="password" placeholder="Password"
                                           required minlength="${pwMinLength}" maxlength="20" pattern="[a-zA-Z0-9]*">
                                    <div class="invalid-feedback">
                                        Please enter a valid password.
                                    </div>
                                </div>

                                <div>
                                    <input type="password" class="form-control" id="createacc-confirm-password-input" name="confirm" placeholder="Confirm Password"
                                           required>
                                    <div class="invalid-feedback" id="createacc-confirm-password-feedback">
                                        Please confirm your password.
                                    </div>
                                    <div class="form-text">
                                        ${pwMinLength}-20 alphanumeric characters, no whitespace or special characters.
                                    </div>
                                </div>
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
    </script>

    <div id="resetpw-modal" class="modal fade" tabindex="-1" aria-labelledby="resetpw-modal-title" aria-hidden="true">
        <div class="modal-dialog" style="max-width: 30rem;">
            <div class="modal-content">
                <form action="${url.forPath(Paths.PASSWORD)}" method="post" class="needs-validation" autocomplete="off">
                    <input type="hidden" name="formType" value="resetPassword">

                    <div class="modal-header">
                        <h5 class="modal-title" id="resetpw-modal-title">Reset your password</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">

                        <div class="row g-3">
                            <div class="col-12">
                                <label for="resetpw-username-input" class="form-label">Username</label>
                                <input type="text" class="form-control" id="resetpw-username-input" name="accountUsername" placeholder="Username"
                                       required autofocus>
                                <div class="invalid-feedback">
                                    Please enter your username.
                                </div>
                            </div>

                            <div class="col-12">
                                <label for="resetpw-email-input" class="form-label">Email</label>
                                <input type="email" class="form-control" id="resetpw-email-input" name="accountEmail" placeholder="Email"
                                       required>
                                <div class="invalid-feedback" id="resetpw-email-feedback">
                                    Please enter your email address.
                                </div>
                            </div>

                            <div class="col-12">
                                <div class="form-text">
                                    This will send a mail with a link to change your password to your email account.
                                </div>
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
        const emailInput = document.getElementById('resetpw-email-input');
        const emailFeedback = document.getElementById('resetpw-email-feedback');

        emailInput.addEventListener('input', function () {
            if (emailInput.validity.valueMissing) {
                emailFeedback.innerText = 'Please enter your email address.';
            } else if (emailInput.validity.typeMismatch) {
                emailFeedback.innerText = 'Please enter a valid email address.';
            }
        });
    </script>

    <c:if test="${resetPw != null}">
        <div id="changepw-modal" class="modal fade" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="changepw-modal-title" aria-hidden="true">
            <div class="modal-dialog" style="max-width: 30rem;">
                <div class="modal-content">
                    <form action="${url.forPath(Paths.PASSWORD)}" method="post" class="needs-validation" autocomplete="off">
                        <input type="hidden" name="resetPwSecret" id="resetPwSecret" value="${resetPw}">
                        <input type="hidden" name="formType" value="changePassword">

                        <div class="modal-header">
                            <h5 class="modal-title" id="changepw-modal-title">Reset your password</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">

                            <div class="row g-3">
                                <div class="col-12">
                                    <div class="mb-2">
                                        <label for="changepw-password-input" class="form-label">Password</label>
                                        <!-- TODO(Alex): Increase maxLength. OWASP says: "A common maximum length is 64 characters due to limitations in certain hashing algorithms […]"  -->
                                        <input type="password" class="form-control" id="changepw-password-input" name="inputPasswordChange" placeholder="Password"
                                               required minlength="${pwMinLength}" maxlength="20" pattern="[a-zA-Z0-9]*">
                                        <div class="invalid-feedback">
                                            Please enter a valid password.
                                        </div>
                                    </div>

                                    <div>
                                        <input type="password" class="form-control" id="changepw-confirm-password-input" name="inputConfirmPasswordChange" placeholder="Confirm Password"
                                               required>
                                        <div class="invalid-feedback" id="changepw-confirm-password-feedback">
                                            Please confirm your password.
                                        </div>
                                        <div class="form-text">
                                            ${pwMinLength}-20 alphanumeric characters, no whitespace or special characters.
                                        </div>
                                    </div>
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

        <script type="module">
            import {Modal} from '${url.forPath("/js/bootstrap.mjs")}';


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

            new Modal('#changepw-modal').show();
        </script>
    </c:if>
</p:main_page>
