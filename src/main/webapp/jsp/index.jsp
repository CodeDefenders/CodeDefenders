<%--

    Copyright (C) 2016-2018 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<% String pageTitle = "Welcome"; %>

<%@ include file="/jsp/header_logout.jsp" %>

	<div id="splash" class="jumbotron masthead">
		<h1>Code Defenders</h1>
		<p>A Mutation Testing Game</p>
		<a id="enter" class="btn btn-primary btn-large" href="login">Enter</a>
		<p><img style="margin-top:20px" src="images/schema.jpeg" class="img-responsive displayed"/></p>
		<p class="text-muted" style="font-size:small;">
			<b>Important note:</b> Internet Explorer is not currently supported.
		</p>
	</div>
<%@ include file="/jsp/footer_logout.jsp" %>
<%@ include file="/jsp/footer.jsp" %>
