<!DOCTYPE html>
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

    <script src="codemirror/lib/codemirror.js"></script>
    <script src="codemirror/mode/javascript/javascript.js"></script>
    <link href="codemirror/lib/codemirror.css" rel="stylesheet" >

</head>

<body>
	<%@ page import="org.gammut.*,java.io.*, java.util.*" %>
	<% Game game = (Game) session.getAttribute("game"); %>

		<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
    		<div class="navbar-header">
      			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
      			</button>
    		</div>
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav navbar-left">
            		<a class="navbar-brand" href="games">GamMut</a>
            		<li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
            		<li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
            		<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants are Alive</li>
          		</ul>
          		<ul class="nav navbar-nav navbar-right">
          			<% if (game.getActivePlayer().equals("ATTACKER")) {%> <button type="submit" class="btn btn-default navbar-btn" form="atk">Attack!</button><%}%>
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

	<div id="info">

		<h2> Mutants </h2>
	    <table class="table table-hover table-responsive table-paragraphs">

		<%
		boolean isMutants = false;
		for (Mutant m : game.getAliveMutants()) {
			isMutants = true;
		%>

			<tr>
				<td class="col-sm-2"><%= "Mutant" %></td>
				<td class="col-sm-1"><% if (m.isAlive()) {%><%="Alive"%><%} else {%><%="Dead"%><%} %></td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="2"><%
					for (String change : m.getHTMLReadout()) {
						%><p><%=change%><p><%
					}
				%></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="2"></td>
			</tr>

		<%
		}
		if (!isMutants) {%>
			<p> There are currently no mutants </p>
		<%}
		%>
		</table>

		<h2> Tests </h2>
		<table class="table table-hover table-responsive table-paragraphs">

		<%
		boolean isTests = false;
		int count = 1;
		for (Test t : game.getTests()) {
			isTests = true;
		%>

			<tr>
				<td class="col-sm-2"><%= "No: " + count %></td>
				<td class="col-sm-1"><%= "yes" %></td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="2"><%
					for (String line : t.getHTMLReadout()) {
						%><p><%=line%><p><%
					}
				%></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="2"></td>
			</tr>

		<%
			count++;
		}
		if (!isTests) {%>
			<p> There are currently no tests </p>
		<%}
		%>
		</table>
	</div>

    <div id="right">
	    <form id="atk" action="play" method="post">

	    	<%
			    InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/data/sources/"+game.getClassName()+".java");
			    String line;
			    String source = "";
			    BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
			    while((line = is.readLine()) != null) {source+=line+"\n";}
			%>

			<input type="hidden" name="formType" value="createMutant">
			<textarea id="code" name="mutant" cols="90" rows="50"><%=source%></textarea>
		    <br>

	    </form>
	</div>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <script>
	    var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
	    lineNumbers: true,
	    matchBrackets: true
		});
		editor.setSize("100%", 575);
	</script>
</body>
</html>
