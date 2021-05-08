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
<%@ page import="org.codedefenders.model.KeyMap" %>
<%@ page import="org.codedefenders.util.Paths" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<div class="btn-toolbar" role="toolbar">

    <div class="btn-group me-2" role="group">
        <button type="button" data-bs-toggle="modal" data-bs-target="#editor-help-modal" class="btn btn-xs btn-secondary">
            Keyboard Shortcuts
            <i class="fa fa-question-circle ms-1"></i>
        </button>
    </div>

    <div class="btn-group" role="group">
        <div id="keymap-dropdown" class="dropdown">
            <button class="btn btn-xs btn-secondary dropdown-toggle" type="button" id="editor-mode-menu" data-bs-toggle="dropdown" aria-expanded="false">
                Editor Mode: <span id="current-keymap">${login.user.keyMap.CMName}</span>
            </button>
            <ul class="dropdown-menu" aria-labelledby="editor-mode-menu">
                <li><span class="dropdown-item">${login.user.keyMap.CMName}</span></li>
                <li class="dropdown-divider"></li>
                <%-- FIXME: Workaround: The keymap forms are now nested in the "defend" form and since nested forms are
                not officially a thing, the first nested form tag is somehow removed. So we insert a dummy form tag that
                can be removed instead. --%>
                <form style="display: none;"></form>
                <%
                    for (KeyMap km : KeyMap.values()) {
                        if (km != login.getUser().getKeyMap()) {
                %>
                        <li>
                            <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post">
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
    </div>

</div>

<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>
