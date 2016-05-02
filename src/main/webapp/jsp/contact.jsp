<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - Contact Us</title>

	<!-- jQuery -->
	<script src="js/jquery.min.js" type="text/javascript" ></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript" ></script>
	<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet" type="text/css" />

	<script>
		$(document).ready(function() {
			$('#messages-div').delay(10000).fadeOut();
		});
	</script>
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
<%
	String result = (String)request.getSession().getAttribute("emailSent");
	request.getSession().removeAttribute("emailSent");
	if (result != null) {
%>
<div class="alert alert-info" id="messages-div">
	<p><%=result%></p>
</div>
<%
	}
%>

<div class="container">
	<form  action="sendEmail" method="post" class="form-signin">
		<input type="hidden" name="formType" value="login">
		<h2 class="form-signin-heading">Contact Us</h2>
		<label for="inputName" class="sr-only">Name</label>
		<input type="text" id="inputName" name="name" class="form-control" placeholder="Name" required autofocus>
		<label for="inputEmail" class="sr-only">Email</label>
		<input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
		<label for="inputSubject" class="sr-only">Subject</label>
		<input type="text" id="inputSubject" name="subject" class="form-control" placeholder="Subject" required autofocus>
		<label for="inputMessage" class="sr-only">Message</label>
		<textarea id="inputMessage" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
		<button class="btn btn-lg btn-primary btn-block" type="submit">Send</button>
	</form>
</div>
</body>
</html>
