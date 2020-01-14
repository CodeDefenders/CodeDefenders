<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>

<%
	MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
	final GameClass cut = game.getCUT();
%>

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<% classViewer.setClassCode(game.getCUT()); %>
<% classViewer.setDependenciesForClass(game.getCUT()); %>

<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<% testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants()); %>

<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<% gameHighlighting.setGameData(game.getMutants(), game.getTests()); %>
<% gameHighlighting.setFlaggingData(game.getMode(), game.getId()); %>
<% gameHighlighting.setEnableFlagging(false); %>
<% gameHighlighting.setCodeDivSelector("#cut-div"); %>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>

<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<% mutantAccordion.setMutantAccordionData(cut, game.getAliveMutants(), game.getKilledMutants(),
		game.getMutantsMarkedEquivalent(), game.getMutantsMarkedEquivalentPending()); %>
<% mutantAccordion.setFlaggingData(game.getMode(), game.getId()); %>
<% mutantAccordion.setEnableFlagging(false); %>
<% mutantAccordion.setViewDiff(true); %>

</div> <%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
	<div class="col-md-6">
		<div id="mutants-div">
			<h3>Existing Mutants</h3>
			<jsp:include page="/jsp/game_components/mutants_list.jsp"/>
		</div>

		<div id="tests-div">
			<h3>JUnit tests </h3>
			<jsp:include page="/jsp/game_components/test_accordion.jsp"/>
		</div>
	</div>

	<div class="col-md-6" id="cut-div">
		<h3>Class Under Test</h3>
		<jsp:include page="/jsp/game_components/class_viewer.jsp"/>
		<jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
		<jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
	</div>
</div>
