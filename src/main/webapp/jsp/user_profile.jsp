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
<%@ page import="org.codedefenders.model.UserInfo" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<% String pageTitle=null; %>

<%@ include file="/jsp/header_main.jsp" %>
<%
	final UserInfo userInfo = ((UserInfo) request.getAttribute("userProfileInfo"));
    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
%>
<div>
    <div id="user-information-container">
        <h2>Profile Information</h2>

        <h4>Played games</h4>
        <p>
            You can look at your <a href="<%=request.getContextPath() + Paths.GAMES_HISTORY%>">games history</a>.
        </p>

        <%
            String privacyNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.PRIVACY_NOTICE).getStringValue();
            if(!privacyNotice.isEmpty()) {
        %>

        <h4>Privacy Notice</h4>
        <p>
            <%=privacyNotice%>
        </p>
        <%
          }
        %>

    </div>

    <div id="update-profile-container">
        <h2>Update Profile</h2>
        <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post">
            <input type="hidden" class="form-control" name="formType" value="updateProfile">

            <div>
                <h4>Update E-Mail Preferences</h4>
                <div class="form-group row">
                    <label for="updatedEmail" class="col-sm-2 col-form-label">E-Mail</label>
                    <div class="col-sm-10">
                        <input type="email" class="form-control" id="updatedEmail" name="updatedEmail" placeholder="<%=userInfo.getUser().getEmail()%>">
                    </div>
                </div>
                <div class="form-group row">
                    <input class="form-check-input" type="checkbox" id="allowContact" name="allowContact" value="true"
                        <%=userInfo.getUser().getAllowContact() ? "checked" : ""%>>
                    <label class="form-check-label" for="allowContact">
                        Allow us to contact your email address.
                    </label>
                </div>
            </div>

            <div>
                <h4>Update Password</h4>
                <div class="form-group row">
                    <label for="updatedPassword" class="col-sm-2 col-form-label">New Password</label>
                    <div class="col-sm-10">
                        <input onkeyup="validatePassword()" type="password" class="form-control" id="updatedPassword"
                               name="updatedPassword" placeholder="Password" minlength="<%=pwMinLength%>">
                    </div>
                </div>
                <div class="form-group row">
                    <label for="repeatedPassword" class="col-sm-2 col-form-label">Repeat Password</label>
                    <div class="col-sm-10">
                        <input onkeyup="validatePassword()" type="password" class="form-control" id="repeatedPassword"
                               name="repeatedPassword" placeholder="Password">
                    </div>
                </div>
                <span class="label label-danger" id="pw_confirm_message" style="color: white;visibility: hidden">Passwords do not match!</span>

                <script type="text/javascript">
                    function validatePassword() {
                        if(document.getElementById('updatedPassword').value === '') {
                            document.getElementById('pw_confirm_message').style.visibility = "hidden";
                            document.getElementById('submitUpdateProfile').disabled = false;
                            return;
                        }

                        if (document.getElementById('updatedPassword').value === document.getElementById('repeatedPassword').value) {
                            document.getElementById('pw_confirm_message').style.visibility = "hidden";
                            document.getElementById('submitUpdateProfile').disabled = false;
                        } else {
                            document.getElementById('pw_confirm_message').style.visibility = "visible";
                            document.getElementById('submitUpdateProfile').disabled = true;
                        }
                    }
                </script>
            </div>

            <div class="form-group row">
                <div class="col-sm-10">
                    <button id="submitUpdateProfile" type="submit" class="btn btn-primary">Update Profile</button>
                </div>
            </div>
        </form>
    </div>

    <div>
        <h3>Delete your account</h3>

        <div class="panel panel-default">
            <div class="panel-heading">
                <button class="btn btn-primary" type="button" data-toggle="collapse" data-target="#delete-user-container">
                    View Account Deletion
                </button>
            </div>
            <div class="collapse" id="delete-user-container">
                <div class="panel-body">
                    <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post">
                        <p>
                            This action cannot be reversed.
                            <br>
                            We will delete all personalized information related to your account.
                            Please be aware that you will no longer be able to log in again.
                            To play further games, you'd have to create a new account.
                        </p>
                        <input type="hidden" class="form-control" name="formType" value="deleteAccount">
                        <div class="form-group row">
                            <div class="col-sm-10">
                                <button type="submit" class="btn btn-danger">Delete Account</button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

</div>

<%@ include file="/jsp/footer.jsp" %>
