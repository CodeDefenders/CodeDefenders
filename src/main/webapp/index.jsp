<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>

<head>
	<title>Code Defenders</title>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/gamestyle.css" rel="stylesheet">

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
	<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
	<!-- Include all compiled plugins (below), or include individual files as needed -->
	<script src="js/bootstrap.min.js"></script>
	<script type="text/javascript">
		$(document).ready(function() {
			$('#messages-div').delay(10000).fadeOut();
		});
		var $j = jQuery.noConflict();

		$j(document).ready(function() {
			// Toggle Single Bibtex entry
			$j('a.papercite_toggle').click(function() {
				$j( "#" + $j(this).attr("id") + "_block" ).toggle();
				return false;
			});
		});
	</script>
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
</head>

<body>

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
				    <li><a href="#research">Research</a></li>
				    <li><a href="#contact">Contact</a></li>
			    </ul>
		    </div>
   		</div>
	</nav>

	<div id="splash" class="jumbotron masthead">
		<h1>Code Defenders</h1>
		<p>A Mutation Testing Game</p>
		<a  class="btn btn-primary btn-large"  href="login">Enter</a>
		<p><img style="margin-top:20px" src="images/schema.jpeg" class="img-responsive displayed"/></p>
		<p class="text-muted" style="font-size:small;">
			Developed at The University of Sheffield <br>Supported by the <a href="https://www.sheffield.ac.uk/sure">SURE (Sheffield Undergraduate Research Experience)</a> scheme
		</p>
	</div>

	<hr>

	<div id="research" class="container">
		<br><br><br>
		<h1>Research</h1>
		<p></p>
		<div class="row-fluid">
				<ul class="papercite_bibliography">
					<li>
						<a href="papers/Mutation16_CodeDefenders.pdf" title='Download PDF'>
							<img src='images/pdf.png' alt="[PDF]"/>
						</a>
						Jos&eacute; Miguel Rojas and G. Fraser
						&#8220;Code Defenders: A Mutation Testing Game,&#8221;
						In <span style="font-style: italic">Proc. of The 11th International Workshop on Mutation Analysis</span>,  2016.<br/>
						<a href="javascript:void(0)" onclick="javascript:var x=document.getElementById('mutation16_bibtex');x.style.display=x.style.display=='none'?'':'none'">[Bibtex]</a>
						<div style="display: none" id="mutation16_bibtex">
<pre style="text-align: left"><code class="tex bibtex">@inproceedings{Mutation16_CodeDefenders,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Code Defenders: A Mutation Testing Game},
	booktitle = {Proc. of The 11th International Workshop on Mutation Analysis},
	year = {2016},
	publisher = {IEEE},
	note = {To appear}
}</code></pre>
						</div>
					</li>
				</ul>
		</div>
	</div>

	<hr>

	<div id="contact" class="container">
		<form  action="sendEmail" method="post" class="form-signin">
			<input type="hidden" name="formType" value="login">
			<h1 class="form-signin-heading">Contact Us</h1>
			<label for="inputName" class="sr-only">Name</label>
			<input type="text" id="inputName" name="name" class="form-control" placeholder="Name" required>
			<label for="inputEmail" class="sr-only">Email</label>
			<input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
			<label for="inputSubject" class="sr-only">Subject</label>
			<input type="text" id="inputSubject" name="subject" class="form-control" placeholder="Subject" required>
			<label for="inputMessage" class="sr-only">Message</label>
			<textarea id="inputMessage" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
			<button class="btn btn-lg btn-primary btn-block" type="submit">Send</button>
		</form>
		<%
			String result = (String)request.getSession().getAttribute("emailSent");
			request.getSession().removeAttribute("emailSent");
			if (result != null) {
		%>
		<div class="alert alert-info" id="messages-div">
			<p><%=result%></p>
		</div>
		<%
			}
		%>
	</div>

</body>
</html>
