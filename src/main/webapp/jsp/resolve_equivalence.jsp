<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
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
	<link href="codemirror/lib/codemirror.css" rel="stylesheet">

	<script>
		$(document).ready(function() {
			$('.single-item').slick({
				arrows: true,
				infinite: true,
				speed: 300
			});
		});
	</script>
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
            		<li class="navbar-text">Round <%= game.getCurrentRound() %></li>
            		<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants are Alive</li>
          		</ul>
          		<ul class="nav navbar-nav navbar-right">
          			<!--<button type="submit" class="btn btn-default navbar-btn" form="equivalence">Resolve</button>-->
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

	<div id="info" class="col-md-6">

		<h2>Equivalent Mutant?</h2>
		<table class="table table-hover table-responsive table-paragraphs">

		<%

		for (Mutant m : game.getAliveMutants()) { 
			if (m.getEquivalent().equals("PENDING_TEST") && m.isAlive()) {
		%>

			<tr>
				<td class="col-sm-1"><%= "Mutant" %></td>
			</tr>
			<tr>
				<td class="col-sm-1" colspan="3"><%= m.getHTMLReadout() %></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="3"></td>
			</tr>

		<%	
			break;
			}
		}
		%>
		</table>

		<h2> Tests </h2>
		<div class="slider single-item">
		<% 
		boolean isTests = false;
		int count = 1;
		for (Test t : game.getTests()) { 
			isTests = true;
			String tc = "";
			for (String line : t.getHTMLReadout()) { tc += line + "\n"; }
		%>
			<div><h4>Test <%=count%></h4><pre><textarea id=<%="tc"+count%> name="utest" class="utest" cols="20" rows="10"><%=tc%></textarea></pre></div>
		<%
			count++;
		} 
		if (!isTests) {%>
			<div><h2></h2><p> There are currently no tests </p></div>
		<%}
		%>
		</div> <!-- slider single-item -->

		<h2> Source Code </h2>
		<%
	    InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/data/sources/"+game.getClassName()+".java");
	    String line;
	    String source = "";
	    BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
	    while((line = is.readLine()) != null) {source+=line+"\n";}
		%>
		<input type="hidden" name="formType" value="createMutant">
		<pre><textarea class="utest" cols="80" rows="50"><%=source%></textarea></pre>
	</div>  <!-- col-md6 left -->

	<div id="code" class="col-md-6">
		<form id="equivalence" action="play" method="post">
			<input type="hidden" name="formType" value="resolveEquivalence">
			<input class="btn btn-default" name="acceptEquivalent" type="submit" value="Accept Equivalent">
			<input class="btn btn-default turn" name="aejectEquivalent" type="submit" value="Submit Killing Test">
			<h2>Not Equivalent? Write a killing test here:</h2>
	        	<pre><textarea id="newtest" name="test" cols="90" rows="30">
import org.junit.*;
import static org.junit.Assert.*;

public class Test<%=game.getClassName()%> {
@Test
public void test() {

}
}</textarea></pre>
	    </form>
	</div>  <!-- col-md6 right -->

<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="js/bootstrap.min.js"></script>
<script>
	var editor = CodeMirror.fromTextArea(document.getElementById("newtest"), {
		lineNumbers: true,
		matchBrackets: true
	});
	editor.setSize("100%", 575);

	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
			readOnly: true
		});
	}
</script>
</body>
</html>
