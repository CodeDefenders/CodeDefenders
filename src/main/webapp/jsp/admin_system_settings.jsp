<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminSystemSettings"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>System Settings</h3>
    <form id="changeSettings" name="changeSettings" action="admin/settings" onsubmit="return validateForm()" method="post">
        <script>
            function validateForm() {
                var form = document.forms['changeSettings'];
                var mailEnabled = form["<%=AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED.name()%>"].checked;
                var address = form["<%=AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS.name()%>"];
                var password = form["<%=AdminSystemSettings.SETTING_NAME.EMAIL_PASSWORD.name()%>"];
                var smtpHost = form["<%=AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_HOST.name()%>"];
                var smtpPort = form["<%=AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_PORT.name()%>"];

                if (mailEnabled) {
                    if (address.value === "") {
                        address.parentElement.style.border = "1px solid red";
                    } else {
                        address.parentElement.style.border = '0px';
                    }
                    if (password.value === "") {
                        password.parentElement.style.border = "1px solid red";
                    } else {
                        password.parentElement.style.border ='0px';
                    }

                    if (smtpHost.value === "") {
                        smtpHost.parentElement.style.border = "1px solid red";
                    } else {
                        smtpHost.parentElement.style.border ='0px';
                    }

                    if (smtpPort.value === "") {
                        smtpPort.parentElement.style.border = "1px solid red";
                    } else {
                        smtpPort.parentElement.style.border ='0px';
                    }

                    return address.value !== "" && password.value !== "" && smtpHost.value !== "" && smtpPort.value !== 0;
                } else {
                    address.parentElement.style.border = '0px';
                    password.parentElement.style.border ='0px';
                    smtpHost.parentElement.style.border ='0px';
                    smtpPort.parentElement.style.border ='0px';
                    return true;
                }
            }
        </script>
        <input type="hidden" name="formType" value="saveSettings">

        <% for (AdminSystemSettings.SettingsDTO setting : AdminDAO.getSystemSettings()) {
            String readableName = setting.getName().name().toLowerCase().replace("_", " ");
            String explanation = setting.getName().toString();
            switch (setting.getType()) {
                case STRING_VALUE:
                    if (setting.getName().equals(AdminSystemSettings.SETTING_NAME.SITE_NOTICE)) {%>
        <div class="form-group" id="<%="group_"+setting.getName().name()%>">
            <label for="<%=setting.getName().name()%>" title="<%=explanation%>"><%=readableName%>
            </label>
            <textarea class="form-control" rows="3" name="<%=setting.getName().name()%>"
                      id="<%=setting.getName().name()%>"><%=setting.getStringValue()%></textarea>
        </div>

        <% } else {%>
        <div class="input-group" id="<%="group_"+setting.getName().name()%>">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input class="form-control" name="<%=setting.getName().name()%>"
                   <%if (!setting.getName().name().startsWith("EMAIL_")) {%>required <%}%>
                   style = "padding-left:10px"
                   type="<%=setting.getName().name().contains("PASSWORD") ? "password" : "text"%>"
                   id="<%=setting.getName().name()%>" value="<%=setting.getStringValue()%>">
        </div>
        <%
                }
                break;
            case BOOL_VALUE:
        %>
        <div class="input-group" id="<%="group_"+setting.getName().name()%>">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input type="checkbox" id="<%=setting.getName().name()%>" name="<%=setting.getName().name()%>"
                   class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                   data-onstyle="primary" data-offstyle=""
                <%=setting.getBoolValue() ? "checked" : ""%>>
        </div>

        <% break;
            case INT_VALUE: %>
        <div class="input-group" id="<%="group_"+setting.getName().name()%>">
            <span class="input-group-addon" style=" width: 250px; text-align: left;"
                  title="<%=explanation%>"><%=readableName%> </span>
            <input type="number" value="<%=setting.getIntValue()%>" id="<%=setting.getName().name()%>"
                   name="<%=setting.getName().name()%>" min="0" required class="form-control" style="width: 80px"/>
        </div>
        <%
                    break;
            }
        %>
        <br>
        <%}%>
        <button type="submit" class="btn btn-primary" name="saveSettingsBtn" id="saveSettingsBtn"> Save
        </button>
        <a class="btn btn-default" id="cancelBtn" onclick="window.location.reload();">Cancel</a>
    </form>
</div>
<%@ include file="/jsp/footer.jsp" %>
