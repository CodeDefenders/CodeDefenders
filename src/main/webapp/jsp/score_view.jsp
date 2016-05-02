<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - Score Board</title>

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- jQuery -->
	<script src="js/jquery.min.js" type="text/javascript" ></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript" ></script>
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet" type="text/css" />

	<title>Score Window</title>
</head>

<body>

	<%@ page import="org.codedefenders.Game" %>
	<% Game game = (Game) session.getAttribute("game"); %>

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
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav">
		            <li><a href="games/user">My Games</a></li>
          		</ul>
      		</div>
   		</div>
	</nav>



	<div id="splash">
		<h1> TBD </h1>
	</div>

</body>
</html>
