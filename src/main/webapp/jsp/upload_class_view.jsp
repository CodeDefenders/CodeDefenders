<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.0.0/jquery.min.js"></script>

	<!-- Bootstrap -->
	<script src="js/bootstrap.min.js" type="text/javascript"></script>
	<link href="css/bootstrap.min.css" rel="stylesheet">

	<!-- Bootstrap plugins -->
	<!-- toggle -->
	<link href="https://gitcdn.github.io/bootstrap-toggle/2.2.0/css/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="https://gitcdn.github.io/bootstrap-toggle/2.2.0/js/bootstrap-toggle.min.js"></script>
	<!-- select -->
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.9.3/css/bootstrap-select.min.css">
	<script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.9.3/js/bootstrap-select.min.js"></script>
	<!-- fileinput -->
	<script src="js/fileinput.min.js" type="text/javascript"></script>
	<link href="css/fileinput.min.css" media="all" rel="stylesheet" type="text/css" />

	<!-- Game -->
	<link href="css/gamestyle.css" rel="stylesheet">

	<script>
		$(document).on('ready', function() {
			$("#fileUpload").fileinput({showCaption: false});
		});
	</script>
</head>

<body>

<%@ page import="org.codedefenders.*,java.io.*" %>
<nav class="navbar navbar-inverse navbar-fixed-top">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
			</button>
		</div>
		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
			<ul class="nav navbar-nav">
				<li><a class="navbar-brand" href="games">Code Defenders</a></li>
				<li><a href="games/user">My Games</a></li>
				<li><a href="games/open">Open Games</a></li>
				<li class="active"><a href="games/create">Create Game</a></li>
				<li><a href="games/history">History</a></li>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<li></li>
				<li>
					<p class="navbar-text">
						<span class="glyphicon glyphicon-user" aria-hidden="true"></span>
						<%=request.getSession().getAttribute("username")%>
					</p>
				</li>
				<li><input type="submit" form="logout" class="btn btn-inverse navbar-btn" value="Log Out"/></li>
			</ul>
		</div>
	</div>
</nav>

<form id="logout" action="login" method="post">
	<input type="hidden" name="formType" value="logOut">
</form>

<div id="divUpload" class="container">
	<form id="formUpload" action="/upload" class="form-upload" method="post" enctype="multipart/form-data">
		<h2>Upload CUT</h2>
		<input id="fileUpload" name="fileUpload" type="file" class="file-loading" data-allowed-file-extensions='["java"]' data-show-preview="false" data-placeholder="No file" data-show-upload="true" data-show-remove="false" data-show-caption="true" data-buttonText="Find CUT">
	</form>
</div>
</body>
</html>
