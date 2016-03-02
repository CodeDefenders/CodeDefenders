<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>

<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/gamestyle.css" rel="stylesheet">

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
	<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
	<!-- Include all compiled plugins (below), or include individual files as needed -->
	<script src="js/bootstrap.min.js"></script>

	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
</head>

<body>

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

	<div class="container">
		<div id="splash" class="jumbotron">
			<h1>Code Defenders</h1>
			<p>A Mutation Testing Game</p>
			<p>
			<a  class="btn btn-primary btn-large"  href="login">Enter</a>
			<p><img src="images/schema.jpeg" class="img-responsive displayed"/></p>
			<p class="text-muted"  style="font-size:small;">Developed at The University of Sheffield</p>
			<a class="text-muted" href="contact">Contact Us</a>
			<a class="text-muted" href="intro.html">Intro</a>
		</div>
	</div>
</body>
</html>
