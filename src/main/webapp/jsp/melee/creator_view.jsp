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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>

<%--
    @param MutliplayerGame game
        The game to be displayed.
--%>
<%
	MeleeGame game = (MeleeGame) request.getAttribute("game");
%>


<%-- -------------------------------------------------------------------------------- --%>

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
	classViewer.setClassCode(game.getCUT());
	classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
	gameHighlighting.setGameData(game.getMutants(), game.getTests());
	gameHighlighting.setFlaggingData(game.getMode(), game.getId());
	gameHighlighting.setEnableFlagging(false);
%>

<%--
<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<%
	mutantAccordion.setMutantAccordionData(cut, user, game.getMutants());
	mutantAccordion.setFlaggingData(game.getMode(), game.getId());
	mutantAccordion.setEnableFlagging(false);
	mutantAccordion.setViewDiff(true);
%>
--%>

<%--
<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<% testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants()); %>
--%>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<%-- -------------------------------------------------------------------------------- --%>


<div class="row">
	<div class="col-xl-6 col-12">
		<t:mutant_accordion/>

		<div id="tests-div">
            <div class="game-component-header"><h3>JUnit Tests</h3></div>
            <t:test_accordion/>
		</div>
	</div>

	<div class="col-xl-6 col-12" id="cut-div">
        <div class="game-component-header"><h3>Class Under Test</h3></div>
        <t:defender_intention_collection_note/>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
		<jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
	</div>
</div>
