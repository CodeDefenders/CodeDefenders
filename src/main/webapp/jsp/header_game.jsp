<!DOCTYPE html>
<html>

<head>
    <title>Code Defenders - Attack</title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Slick -->
    <link rel="stylesheet" type="text/css" href="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.css"/>
    <script type="text/javascript" src="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.min.js"></script>


    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/gamestyle.css" rel="stylesheet">

    <script src="codemirror/lib/codemirror.js"></script>
    <script src="codemirror/mode/javascript/javascript.js"></script>
    <script src="codemirror/mode/diff/diff.js"></script>
    <link href="codemirror/lib/codemirror.css" rel="stylesheet">


    <script>
        $(document).ready(function() {
            $('.single-item').slick({
                arrows: true,
                infinite: true,
                speed: 300,
                draggable:false
            });
            $('#messages-div').delay(10000).fadeOut();
        });
    </script>
</head>

<body>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.GameClass" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
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
            <ul class="nav navbar-nav navbar-left">
                <li><a href="games/user">My Games</a></li>
                <li class="navbar-text">Game ID: <%= game.getId() %></li>
                <li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
                <li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
                <% if (game.getAliveMutants().size() == 1) {%>
                <li class="navbar-text">1 Mutant Alive</li>
                <% } else {%>
                <li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants Alive</li>
                <% }%>
                <li class="navbar-text">
                    <% if (game.getState().equals(ACTIVE) && game.getActiveRole().equals(Game.Role.ATTACKER)) {%>
                    <span class="label label-primary turn-badge">Your turn</span>
                    <% } else { %>
                    <span class="label label-default turn-badge">Waiting</span>
                    <% } %>
                </li>
            </ul>
            <ul class="nav navbar-nav navbar-right">
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

    <%
	ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
	request.getSession().removeAttribute("messages");
	if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <% } %>
</div>
<%	} %>