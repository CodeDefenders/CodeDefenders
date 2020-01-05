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

    <div class="btn-group" role="group">
        <div data-toggle="modal" href="#editorHelpConfig" class="btn btn-ssm btn-default">
            Keyboard Shortcuts <span class="glyphicon glyphicon-question-sign"></span>
        </div>
    </div>

    <div class="btn-group" role="group" aria-label="Second group">
        <div id="keymap-drowdown" class="dropdown">
            <button class="btn btn-default btn-ssm dropdown-toggle" type="button" id="editorModeMenu" data-toggle="dropdown">
                <span class="caret"></span>
                Editor Mode: <span id="current-keymap" >${login.user.keyMap.CMName}</span>
            </button>
            <ul class="dropdown-menu dropdown-menu-right" aria-labelledby="editorModeMenu">
                <li><a>${login.user.keyMap.CMName}</a></li>
                <li role="separator" class="divider"></li>
                <%
                    for (KeyMap km : KeyMap.values()) {
                        if (km != login.getUser().getKeyMap()) {
                %>
                        <li>
                            <a>
                                <form action="<%=request.getContextPath() + Paths.USER_PROFILE%>" method="post">
                                    <input type="hidden" class="form-control" name="formType" value="updateKeyMap">
                                    <input type="hidden" class="form-control" name="editorKeyMap" value="<%=km.name()%>">
                                    <button style="width: 100%; all: inherit" type="submit"><%=km.getCMName()%></button>
                                </form>
                            </a>
                        </li>
                <%
                        }
                    }
                %>
            </ul>
        </div>
    </div>

</div>
