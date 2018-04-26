<% String pageTitle="Duel Creation"; %>
<%@ include file="/jsp/header.jsp" %>


<div id="creategame" class="container">
	<form id="create" action="<%=request.getContextPath() %>/games" method="post" class="form-creategame">
		<h2>Create Game</h2>
		<input type="hidden" name="formType" value="createGame">
		<table class="tableform">
			<tr>
				<td>Java Class</td>
				<td>
					<select name="class" class="form-control selectpicker" data-size="large" >
						<% for (GameClass c : DatabaseAccess.getAllClasses()) { %>
						<option value="<%=c.getId()%>"><%=c.getAlias()%></option>
						<%}%>
					</select>
				</td>
				<td>
					<a href="games/upload" class="text-center new-account">Upload Class</a>
				</td>
			</tr>
			<%-- Disable mode selection for release.
			<tr>
				<td>Mode</td>
				<td>
					<select name="mode" class="form-control selectpicker" data-size="large">
						<option value="duel">Duel</option>
						<option value="sing">Singleplayer</option>
						<option value="utst">Unit Testing</option>
					</select>
				</td>
			</tr>
			--%>
			<tr>
				<td>Role</td> <td><input type="checkbox" id="role" name="role" class="form-control" data-size="large" data-toggle="toggle" data-on="Attacker" data-off="Defender" data-onstyle="success" data-offstyle="primary"></td>
			</tr>
			<tr>
				<td>Rounds</td><td><input class="form-control" type="number" name="rounds" value="3" min="1" max="10"></td>
			</tr>
			<tr>
            <td>Level</td> <td><input type="checkbox" id="level" name="level" class="form-control" data-size="large" data-toggle="toggle" data-on="Easy" data-off="Hard" data-onstyle="info" data-offstyle="warning">
			</tr>
		</table>
		<button class="btn btn-lg btn-primary btn-block" type="submit" value="Create">Create</button>
	</form>
</div>
<%@ include file="/jsp/footer.jsp" %>