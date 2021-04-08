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
<div style="height: 24px;">
	<div>
		<span class="mutantCUTImage mutantImageAlive"></span>
		<span class="mutantCUTLegendDesc">Live</span>
		<span class="mutantCUTImage mutantImageKilled"></span>
		<span class="mutantCUTLegendDesc">Killed</span>
		<span class="mutantCUTImage mutantImageFlagged"></span>
		<span class="mutantCUTLegendDesc">Claimed Equivalent</span>
		<span class="mutantCUTImage mutantImageEquiv"></span>
		<span class="mutantCUTLegendDesc">Equivalent</span>
	</div>

    <div style="float:right">
         <div style="display: inline-block;"> Mutant restrictions:</div>
         <div
            data-toggle="modal" href="#validatorExplanation"
            title="Click the question sign for more information on the levels"
            class="<%="validatorLevelTag btn " + levelStyling%>">
            <%=StringUtils.capitalize(mutantExplanation.getCodeValidatorLevel().toString().toLowerCase())%>
        </div>
         <div style="display: inline-block;">
             <a data-toggle="modal" href="#validatorExplanation" style="color:black">
                 <span class="glyphicon glyphicon-question-sign"></span>
             </a>
         </div>
     </div>
</div>

<div class="modal fade" id="validatorExplanation" role="dialog"
	aria-labelledby="validatorExplanation" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">&times;</button>
				<h4 class="modal-title">Validator Explanation</h4>
			</div>

            <div class="modal-body">
                <%@ include file="/jsp/mutant_validator_explanation.jsp"%>
				<%@ include file="/jsp/test_validator_explanation.jsp"%>
			</div>

			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>



