<html>

<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- Bootstrap -->
    <link href="${pageContext.request.contextPath}/html/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/html/css/gamestyle.css" rel="stylesheet">
</head>

<body>

  <%@ page import="gammut.*,java.io.*, java.util.*" %>
	<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
    		<div class="navbar-header">
      			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
      			</button>
    		</div>
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav">
            		<a class="navbar-brand" href="/gammut/games">GamMut</a>
          		</ul>
      		</div>
   		</div>
	</nav>

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

  <div id="login">
    <h2>Existing Account</h2>
    <form action="/gammut/login" method="post">
      <input type="hidden" name="formType" value="login">
      Username:<input type="text" name="username"><br>
      Password:<input type="password" name="password"><br>
      <input type="submit" value="Login">
    </form>

	<div id="create">
		<h2>Create Account</h2>
    <form action="/gammut/login" method="post">
      <input type="hidden" name="formType" value="create">
      Username:<input type="text" name="username"><br>
      Password:<input type="password" name="password"><br>
      Confirm Password:<input type="password" name="confirm"></br>
      <input type="submit" value="Create Account">
    </form>
	</div>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>