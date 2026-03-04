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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ tag import="org.codedefenders.game.AbstractGame" %>
<%@ tag import="org.codedefenders.util.Constants" %>
<%@ tag import="static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS" %>
<%@ tag import="java.util.Objects" %>
<%@ tag import="org.xnap.commons.i18n.I18n" %>
<%
    I18n i18n = (I18n) request.getAttribute("i18n");
    AbstractGame game = (AbstractGame) request.getAttribute("game");
    game = Objects.nonNull(game) ? game : (AbstractGame) request.getAttribute(Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME);

    String maxAssertionsText;
    if (Objects.nonNull(game)) {
        maxAssertionsText = i18n.trn(
                "Only {0} assertion per test",
                "Only {0} assertions per test",
                game.getMaxAssertionsPerTest(),
                game.getMaxAssertionsPerTest()
        );
    } else {
        maxAssertionsText = i18n.tr("Only the configured number of assertions per test (default: {0})", DEFAULT_NB_ASSERTIONS);
    }
%>
<h3>${i18n.tr('Test rules')}</h3>
<ul class="mb-0">
    <li>${i18n.tr('No loops')}</li>
    <li>${i18n.tr('No calls to System.*')}</li>
    <li>${i18n.tr('No new methods or conditionals')}</li>
    <li><%=maxAssertionsText%></li>
</ul>
