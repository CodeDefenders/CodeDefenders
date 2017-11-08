<% String pageTitle = "Login"; %>

<%@ include file="/jsp/header_logout.jsp" %>

  <%
	  Object uid = request.getSession().getAttribute("uid");
	  Object username = request.getSession().getAttribute("username");
	  if (uid != null && username != null)
		  response.sendRedirect("games");

	  String email = request.getParameter("email");
	  String reference = request.getParameter("reference");
  %>


  <div id="login" class="container">
      <div class="modal-body">
          <div id="create">
              <form  action="login" method="post" class="form-signin">
                  <input type="hidden" name="formType" value="resetPassword">
                  <input type="hidden" name="reference"
                         value="<%= reference %>">
                  <label for="inputEmail" class="sr-only">Email</label>
                  <input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required autofocus
                  value="<%= email %>" />
                  <label for="inputPasswordCreate" class="sr-only">Password
                  </label>
                  <input type="password" id="inputPasswordCreate" name="password" class="form-control" placeholder="Password" required>
                  <label for="inputConfirmPassword"
                         class="sr-only">Confirm Password
                  </label>
                  <input type="password" id="inputConfirmPassword" name="confirm" class="form-control" placeholder="Confirm Password" required>
                  <button class="btn btn-lg btn-primary btn-block" type="submit">Reset Password</button>
              </form>
              <span style="margin-right:5px; font-size:small;">Valid password: 3-20 characters.</span>
          </div>
      </div>
  </div>