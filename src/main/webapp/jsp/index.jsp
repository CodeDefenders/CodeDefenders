<% String pageTitle = "Welcome"; %>

<%@ include file="/jsp/header_logout.jsp" %>

	<div id="splash" class="jumbotron masthead">
		<h1>Code Defenders</h1>
		<p>A Mutation Testing Game</p>
		<a  class="btn btn-primary btn-large"  href="login">Enter</a>
		<p><img style="margin-top:20px" src="images/schema.jpeg" class="img-responsive displayed"/></p>
		<p class="text-muted" style="font-size:small;">
			Developed at The University of Sheffield <br />Supported by the <a href="https://www.sheffield.ac.uk/sure">SURE (Sheffield Undergraduate Research Experience)</a> scheme <br />
			<b>Important note:</b> Internet Explorer and Safari are not currently supported.
		</p>
	</div>
<%@ include file="/jsp/footer_logout.jsp" %>
<%@ include file="/jsp/footer.jsp" %>
