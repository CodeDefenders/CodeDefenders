<%@ page import="org.codedefenders.model.KeyMap" %>

<%
    {
        KeyMap keymap = ((KeyMap) session.getAttribute("user-keymap"));
%>
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
                Editor Mode: <span id="current-keymap" ><%=keymap.getCMName()%></span>
            </button>
            <ul class="dropdown-menu dropdown-menu-right" aria-labelledby="editorModeMenu">
                <li><a><%=keymap.getCMName()%></a></li>
                <li role="separator" class="divider"></li>
                <%
                    for (KeyMap km : KeyMap.values()) {
                        if (km != keymap) {
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
<% } %>
