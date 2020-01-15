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
<%@ page import="org.codedefenders.beans.game.MutantAccordionBean.MutantAccordionCategory" %>
<%@ page import="com.google.gson.GsonBuilder" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.util.JSONUtils" %>
<%@ page import="org.codedefenders.beans.game.MutantAccordionBean" %>

<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>

<style type="text/css">
    <%-- Prefix all classes with "ta-" to avoid conflicts.
    We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

    #mutants-accordion {
        margin-bottom: 0;
    }
    #mutants-accordion .panel-body {
        padding: 0;
    }
    #mutants-accordion thead {
        display: none;
    }
    #mutants-accordion .dataTables_scrollHead {
        display: none;
    }

    #mutants-accordion .panel-heading {
        padding-top: .375em;
        padding-bottom: .375em;
    }
    #mutants-accordion td {
        vertical-align: middle;
    }

    #mutants-accordion .panel-title.ma-covered {
        color: black;
    }
    #mutants-accordion .panel-title:not(.ma-covered) {
        color: #B0B0B0;
    }
    #mutants-accordion .ta-column-name {
        color: #B0B0B0;
    }

    #mutants-accordion .ta-count {
        margin-right: .5em;
        padding-bottom: .2em;
    }

    #mutants-accordion .ta-covered-link,
    #mutants-accordion .ta-killed-link {
        color: inherit;
    }
</style>

<div class="panel panel-default">
    <div class="panel-body" id="mutants">
        <div class="panel-group" id="mutants-accordion">
            <%
                for (MutantAccordionCategory category : mutantAccordion.getCategories()) {
            %>
            <div class="panel panel-default">
                <div class="panel-heading" id="ma-heading-<%=category.getId()%>">
                    <a role="button" data-toggle="collapse" aria-expanded="false"
                       href="#ma-collapse-<%=category.getId()%>"
                       aria-controls="ma-collapse-<%=category.getId()%>"
                       class="panel-title <%=category.getMutantIds().isEmpty() ? "" : "ma-covered"%>"
                       style="text-decoration: none;">
                        <% if (!category.getMutantIds().isEmpty()) { %>
                        <span class="label label-primary ma-count"><%=category.getMutantIds().size()%></span>
                        <% } %>
                        <%=category.getDescription()%>
                    </a>
                </div>
                <div class="panel-collapse collapse" data-parent="#mutants-accordion"
                     id="ma-collapse-<%=category.getId()%>"
                     aria-labelledby="ma-heading-<%=category.getId()%>">
                    <div class="panel-body">
                        <table id="ma-table-<%=category.getId()%>" class="table table-sm"></table>
                    </div>
                </div>
            </div>
            <%
                }
            %>
        </div>
    </div>
</div>

