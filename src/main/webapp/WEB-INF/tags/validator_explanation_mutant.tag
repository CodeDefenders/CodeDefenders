<%--

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

<%@ tag pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="ruleset" required="false" type="java.lang.String" %>


<%--@elvariable id="defaultRuleSets" type="org.codedefenders.validation.code.DefaultRuleSets"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>


<c:forEach items="${defaultRuleSets.getValues()}" var="set">
    <div id="rule-div-${set.getName()}" ${ruleset != null && !set.getName().equals(ruleset) ? "hidden" : ""}>
        <t:mutant_ruleset_explanation ruleset="${set}"/>
    </div>
</c:forEach>

