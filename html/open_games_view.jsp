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
                <li class="active"><a href="/gammut/games/open">Open Games</a></li>
                <li><a href="/gammut/games/create">Create Game</a></li>
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

  <h2> Open Games </h2>
    <table class="table table-hover table-responsive table-paragraphs">
      <tr>
        <td class="col-sm-2">Game No.</td>
        <td class="col-sm-2">Attacker</td>
        <td class="col-sm-2">Defender</td>
        <td class="col-sm-2">Game State</td>
        <td class="col-sm-2">Class Tested</td>
        <td class="col-sm-2"></td>
      </tr>


    <% 
    boolean isGames = false;
    String atkName;
    String defName;
    int uid = (Integer)request.getSession().getAttribute("uid");
    int atkId;
    int defId;
    for (Game g : GameSelectionManager.getAllGames()) { 
      isGames = true;

      atkId = g.getAttackerId();
      defId = g.getDefenderId();

      if ((atkId == uid)||(defId == uid)) {continue;}
      
      atkName = GameSelectionManager.getNameForUser(atkId);
      defName = GameSelectionManager.getNameForUser(defId);

      if ((atkName != null)&&(defName != null)) {continue;}

      if (atkName == null) {atkName = "Empty";}
      if (defName == null) {defName = "Empty";}
    %>

      <tr>
        <td class="col-sm-2"><%= g.getId() %></td>
        <td class="col-sm-2"><%= atkName %></td>
        <td class="col-sm-2"><%= defName %></td>
        <td class="col-sm-2"><%= g.getState() %></td>
        <td class="col-sm-2"><%= GameSelectionManager.getNameForClass(g.getClassId()) %></td>
        <td class="col-sm-2">
          <form id="view" action="/gammut/games" method="post">
            <input type="hidden" name="formType" value="joinGame">
            <input type="hidden" name="game" value=<%=g.getId()%>>
            <input type="submit" value="Join Game">
          </form>
        </td>
      </tr>

    <%
    } 
    if (!isGames) {%>
      <p> There are currently no open games </p>
    <%}
    %>
    </table>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>