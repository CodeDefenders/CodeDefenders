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
          			<% if (game.getActivePlayer().equals("DEFENDER")) {%>
          				<button type="submit" class="btn btn-default navbar-btn" form="equiv">Mark Equivalences</button>
          				<button type="submit" class="btn btn-default navbar-btn" form="def">Defend!</button>
          			<%}%>
          		</ul>
      		</div>
   		</div>
	</nav>

	<% 
      ArrayList<String> messages = (ArrayList<String>) request.getAttribute("messages");
      if (messages != null) {
        for (String m : messages) { %>
          <div class="alert alert-info">
              <pre><strong><%=m%></strong></pre>
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
				<td class="col-sm-1"><%= "Mutant" %></td>
				<td class="col-sm-1"><% if (m.isAlive()) {%><%="Alive"%><%} else {%><%="Dead"%><%} %></td>
				<td class="col-sm-1">
					<% if (game.getActivePlayer().equals("DEFENDER")) {%>
					Mark as Equivalent: <input type="checkbox" form="equiv" name="mutant<%=m.getId()%>" value="equivalent">
					<%}%>
				</td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="3"><%
					for (String change : m.getHTMLReadout()) {
						%><p><%=change%><p><%
					}
				%></td>
			</tr>
			<tr class="blank_row">
				<td class="row-borderless" colspan="3"></td>
			</tr>

		<%
		} 
		if (!isMutants) {%>
			<p> There are currently no mutants </p>
		<%}
		%>
		</table>

		<h2> Submitted Tests </h2>
		<table class="table table-hover table-responsive table-paragraphs">

		<% 
		boolean isTests = false;
		int count = 1;
		for (Test t : game.getTests()) { 
			isTests = true;
		%>

			<tr>
				<td class="col-sm-2"><%= "Test" %></td>
				<td class="col-sm-1"><%= "yes" %></td>
			</tr>

			<tr>
				<td class="col-sm-3" colspan="2">
					<%
						String utest = "";
						for (String line : t.getHTMLReadout()) { utest += line + "\n"; }
					%>
					<pre><textarea id=<%="tc"+count%> name="utest" class="utest" cols="20" rows="10"><%=utest%></textarea></pre>
				</td>

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

		<h2> Source Code </h2>
		<%
	    InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/data/sources/"+game.getClassName()+".java");
	    String line;
	    String source = "";
	    BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
	    while((line = is.readLine()) != null) {source+=line+"\n";}
		%>
		<pre>
		<textarea id="sut" name="cut" cols="80" rows="30"><%=source%></textarea>
		</pre>

	</div>

	<div id="right">
		<h2> Write your JUnit test here!</h2>
		<form id="equiv" action="play" method="post">
			<input type=hidden name="formType" value="markEquivalences">
		</form>
		<form id="def" action="play" method="post">
			<input type="hidden" name="formType" value="createTest">
	        <pre><textarea id="code" name="test" cols="80" rows="30">
import org.junit.*;
import static org.junit.Assert.*;

public class Test<%=game.getClassName()%> {
    @Test
    public void test() {
        // test here!
    }
}</textarea></pre>

		    <br>
	    </form>
	</div>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <script> 
	    var editorTest = CodeMirror.fromTextArea(document.getElementById("code"), {
	        lineNumbers: true,
	        matchBrackets: true
		});

	    editorTest.on('beforeChange',function(cm,change) {
		    var text = cm.getValue();
		    var lines = text.split(/\r|\r\n|\n/);
		    var readOnlyLines = [0,1,2,3,4,5];
		    var readOnlyLinesEnd = [lines.length-1,lines.length-2];
		    if ( ~readOnlyLines.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
			    change.cancel();
		    }
	    });
	    var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
		    lineNumbers: true,
		    matchBrackets: true,
		    readOnly: true
	    });
	    var x = document.getElementsByClassName("utest");
	    var i;
	    for (i = 0; i < x.length; i++) {
		    CodeMirror.fromTextArea(x[i], {
			    lineNumbers: true,
			    matchBrackets: true
		    });
	    }
    </script>
</body>
</html>
