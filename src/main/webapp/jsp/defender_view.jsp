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
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>

<% String pageTitle="Defending Class"; %>
<%@ include file="/jsp/header_game.jsp" %>

<% { %>

<%-- Set request attributes for the components. --%>
<%
	/* class_viewer */
    final GameClass cut = game.getCUT();
    request.setAttribute("className", cut.getBaseName());
    request.setAttribute("classCode", cut.getAsHTMLEscapedString());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

	/* test_editor */
	String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	if (previousTestCode != null) {
		request.setAttribute("testCode", previousTestCode);
	} else {
		request.setAttribute("testCode", game.getCUT().getTestTemplate());
	}
    request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

	/* tests_carousel */
	request.setAttribute("tests", game.getTests());

	/* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("markEquivalent", true);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY);
	request.setAttribute("gameType", "DUEL");

	/* game_highlighting */
	request.setAttribute("codeDivSelector", "#cut-div");
	// request.setAttribute("tests", game.getTests());
	request.setAttribute("mutants", game.getMutants());
	request.setAttribute("showEquivalenceButton", game.getState() == GameState.ACTIVE);
    // request.setAttribute("markUncoveredEquivalent", false);
    // request.setAttribute("gameType", "DUEL");

	/* finished_modal */
    int attackerScore = game.getAttackerScore();
    int defenderScore = game.getDefenderScore();
	request.setAttribute("win", defenderScore > attackerScore);
	request.setAttribute("loss", attackerScore > defenderScore);

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* test_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<% if (game.getState() == GameState.FINISHED) { %>
    <%@include file="game_components/finished_modal.jsp"%>
<% } %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="game_components/class_viewer.jsp"%>
        <%@include file="game_components/game_highlighting.jsp"%>
        <%@include file="game_components/mutant_explanation.jsp"%>
    </div>

    <div class="col-md-6" id="utest-div">
        <%@include file="game_components/test_progress_bar.jsp"%>
        <h3>Write a new JUnit test here
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
                    <% if (game.getState() != GameState.ACTIVE || game.getActiveRole() != Role.DEFENDER) { %> disabled <% } %>>
                Defend!
            </button>
        </h3>
        <form id="def" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
            <%@include file="game_components/test_editor.jsp"%>
            <input type="hidden" name="formType" value="createTest">
        </form>
    </div>
</div>

<div class="row" style="padding: 0px 15px;">
	<div class="col-md-6" id="submitted-div">
		<h3>JUnit tests </h3>
		<%@include file="game_components/tests_carousel.jsp"%>
	</div>

	<div class="col-md-6" id="mutants-div">
		<h3>Mutants</h3>
        <%@include file="game_components/mutants_list.jsp"%>
	</div>
</div>

<script>
	<% if (game.getActiveRole().equals(Role.ATTACKER)) {%>
        function checkForUpdate(){
            $.post('/duelgame', {
                formType: "whoseTurn",
                gameID: <%= game.getId() %>
            }, function(data){
                if(data === "defender"){
                    window.location.reload();
                }
            },"text");
        }
        setInterval("checkForUpdate()", 10000);
	<% } %>
</script>

<% } %>

<%@include file="/jsp/footer_game.jsp" %>
