<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.AdminDAO" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header.jsp" %>

<div class="full-width">
    <ul class="nav nav-tabs">
        <li><a href="<%=request.getContextPath()%>/admin/games"> Manage Games</a></li>
        <li><a href="<%=request.getContextPath()%>/admin/users">Manage Users</a></li>
        <li class="active"><a href="#">System Settings</a></li>
    </ul>

    <h3>System Settings</h3>

    <form id="changeSettings" action="admin/settings" method="post">
        <input type="hidden" name="formType" value="saveSettings">

        <% for (AdminSystemSettings.SettingsDTO setting : AdminDAO.getSystemSettings()) {
            String readableName = setting.getName().name().toLowerCase().replace("_", " ");
            String explanation = setting.getName().toString();
            switch (setting.getType()) {
                case STRING_VALUE:
                    if (setting.getName().equals(AdminSystemSettings.SETTING_NAME.SITE_NOTICE)) {%>
        <div class="form-group">
            <label for="<%=setting.getName().name()%>" title="<%=explanation%>"><%=readableName%>
            </label>
            <textarea class="form-control" rows="3" name="<%=setting.getName().name()%>"
                      id="<%=setting.getName().name()%>"><%=setting.getStringValue()%></textarea>
        </div>

        <% } else {%>
        <div class="input-group">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input class="form-control" name="<%=setting.getName().name()%>"
                   type="<%=setting.getName().name().contains("PASSWORD") ? "password" : "text"%>"
                   id="<%=setting.getName().name()%>"><%=setting.getStringValue()%>
        </div>
        <%
                }
                break;
            case BOOL_VALUE:
        %>
        <div class="input-group">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input type="checkbox" id="<%=setting.getName().name()%>" name="<%=setting.getName().name()%>"
                   class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                   data-onstyle="primary" data-offstyle=""
                <%=setting.getBoolValue() ? "checked" : ""%>>
        </div>

        <% break;
            case INT_VALUE: %>
        <div class="input-group">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input type="number" value="<%=setting.getIntValue()%>" id="<%=setting.getName().name()%>"
                   name="<%=setting.getName().name()%>" min="1" required class="form-control" style="width: 80px"/>
        </div>
        <%
                    break;
            }
        %>
        <br>
        <%}%>
        <button type="submit" class="btn btn-primary" name="saveSettingsBtn" id="saveSettingsBtn"> Save
        </button>
        <a class="btn btn-default" onclick="window.location.reload();">Cancel</a>
    </form>
</div>
<%@ include file="/jsp/footer.jsp" %>
