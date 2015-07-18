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
</head>
<body>

	<%@ page import="gammut.*,java.io.*" %>
	<% GameState gs = (GameState) getServletContext().getAttribute("gammut.gamestate"); %>

	<nav class="navbar navbar-inverse navbar-fixed-top">
  		<div class="container-fluid">
    		<div class="navbar-header">
      			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
      			</button>
    		</div>
      		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
          		<ul class="nav navbar-nav navbar-left">
            		<a class="navbar-brand" href="/gammut/intro">GamMut</a>
            		<li class="navbar-text">ATK: <%= gs.getScore(0) %> | DEF: <%= gs.getScore(1) %></li>
            		<li class="navbar-text">Round <%= gs.getRound() %></li>
            		<li class="navbar-text"><%= gs.getAliveMutants().size() %> Mutants are Alive</li>
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
		for (Mutant m : gs.getAliveMutants()) { 
			isMutants = true;
		%>

			<tr>
				<td class="col-sm-2"><%= "Greg" %></td>
				<td class="col-sm-1"><%= "yes" %></td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="2"><%= m.getHTMLReadout() %></td>
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
		for (Test t : gs.getTests()) { 
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
	    InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/"+gs.getClassName()+".java");
	    String line;
	    String source = "";
	    BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
	    while((line = is.readLine()) != null) {source+=line+"<br>";}
		%>
		<code><%=source%></code>

	</div>

	<div id="code">
		<form id="def" action="/gammut/defender" method="post">

			<input type="hidden" name="user" value="1">
	        <textarea name="test" cols="90" rows="30">
import org.junit.*;
import static org.junit.Assert.*;

public class Test<%=gs.getClassName()%> {
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
    <script src="js/bootstrap.min.js"></script>
</body>
</html>