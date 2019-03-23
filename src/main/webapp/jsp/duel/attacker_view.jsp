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
<% String pageTitle = "Attacking Class"; %>
<%@ include file="/jsp/duel/header_game.jsp" %>

<% { %>

<%-- Set request attributes for the components. --%>
<%
    /* mutant_editor */
    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);

    final GameClass cut = game.getCUT();

    if (previousMutantCode != null) {
        request.setAttribute("mutantCode", previousMutantCode);
    } else {
        request.setAttribute("mutantCode", cut.getAsHTMLEscapedString());
    }
    request.setAttribute("mutantName", cut.getBaseName());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* tests_carousel */
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", GameMode.DUEL);
    request.setAttribute("gameId", game.getId());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
//    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    // request.setAttribute("markUncoveredEquivalent", false);
    // request.setAttribute("gameType", GameMode.DUEL);
//    request.setAttribute("gameId", game.getId());

    /* finished_modal */
    int attackerScore = game.getAttackerScore();
    int defenderScore = game.getDefenderScore();
    request.setAttribute("win", attackerScore > defenderScore);
    request.setAttribute("loss", defenderScore > attackerScore);

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* mutant_progressbar */
//    request.setAttribute("gameId", game.getId());
%>

<% if (game.getState() == GameState.FINISHED) { %>
    <%@include file="../game_components/finished_modal.jsp"%>
<% } %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Mutants</h3>
            <%@include file="../game_components/mutants_list.jsp"%>
        </div>

        <% if (game.getLevel().equals(GameLevel.EASY) || game.getState().equals(GameState.FINISHED)) { %>
            <div id="tests-div">
                <h3>JUnit tests </h3>
                <%@include file="../game_components/tests_carousel.jsp"%>
            </div>
        <% } %>
    </div>

    <div class="col-md-6" id="cut-div">
        <%@include file="../game_components/mutant_progress_bar.jsp"%>
        <h3 style="margin-bottom: 0;">Create a mutant here</h3>

        <form id="reset" action="<%=request.getContextPath() + Paths.DUEL_GAME %>" method="post">
            <input type="hidden" name="formType" value="reset">
            <input type="hidden" name="gameId" value="<%=game.getId()%>">
            <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset" style="margin-top: -40px; margin-right: 80px">
                Reset
            </button>
        </form>

        <form id="atk" action="<%=request.getContextPath() + Paths.DUEL_GAME %>" method="post">
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';" style="margin-top: -50px"
                    <% if (game.getState() != GameState.ACTIVE || game.getActiveRole() != Role.ATTACKER) { %> disabled <% } %>>
                Attack!
            </button>
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>"/>

            <%@include file="../game_components/mutant_editor.jsp"%>
            <%@include file="../game_components/game_highlighting.jsp"%>
        </form>
        <%@include file="../game_components/mutant_explanation.jsp"%>
        <%@include file="../game_components/editor_help_config_toolbar.jsp"%>
    </div>
</div>

<script>
    <% if (game.getActiveRole().equals(Role.DEFENDER)) {%>
    function checkForUpdate() {
        $.post('<%=request.getContextPath() + Paths.DUEL_GAME%>', {
            formType: "whoseTurn",
            gameId: <%= game.getId() %>
        }, function (data) {
            if (data === "attacker") {
                window.location.reload();
            }
        }, 'text');
    }
    setInterval(checkForUpdate, 10000);
    <% } %>
</script>

</div>
<%@include file="../game_components/editor_help_config_modal.jsp"%>
<div>

<%@ include file="/jsp/footer_game.jsp" %>

<% } %>