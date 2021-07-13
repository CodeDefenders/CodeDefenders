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
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("My Profile"); %>

<jsp:include page="/jsp/header.jsp"/>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
%>

<div class="container form-width">
    <h2>${pageInfo.pageTitle}</h2>

    <h3 class="mt-4 mb-3">Played games</h3>
    <p>
        You can find a list of your past games in the
        <a href="<%=request.getContextPath() + Paths.GAMES_HISTORY%>">games history</a>.
    </p>

    <h3 class="mt-4 mb-3">Account Information</h3>

    <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post" class="row g-3 needs-validation" autocomplete="off">
        <input type="hidden" class="form-control" name="formType" value="updateProfile">

        <div class="col-12">
            <div class="mb-2">
                <label for="updatedEmail" class="form-label">Email</label>
                <input type="email" class="form-control" id="updatedEmail" name="updatedEmail"
                       value="<%=login.getUser().getEmail()%>" placeholder="Email" required>
            </div>

            <div class="form-check">
                <input class="form-check-input" type="checkbox" id="allowContact" name="allowContact"
                    <%=login.getUser().getAllowContact() ? "checked" : ""%>>
                <label class="form-check-label" for="allowContact">
                    Allow us to contact your email address.
                </label>
            </div>
        </div>

        <div class="col-12">
            <div class="mb-2">
                <label for="updatedPassword" class="form-label">Password</label>
                <input type="password" class="form-control" id="updatedPassword"
                       name="updatedPassword" placeholder="Password (leave empty for unchanged)"
                       minlength="<%=pwMinLength%>" maxlength="20" pattern="[a-zA-Z0-9]*">
                <div class="invalid-feedback">
                    Please enter a valid password.
                </div>
            </div>

            <div>
                <input type="password" class="form-control" id="repeatedPassword"
                       name="repeatedPassword" placeholder="Confirm Password">
                <div class="invalid-feedback" id="confirm-password-feedback">
                    Please confirm your password.
                </div>
                <div class="form-text">
                    <%=pwMinLength%>-20 alphanumeric characters, no whitespace or special characters.
                    <br>
                    Leave empty to keep unchanged.
                </div>
            </div>
        </div>

        <div class="col-12">
            <button id="submitUpdateProfile" type="submit" class="btn btn-primary">Update Profile</button>
        </div>

        <script>
            $(document).ready(() => {
                const passwordInput = document.getElementById('updatedPassword');
                const confirmPasswordInput = document.getElementById('repeatedPassword');
                const confirmPasswordFeedback = document.getElementById('confirm-password-feedback');

                const validateConfirmPassword = function () {
                    if (passwordInput.value === confirmPasswordInput.value)  {
                        confirmPasswordInput.setCustomValidity('');
                        confirmPasswordFeedback.innerText = '';
                    } else {
                        confirmPasswordInput.setCustomValidity('password-mismatch');
                        confirmPasswordFeedback.innerText = "Passwords don't match.";
                    }
                };

                passwordInput.addEventListener('input', validateConfirmPassword);
                confirmPasswordInput.addEventListener('input', validateConfirmPassword);
            });
        </script>
    </form>

    <h3 class="mt-4 mb-3">Account Deletion</h3>

    <button type="button" class="btn btn-danger" data-bs-toggle="modal" data-bs-target="#account-deletion-modal">
        Delete Account
    </button>

    <div class="modal" id="account-deletion-modal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post">
                    <input type="hidden" class="form-control" name="formType" value="deleteAccount">

                    <div class="modal-header">
                        <h5 class="modal-title">Account Deletion</h5>
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

</div>

<%@ include file="/jsp/footer.jsp" %>
