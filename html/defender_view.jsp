<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- Bootstrap -->
    <link href="${pageContext.request.contextPath}/html/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/html/css/gamestyle.css" rel="stylesheet">

    <script src="${pageContext.request.contextPath}/html/codemirror/lib/codemirror.js"></script>
    <script src="${pageContext.request.contextPath}/html/codemirror/mode/javascript/javascript.js"></script>
    <link href="${pageContext.request.contextPath}/html/codemirror/lib/codemirror.css" rel="stylesheet" >

</head>
<body>

	<%@ page import="gammut.*,java.io.*" %>
	<% Game game = (Game) session.getAttribute("game"); %>

	<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
    		<div class="navbar-header">
      			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
      			</button>
    		</div>
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav navbar-left">
            		<a class="navbar-brand" href="/gammut/games">GamMut</a>
            		<li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
            		<li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
            		<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants are Alive</li>
          		</ul>
          		<ul class="nav navbar-nav navbar-right">
          			<button type="submit" class="btn btn-default navbar-btn" form="def">Defend!</button>
          		</ul>
      		</div>
   		</div>
	</nav>

	<div id="info">

		<h2> Mutants </h2>
	    <table class="table table-hover table-responsive table-paragraphs">

		<% 
		boolean isMutants = false;
		int count = 0;
		for (Mutant m : game.getAliveMutants()) { 
			isMutants = true;
		%>

			<tr>
				<td class="col-sm-1"><%= "Greg" %></td>
				<td class="col-sm-1"><% if (m.isAlive()) {%><%="Alive"%><%} else {%><%="Dead"%><%} %></td>
				<td class="col-sm-1">
					Mark as Equivalent: <input type="checkbox" form="def" name="mutant<%=count%>" value="equivalent" <% if (m.isEquivalent()) {%><%="checked"%><%}%>>
				</td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="3"></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="3"></td>
			</tr>

		<%
			count++;
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
		for (Test t : game.getTests()) { 
			isTests = true;
		%>

			<tr>
				<td class="col-sm-2"><%= "Greg" %></td>
				<td class="col-sm-1"><%= "yes" %></td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="2"><%= t.getHTMLReadout() %></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="2"></td>
			</tr>

		<%
		} 
		if (!isTests) {%>
			<p> There are currently no tests </p>
		<%}
		%>
		</table>

		<h2> Source Code </h2>
		<%
	    InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/sources/"+game.getClassName()+".java");
	    String line;
	    String source = "";
	    BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
	    while((line = is.readLine()) != null) {source+=line+"<br>";}
		%>
		<pre><code><%=source%></code></pre>

	</div>

	<div id="right">
		<form id="def" action="/gammut/play" method="post">

			<input type="hidden" name="formType" value="createTest">
	        <textarea id="code" name="test" cols="90" rows="30">
import org.junit.*;
import static org.junit.Assert.*;

public class Test<%=game.getClassName()%> {
@Test
public void test() {

}
}</textarea>

		    <br>
	    </form>
	</div>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="${pageContext.request.contextPath}/html/js/bootstrap.min.js"></script>
    <script> 
	    var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
	    lineNumbers: true,
	    matchBrackets: true
		}); 
	</script>
</body>
</html>