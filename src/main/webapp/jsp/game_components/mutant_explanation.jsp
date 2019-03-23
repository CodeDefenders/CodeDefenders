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
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%--
    Displays explanations about the mutant icons and the mutant validator level.

    @param CodeValidatorLevel mutantValidatorLevel
        The validation level for mutants.
--%>

<% { %>

<%
    CodeValidatorLevel level = (CodeValidatorLevel) request.getAttribute("mutantValidatorLevel");
    if (level == null) {
        level = CodeValidatorLevel.MODERATE;
    }

    String levelStyling;
    switch (level) {
        case RELAXED: levelStyling = "btn-success"; break;
        case MODERATE: levelStyling = "btn-warning"; break;
        case STRICT: levelStyling = "btn-danger"; break;
        default: levelStyling = "btn-primary";
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
        <div data-toggle="collapse" href="#validatorExplanation"
             title="Click the question sign for more information on the levels"
             class="<%="validatorLevelTag btn " + levelStyling%>">
             <%= StringUtils.capitalize(level.toString().toLowerCase()) %>
        </div>
        <div style="display: inline-block;">
            <a data-toggle="collapse" href="#validatorExplanation" style="color:black">
                <span class="glyphicon glyphicon-question-sign"></span>
            </a>
        </div>
    </div>
</div>

<div id="validatorExplanation" class="collapse panel panel-default"
     style="margin: 25px auto; max-width: 50%;">
    <%@ include file="/jsp/validator_explanation.jsp" %>
</div>

<% } %>
