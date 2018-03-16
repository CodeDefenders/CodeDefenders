<%@ page import="org.codedefenders.util.AdminDAO" %>
<%@ page import="org.codedefenders.*" %>
<% String pageTitle = "Login"; %>

<%@ include file="/jsp/header_logout.jsp" %>

  <%
	  Object uid = request.getSession().getAttribute("uid");
	  Object username = request.getSession().getAttribute("username");
	  if (uid != null && username != null)
		  response.sendRedirect(request.getContextPath()+"/games");
  %>


  <div id="login" class="container">
      <form  action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
          <input type="hidden" name="formType" value="login">
          <h2 class="form-signin-heading">Sign in</h2>
          <label for="inputUsername" class="sr-only">Username/Email</label>
          <input type="text" id="inputUsername" name="username" class="form-control" placeholder="Username or Email" required autofocus>
          <label for="inputPassword" class="sr-only">Password</label>
          <input type="password" id="inputPassword" name="password" class="form-control" placeholder="Password" required>
          <div>
              <input type="checkbox" id="consentOK" style="margin-right:5px;" checked>I understand and consent that the mutants and tests I create in the game will be used for research purposes.
          </div>
          <button class="btn btn-lg btn-primary btn-block" id="signInButton" type="submit">Sign in</button>

          <%if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REGISTRATION).getBoolValue()) { %>
          <a href="#" class="text-center new-account" data-toggle="modal" data-target="#myModal">Create an account</a>
          <%}%>

          <%if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue()) {
          // a newly generated password can only be sent to the user if mails are enabled%>
          <a href="#" class="text-center new-account" data-toggle="modal" data-target="#passwordResetModal"
             style=" float: right;" hidden id="passwordForgotten">Password forgotten</a>
          <%}%>

      </form>
  </div>

<% String resetPw = request.getParameter("resetPW");
    String userId = request.getParameter("user");
    if (resetPw != null && userId != null &&
            DatabaseAccess.checkPasswordResetSecret(Integer.parseInt(userId), resetPw)) {
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
                <a class="close" href = 'login'>&times;</a>
                <h4 class="modal-title">Change your password</h4>
            </div>
            <div class="modal-body">
                <form action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
                    <input type="hidden" name="resetPwSecret" id = "resetPwSecret" value="<%=resetPw%>">
                    <input type="hidden" name="userId" id = "userId" value="<%=userId%>">

                    <input type="hidden" name="formType" value="changePassword">
                    <label for="inputPassword" class="sr-only">Password</label>
                    <input type="password" id="inputPasswordChange" name="inputPasswordChange" class="form-control"
                           onkeyup="check()" placeholder="Password" required minlength="<%=pwMinLength%>">
                    <span class = "label label-danger" style = "color: white" id = "pw_confirm_message"></span>
                    <label for="inputPassword" class="sr-only">Confirm Password</label>
                    <input type="password" id="inputConfirmPasswordChange" name="inputConfirmPasswordChange"
                           onkeyup="check()" class="form-control" placeholder="Confirm Password" required minlength="<%=pwMinLength%>">
                    <button id = "submitChangePassword" disabled class="btn btn-lg btn-primary btn-block"
                            type="submit">Change Password</button>

                    <script>
                        function check() {
                            if (document.getElementById('inputPasswordChange').value ===
                                document.getElementById('inputConfirmPasswordChange').value) {
                                document.getElementById('pw_confirm_message').innerHTML = '';
                                document.getElementById('submitChangePassword').disabled = false;
                            } else {
                                document.getElementById('pw_confirm_message').innerHTML = 'Passwords don\'t match!';
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
                    <input type="text" id="accountUsername" name="accountUsername" class="form-control" placeholder="Username" required autofocus>
                    <label for="inputEmail" class="sr-only">Email</label>
                    <input type="email" id="accountEmail" name="accountEmail" class="form-control" placeholder="Email" required>
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
  <div id="myModal" class="modal fade" role="dialog">
      <div class="modal-dialog">
          <!-- Modal content-->
          <div class="modal-content">
              <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal">&times;</button>
                  <h4 class="modal-title">Create new account</h4>
              </div>
              <div class="modal-body">
	            <div id="create">
                    <form  action="<%=request.getContextPath() %>/login" method="post" class="form-signin">
                      <input type="hidden" name="formType" value="create">
                      <label for="inputUsername" class="sr-only">Username</label>
                      <input type="text" id="inputUsernameCreate" name="username" class="form-control" placeholder="Username" required autofocus>
                      <label for="inputEmail" class="sr-only">Email</label>
                      <input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
                      <label for="inputPassword" class="sr-only">Password</label>
                      <input type="password" id="inputPasswordCreate" name="password" class="form-control" placeholder="Password" required>
                      <label for="inputPassword" class="sr-only">Password</label>
                      <input type="password" id="inputConfirmPassword" name="confirm" class="form-control" placeholder="Confirm Password" required>
                      <button class="btn btn-lg btn-primary btn-block" type="submit">Create Account</button>
                    </form>
                    <span style="margin-right:5px; font-size:small;">Valid username: 3-20 alphanumerics starting with a letter (a-z), no space or special character.<br>
                        Valid password: <%=AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue()%>-20 alphanumeric characters, no whitespace or special character.</span>
                </div>
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

  $(document).ready(function () {
      var messagesDiv = document.getElementById("messages-div");
      if (messagesDiv !== null && messagesDiv.innerText.includes("password was incorrect")) {
          document.getElementById("passwordForgotten").hidden = false;
      }
  });
</script>

<%@ include file="/jsp/footer.jsp" %>
