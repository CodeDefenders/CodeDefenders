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
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%
    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
    pageContext.setAttribute("pwMinLength", pwMinLength);
%>

<c:set var="title" value="Account Settings"/>

<p:main_page title="${title}">
    <div class="container form-width">
        <h1>${title}</h1>

        <section class="mt-5" aria-labelledby="email-settings">
            <h2 class="mb-3" id="email-settings">Email Settings</h2>

            <form action="${url.forPath(Paths.USER_SETTINGS)}" method="post"
                  class="row g-3 needs-validation"
                  autocomplete="off">
                <input type="hidden" class="form-control" name="formType" value="updateProfile">

                <div class="col-12">
                    <div class="mb-2">
                        <label for="updatedEmail" class="form-label">Email</label>
                        <input type="email" class="form-control" id="updatedEmail" name="updatedEmail"
                               value="${login.user.email}" placeholder="Email" required>
                    </div>

                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="allowContact" name="allowContact"
                               ${login.user.contactingAllowed ? "checked" : ""}>
                        <label class="form-check-label" for="allowContact">
                            Allow us to contact your email address.
                        </label>
                    </div>
                </div>

                <div class="col-12">
                    <button id="submitUpdateProfile" type="submit" class="btn btn-primary">Update Email Preferences</button>
                </div>
            </form>
        </section>

        <section class="mt-5" aria-labelledby="password-settings">
            <h2 class="mb-3" id="password-settings">Update Password</h2>

            <form action="${url.forPath(Paths.USER_SETTINGS)}" method="post"
                  class="row g-3 needs-validation"
                  autocomplete="off">
                <input type="hidden" class="form-control" name="formType" value="changePassword">

                <div class="col-12">
                    <div class="mb-2">
                        <label for="updatedPassword" class="form-label">New password</label>
                        <input type="password" class="form-control" id="updatedPassword" required
                               name="updatedPassword" placeholder="Password"
                               minlength="${pwMinLength}" maxlength="1000" pattern="[a-zA-Z0-9]*">
                        <div class="invalid-feedback">
                            Please enter a valid password.
                        </div>
                    </div>

                    <div>
                        <input type="password" class="form-control" id="repeatedPassword" name="repeatedPassword"
                               placeholder="Confirm Password" aria-label="Confirm Password">
                        <div class="invalid-feedback" id="confirm-password-feedback">
                            Please confirm your password.
                        </div>
                        <div class="form-text">
                            Password requirements: min. ${pwMinLength} alphanumeric characters,
                            no whitespace or special characters.
                        </div>
                    </div>
                </div>

                <div class="col-12">
                    <button id="submitChangePassword" type="submit" class="btn btn-primary">Change Password</button>
                </div>

                <script>
                    const passwordInput = document.getElementById('updatedPassword');
                    const confirmPasswordInput = document.getElementById('repeatedPassword');
                    const confirmPasswordFeedback = document.getElementById('confirm-password-feedback');

                    const validateConfirmPassword = function () {
                        if (passwordInput.value === confirmPasswordInput.value) {
                            confirmPasswordInput.setCustomValidity('');
                            confirmPasswordFeedback.innerText = '';
                        } else {
                            confirmPasswordInput.setCustomValidity('password-mismatch');
                            confirmPasswordFeedback.innerText = "Passwords don't match.";
                        }
                    };

                    passwordInput.addEventListener('input', validateConfirmPassword);
                    confirmPasswordInput.addEventListener('input', validateConfirmPassword);
                </script>
            </form>
        </section>

        <section class="mt-5" aria-labelledby="delete-account">
            <h2 class="mb-3" id="delete-account">Account Deletion</h2>

            <p>Delete all personalized information related to your account.</p>
            <button type="button" class="btn btn-danger" data-bs-toggle="modal" data-bs-target="#account-deletion-modal">
                Delete Account
            </button>

            <div class="modal" id="account-deletion-modal" tabindex="-1">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <form action="${url.forPath(Paths.USER_SETTINGS)}" method="post">
                            <input type="hidden" class="form-control" name="formType" value="deleteAccount">

                            <div class="modal-header">
                                <h3 class="modal-title">Confirm Account Deletion</h3>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <p><b>This cannot be undone.</b></p>
                                <p class="mb-0">
                                    We will delete all personalized information related to your account.
                                    Please be aware that you will no longer be able to log in again.
                                    To play further games, you will have to create a new account.
                                </p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                <button type="submit" class="btn btn-danger">Delete Account</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </section>

    </div>
</p:main_page>
