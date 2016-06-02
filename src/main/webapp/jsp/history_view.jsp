<% String pageTitle="Game History"; %>
<%@ include file="/jsp/header.jsp" %>

<<<<<<< HEAD
=======
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- Title -->
	<title>Code Defenders - History</title>

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

<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.User" %>
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
				<li><a href="games/open">Open Games</a></li>
				<li><a href="games/create">Create Game</a></li>
				<li class="active"><a href="games/history">History</a></li>
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

<h2> View Past Games </h2>
>>>>>>> 37098c05c265c5f6f462ceae5c51150a1695cb37
<table class="table table-hover table-responsive table-paragraphs">
	<tr>
		<td class="col-sm-2">Game No.</td>
		<td class="col-sm-2">Attacker</td>
		<td class="col-sm-2">Defender</td>
		<td class="col-sm-2">Class Under Test</td>
		<td class="col-sm-2">Level</td>
		<td class="col-sm-2"></td>
	</tr>


	<%
		boolean isGames = false;
		String atkName;
		String defName;
		int uid = (Integer)request.getSession().getAttribute("uid");
		int atkId;
		int defId;
		for (Game g : DatabaseAccess.getHistoryForUser(uid)) {
			atkId = g.getAttackerId();
			defId = g.getDefenderId();
			User attacker = DatabaseAccess.getUserForKey("User_ID", atkId);
			User defender = DatabaseAccess.getUserForKey("User_ID", defId);
			atkName = attacker == null ? "-" : attacker.username;
			defName = defender == null ? "-" : defender.username;
	%>

	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-2"><%= DatabaseAccess.getClassForKey("Class_ID", g.getClassId()).name %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
		<td class="col-sm-2">
			<form id="view" action="games" method="post">
				<input type="hidden" name="formType" value="enterGame">
				<input type="hidden" name="game" value=<%=g.getId()%>>
				<input type="submit" class="btn btn-default" value="View Scores">
			</form>
		</td>
	</tr>

	<%
		}
		if (!isGames) {%>
	<p> There are currently no games in your history </p>
	<%}
	%>
</table>

<<<<<<< HEAD
<%@ include file="/jsp/footer.jsp" %>
=======
</body>
</html>
>>>>>>> 37098c05c265c5f6f462ceae5c51150a1695cb37
