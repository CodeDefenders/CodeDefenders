<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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