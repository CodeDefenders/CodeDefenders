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

<%--
    Displays explanations about the mutant icons and the mutant validator level as model dialog
--%>

<%@ page import="org.apache.commons.lang.StringUtils"%>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>

<%
    String levelStyling;
    switch (mutantExplanation.getCodeValidatorLevel()) {
    case RELAXED:
        levelStyling = "btn-success";
        break;
    case MODERATE:
        levelStyling = "btn-warning";
        break;
    case STRICT:
        levelStyling = "btn-danger";
        break;
    default:
        levelStyling = "btn-primary";
    }
%>

<div class="d-flex justify-content-between flex-wrap gap-1">

    <div class="mutantCUTLegend">
        <span class="text-nowrap">
            <span class="mutantCUTImage mutantImageAlive align-middle"></span>
            <span class="mutantCUTLegendDesc align-middle me-1">Live</span>
        </span>
        <span class="text-nowrap">
            <span class="mutantCUTImage mutantImageKilled align-middle"></span>
            <span class="mutantCUTLegendDesc align-middle me-1">Killed</span>
        </span>
        <span class="text-nowrap">
            <span class="mutantCUTImage mutantImageFlagged align-middle"></span>
            <span class="mutantCUTLegendDesc align-middle me-1">Claimed Equivalent</span>
        </span>
        <span class="text-nowrap">
            <span class="mutantCUTImage mutantImageEquiv align-middle"></span>
            <span class="mutantCUTLegendDesc align-middle">Equivalent</span>
        </span>
    </div>

    <div>
         <span class="align-middle me-1">Mutant restrictions:</span>
         <button type="button" data-bs-toggle="modal" data-bs-target="#validator-explanation-modal"
                 title="Click for more information"
                 class="btn btn-xs <%=levelStyling%> align-middle">
             <%= StringUtils.capitalize(mutantExplanation.getCodeValidatorLevel().toString().toLowerCase()) %>
             <i class="fa fa-question-circle ms-1"></i>
         </button>
    </div>

</div>

<div id="validator-explanation-modal" class="modal fade" tabindex="-1" aria-labelledby="validator-explanation-modal-title" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
                <h5 class="modal-title" id="validator-explanation-modal-title">Mutant Validator Explanation</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
			</div>

			<div class="modal-body p-4">
				<%@ include file="/jsp/validator_explanation.jsp"%>
			</div>

			<div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
            </div>
		</div>
	</div>
</div>
