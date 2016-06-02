<% String pageTitle="Open Games"; %>
<%@ include file="/jsp/header.jsp" %>

<table class="table table-hover table-responsive table-paragraphs">
	<tr>
		<td class="col-sm-2">Game No.</td>
		<td class="col-sm-2">Attacker</td>
		<td class="col-sm-2">Defender</td>
		<td class="col-sm-2">Game State</td>
		<td class="col-sm-2">Class Under Test</td>
		<td class="col-sm-2">Level</td>
		<td class="col-sm-2"></td>
	</tr>


	<%
		boolean isGames = false;
		String atkName;
		String defName;
		int uid = (Integer)request.getSession().getAttribute("uid");
		int atkId;
		int defId;
		for (Game g : DatabaseAccess.getAllGames()) {
			isGames = true;
			atkName = null;
			defName = null;

			atkId = g.getAttackerId();
			defId = g.getDefenderId();

			if ((atkId == uid)||(defId == uid)) {continue;}

			if (atkId != 0) {atkName = DatabaseAccess.getUserForKey("User_ID", atkId).username;}
			if (defId != 0) {defName = DatabaseAccess.getUserForKey("User_ID", defId).username;}

			if ((atkName != null)&&(defName != null)) {continue;}

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}
	%>

	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-2"><%= g.getState() %></td>
		<td class="col-sm-2"><%= DatabaseAccess.getClassForKey("Class_ID", g.getClassId()).name %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
		<td class="col-sm-2">
			<form id="view" action="games" method="post">
				<input type="hidden" name="formType" value="joinGame">
				<input type="hidden" name="game" value=<%=g.getId()%>>
				<input type="submit" class="btn btn-primary" value="Join Game">
			</form>
		</td>
	</tr>

	<%
		}
		if (!isGames) {%>
	<p> There are currently no open games </p>
	<%}
	%>
</table>


<%@ include file="/jsp/footer.jsp" %>
