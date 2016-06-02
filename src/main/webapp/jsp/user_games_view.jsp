<% String pageTitle="My Games"; %>
<%@ include file="/jsp/header.jsp" %>
	<%
		boolean isGames = false;
		boolean canEnter;
		String atkName;
		String defName;
        String classUnderAttack;
		int uid = (Integer)request.getSession().getAttribute("uid");
        ArrayList<Game> userGames = DatabaseAccess.getGamesForUser(uid);

		for (Game g : userGames) {
			isGames = true;
			atkName = null;
			defName = null;

			if (g.getState().equals(Game.State.FINISHED)) {continue;} // Dont display in active games if finished
			if (g.getState().equals(Game.State.ACTIVE)) {canEnter = true;} // If it is in progress you can enter.
			else {canEnter = false;} // Otherwise, you can see it but you cant enter.

			if (g.getAttackerId() != 0) {
				atkName = DatabaseAccess.getUserForKey("User_ID", g.getAttackerId()).username;
			}

			if (g.getDefenderId() != 0) {
				defName = DatabaseAccess.getUserForKey("User_ID", g.getDefenderId()).username;
			}

			int turnId = g.getAttackerId();
			if (g.getActiveRole().equals(Game.Role.DEFENDER))
				turnId = g.getDefenderId();

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}
            classUnderAttack = DatabaseAccess.getClassForKey("Class_ID", g.getClassId()).name;
            final int stringLength = 25;
            if (classUnderAttack.length() > stringLength){
                classUnderAttack = classUnderAttack.substring(0, classUnderAttack.indexOf(".")) + "." + classUnderAttack.substring(classUnderAttack.lastIndexOf("."));
            }
	%>
    <div class="ws-4"><ul class="unstyled">
		<li>
	<a class="list-item" href='javascript: ' onclick="$('#view').submit();">
		<div class="pull-left" style="margin-right: 10px;">
            <% if (uid == turnId ) {%>
            <div class="circle bg-red" style="width: 40px; height: 40px;"></div>
            <% } else {%>
            <div class="circle bg-light-green" style="width: 40px; height: 40px;"></div>
            <% }%>
		</div>
		<div class="text" style="margin: 5px;">
			<div>
                <p>Attack on <span class="text-green"><%= classUnderAttack %></span>.<br />
				<span class="text-red"><%= atkName %></span> is attacking.<br />
				<span class="text-light-blue"><%= defName %></span> is defending.</p>
						<%
				if (canEnter) { %>

				<form id="view" action="games" method="post">
					<input type="hidden" name="formType" value="enterGame">
					<input type="hidden" name="game" value=<%=g.getId()%>>
				</form>

					<% } %>
				</div>
			<div class="drop down small">

			</div>
            <br />
            <span class="small">
                <p>Difficulty: <%= g.getLevel().name() %></p>
            </span>
		</div>
		</a>
		</li>


    </ul></div>

	<%
		}
		if (!isGames) {%>
	<p> You are not currently in any games </p>

    <% } %>

<%@ include file="/jsp/footer.jsp" %>