<%@ page import="org.codedefenders.validation.CodeValidator.CodeValidatorLevel" %>
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

<div id="validatorExplanation" class="collapse panel panel-default"
     style="margin:auto; margin-top: 50px; max-width: 50%;">
    <%@ include file="/jsp/validator_explanation.jsp" %>
</div>

<% } %>
