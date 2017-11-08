<% String pageTitle = "Login"; %>

<%@ include file="/jsp/header_logout.jsp" %>

  <%
	  Object uid = request.getSession().getAttribute("uid");
	  Object username = request.getSession().getAttribute("username");
	  if (uid != null && username != null)
		  response.sendRedirect("games");
  %>


  <div id="login" class="container">
      <form  action="login" method="post" class="form-signin">
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
          
          <!--  Alessio disable this to avoid students creating additional users. Ideally this should be configure at build time -->
          <!-- 
          <a href="#" class="text-center new-account" data-toggle="modal" data-target="#myModal">Create an account</a>
           -->
          
          
      </form>
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
                    <form  action="login" method="post" class="form-signin">
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
                    <span style="margin-right:5px; font-size:small;">Valid username: 3-20 alphanumerics starting with a letter (a-z), no space or special character.<br>Valid password: 3-20 alphanumeric characters, no whitespace or special character.</span>
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
        if ($(this).is(':checked')) {
            // if consent checkbox is checked
            document.getElementById("signInButton").disabled = false;
        } else {
            document.getElementById("signInButton").disabled = true;
        }
  });
</script>
