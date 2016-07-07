<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - Login</title>

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

	<!-- jQuery -->
	<script src="js/jquery.min.js" type="text/javascript" ></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript" ></script>
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet" type="text/css" />

</head>

<body>

  <%@ page import="java.util.*" %>
	<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
		    <div class="navbar-header">
			    <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1" aria-expanded="false">
			    </button>
			    <a class="navbar-brand" href="/">
				    <span><img class="logo" href="/" src="images/logo.png"/></span>
				    Code Defenders
			    </a>
		    </div>
   		</div>
	</nav>

  <%
	  Object uid = request.getSession().getAttribute("uid");
	  Object username = request.getSession().getAttribute("username");
	  if (uid != null && username != null)
		  response.sendRedirect("games");
  %>
  <% 
      ArrayList<String> messages = (ArrayList<String>) request.getAttribute("messages");
      if (messages != null) {
        for (String m : messages) { %>
          <div class="alert alert-info">
              <strong><%=m%></strong>
          </div>
        <% }
      }
  %>


  <div id="login" class="container">
      <form  action="login" method="post" class="form-signin">
          <input type="hidden" name="formType" value="login">
          <h2 class="form-signin-heading">Sign in</h2>
          <label for="inputUsername" class="sr-only">Username</label>
          <input type="text" id="inputUsername" name="username" class="form-control" placeholder="Username" required autofocus>
          <label for="inputPassword" class="sr-only">Password</label>
          <input type="password" id="inputPassword" name="password" class="form-control" placeholder="Password" required>
          <div>
              <input type="checkbox" id="consentOK" style="margin-right:5px;">I understand and consent that the mutants and tests I create in the game will be used for research purposes.
          </div>
          <button class="btn btn-lg btn-primary btn-block" id="signInButton" type="submit" disabled>Sign in</button>
          <a href="#" class="text-center new-account" data-toggle="modal" data-target="#myModal">Create an account</a>
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
                      <label for="inputPassword" class="sr-only">Password</label>
                      <input type="password" id="inputPasswordCreate" name="password" class="form-control" placeholder="Password" required>
                      <label for="inputPassword" class="sr-only">Password</label>
                      <input type="password" id="inputConfirmPassword" name="confirm" class="form-control" placeholder="Confirm Password" required>
                      <button class="btn btn-lg btn-primary btn-block" type="submit">Create Account</button>
                   </form>
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
</body>
</html>
