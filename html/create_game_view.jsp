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

  <%@ page import="gammut.*,java.io.*" %>
	<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
    		<div class="navbar-header">
      			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
      			</button>
    		</div>
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav">
                <li><a class="navbar-brand" href="/gammut/games">GamMut</a></li>
                <li><a href="/gammut/games/user">My Games</a></li>
                <li><a href="/gammut/games/open">Open Games</a></li>
                <li class="active"><a href="/gammut/games/create">Create Game</a></li>
                <li><a href="/gammut/games/history">History</a></li>
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

  <form id="logout" action="/gammut/login" method="post">
    <input type="hidden" name="formType" value="logOut">
  </form>

  <h2> Create a Game </h2>

  <form id="create" action="/gammut/games" method="post">
    <input type="hidden" name="formType" value="createGame">
    Select the Class you will play
    <select name="class">
      <option value="1">Book</option>
      <option value="2">Dog</option>
    </select> <br>
    <input type="radio" name="role" value="Attacker_ID">Play as Attacker<br>
    <input type="radio" name="role" value="Defender_ID">Play as Defender<br>
    <input type="number" name="rounds" min="1" max="10">Number of Rounds<br>
    <input type="submit" value="Create">
  </form>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>