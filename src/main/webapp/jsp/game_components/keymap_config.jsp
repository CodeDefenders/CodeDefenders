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
<%@ page import="org.codedefenders.model.KeyMap" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>


<div class="btn-group" role="group">
    <div id="keymap-dropdown" class="btn-group" role="group">
        <button class="btn btn-sm btn-outline-secondary dropdown-toggle text-nowrap" type="button" id="editor-mode-menu" data-bs-toggle="dropdown" aria-expanded="false">
            <i class="fa fa-cog"></i>
            Editor Mode:
            <span id="current-keymap">${login.user.keyMap.CMName}</span>
        </button>
        <ul class="dropdown-menu" aria-labelledby="editor-mode-menu">
            <li><span class="dropdown-item">${login.user.keyMap.CMName}</span></li>
            <li class="dropdown-divider"></li>
            <%
                for (KeyMap km : KeyMap.values()) {
                    if (km != login.getUser().getKeyMap()) {
            %>
                    <li>
                        <form action="${url.forPath(Paths.USER_SETTINGS)}" method="post">
                            <input type="hidden" class="form-control" name="formType" value="updateKeyMap">
                            <input type="hidden" class="form-control" name="editorKeyMap" value="<%=km.name()%>">
                            <button class="dropdown-item" type="submit">
                                <%=km.getCMName()%>
                            </button>
                        </form>
                    </li>
            <%
                    }
                }
            %>
        </ul>
    </div>
    <button type="button" data-bs-toggle="modal" data-bs-target="#editor-help-modal" class="btn btn-sm btn-outline-secondary">
        <i class="fa fa-question-circle"></i>
    </button>
</div>

<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>
