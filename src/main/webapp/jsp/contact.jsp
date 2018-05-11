<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.*" %>
<% String pageTitle = "Contact Us"; %>

<%
	Object uid = request.getSession().getAttribute("uid");
	Object username = request.getSession().getAttribute("username");
	if (uid != null && username != null){
%>
<%@ include file="/jsp/header.jsp" %>
<%} else {%>
<%@ include file="/jsp/header_logout.jsp" %>
<%}%>
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
<div class="container" style=" max-width: 50%; min-width: 25%; ">
	<h2 style="text-align: center">Contact Us</h2>
	<p style="text-align: center">
		Code Defenders is an open source project; you can find details on the
		<a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> project page.
	</p>
</div>
	<%
	final boolean emailEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
	if(emailEnabled) {
%>
<div class="container">
	<form  action="<%=request.getContextPath() %>/sendEmail" method="post" class="form-signin">
		<input type="hidden" name="formType" value="login">
		<label for="inputName" class="sr-only">Name</label>
		<input type="text" id="inputName" name="name" class="form-control" placeholder="Name" required autofocus>
		<label for="inputEmail" class="sr-only">Email</label>
		<input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
		<label for="inputSubject" class="sr-only">Subject</label>
		<input type="text" id="inputSubject" name="subject" class="form-control" placeholder="Subject" required autofocus>
		<label for="inputMessage" class="sr-only">Message</label>
		<textarea id="inputMessage" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
		<button class="btn btn-lg btn-primary btn-block" type="submit">Send</button>
	</form>
<%
	}
%>
</div>

<%@ include file="/jsp/footer.jsp" %>
