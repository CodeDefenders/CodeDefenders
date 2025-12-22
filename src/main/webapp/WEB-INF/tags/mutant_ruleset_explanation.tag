<%@ tag import="org.codedefenders.validation.code.MutantValidationRuleSet" %>
<%@ tag import="org.codedefenders.validation.code.DefaultRuleSets" %><%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ attribute name="ruleset" required="true" type="org.codedefenders.validation.code.MutantValidationRuleSet" %>
<h2>Mutation rules</h2>
<h3>${ruleset.name}</h3> <br>

<div class="accordion" id="rule-accordion-${ruleset.name}">
    <c:forEach items="${ruleset.tieredRules}" var="group" varStatus="groupStatus">
        <div class="accordion-item">
            <h2 class="accordion-header" id="rule-heading-${ruleset.name}-${groupStatus.index}">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                        data-bs-target="#rule-collapse-${ruleset.name}-${groupStatus.index}"
                        aria-expanded="false" aria-controls="rule-collapse-${ruleset.name}-${groupStatus.index}">
                        ${group.get(0).generalDescription}
                </button>
            </h2>
            <div id="rule-collapse-${ruleset.name}-${groupStatus.index}" class="accordion-collapse collapse" aria-labelledby="rule-heading-${ruleset.name}-${groupStatus.index}" data-bs-parent="#rule-accordion-${ruleset.name}">
                <div class="accordion-body">
                    <ul>
                        <c:forEach items="${group}" var="rule">
                            <li>${rule.detailedDescription}</li>
                        </c:forEach>
                    </ul>
                </div>
            </div>
        </div>
    </c:forEach>
</div>
