<%--
  ~ Copyright (C) 2021 Code Defenders contributors
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ tag import="org.codedefenders.game.AbstractGame" %>
<%@ tag import="org.codedefenders.util.Constants" %>
<%@ tag import="java.util.Objects" %>
<%
    AbstractGame game = (AbstractGame) request.getAttribute("game");
    game = Objects.nonNull(game) ? game : (AbstractGame) request.getAttribute(Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME);

    String maxAssertionsPerTest;
    if (Objects.nonNull(game)) {
        maxAssertionsPerTest = Integer.toString(game.getMaxAssertionsPerTest());
    } else {
        maxAssertionsPerTest = "the configured amount of";
    }
%>
<h3>Test rules</h3>
<ul class="mb-0">
    <li>No loops</li>
    <li>No calls to System.*</li>
    <li>No new methods or conditionals</li>
    <li>Only <%=maxAssertionsPerTest%> assertion(s) per test</li>
</ul>
