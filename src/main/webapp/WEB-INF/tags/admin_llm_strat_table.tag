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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="strategies" required="true" type="java.util.List<org.codedefenders.model.llm.LlmStrategy>" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>

<div class="card m-2" id="${htmlId}">
    <div class="card-body">
        <h4 class="card-title">${title}:</h4>
        <table id="attack-strategies" class="table table-v-align-middle table-striped">
            <thead>
            <tr>
                <th>Name</th>
                <th>Based on</th>
                <th>Edit</th>
            </tr>
            </thead>
            <c:forEach items="${strategies}" var="strat">
                <tr>
                    <td>${strat.name}</td>
                    <td>${!strat.readOnly ? strat.base.name() : ""}</td>
                    <td>
                        <c:choose>
                        <c:when test="${strat.readOnly}">
                            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#${htmlId}-${strat.name}-modal">
                                Create custom variant
                            </button>
                        </c:when>
                        <c:otherwise>
                            <button class="btn btn-secondary">
                                Edit TODO
                            </button>
                        </c:otherwise>
                        </c:choose>

                </tr>
                <t:admin_llm_edit_strategy_modal htmlId="${htmlId}-${strat.name}-modal" baseStrategy="${strat.base}"/>
            </c:forEach>
        </table>
    </div>
</div>
