<%--
  ~ Copyright (C) 2023 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="type" required="true" type="java.lang.String" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="total" required="true" type="java.lang.Integer" %>
<%@ attribute name="percentage" required="true" type="java.lang.Integer" %>
<%@ attribute name="label1" required="true" type="java.lang.String" %>
<%@ attribute name="value1" required="true" type="java.lang.Integer" %>
<%@ attribute name="label2" required="true" type="java.lang.String" %>
<%@ attribute name="value2" required="true" type="java.lang.Integer" %>

<div class="dashboard-box dashboard-${type}">
    <h3>${title}</h3>

    <div class="pie animate no-round ${total == 0 ? "no-data" : ""}"
         style="--percentage: ${percentage}">${total}</div>

    <div>
        <div class="legend">
            <span class="legend-title">${label1}</span>
            <span class="legend-value">${value1}</span>
        </div>
        <div class="legend">
            <span class="legend-title">${label2}</span>
            <span class="legend-value">${value2}</span>
        </div>
    </div>
</div>
