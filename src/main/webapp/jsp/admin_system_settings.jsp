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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.stream.Collectors" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("System Settings"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminSystemSettings"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form id="changeSettings" name="changeSettings"
          class="needs-validation"
          action="${url.forPath(Paths.ADMIN_SETTINGS)}" method="post"
          autocomplete="off">
        <input type="hidden" name="formType" value="saveSettings">

        <%
            for (AdminSystemSettings.SettingsDTO setting : AdminDAO.getSystemSettings()) {

                // Do not show Killmap Setting here.
                if (AdminSystemSettings.SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.equals(setting.getName())) {
                    continue;
                }

                String readableName = Arrays.stream(setting.getName().name().split("_"))
                        .map(s -> s.charAt(0) + s.substring(1).toLowerCase())
                        .collect(Collectors.joining(" "));

                String explanation = setting.getName().toString();
        %>
            <div class="row mb-3">
                <label class="col-4 col-form-label" id="class-label"
                       for="<%=setting.getName().name()%>"
                       title="<%=explanation%>">
                    <%=readableName%>
                </label>
        <%

                switch (setting.getType()) {
                    case STRING_VALUE:
                        if (setting.getName().equals(AdminSystemSettings.SETTING_NAME.SITE_NOTICE) ||
                            setting.getName().equals(AdminSystemSettings.SETTING_NAME.PRIVACY_NOTICE) ||
                            setting.getName().equals(AdminSystemSettings.SETTING_NAME.CONTACT_NOTICE)) {
        %>
                <div class="col-8">
                    <textarea class="form-control" rows="3"
                              name="<%=setting.getName().name()%>"
                              id="<%=setting.getName().name()%>"><%=setting.getStringValue()%></textarea>
                </div>
        <%
                        } else {
        %>
                <div class="col-8">
                    <input type="<%=setting.getName().name().contains("PASSWORD") ? "password" : "text"%>"
                           class="form-control"
                           name="<%=setting.getName().name()%>"
                           id="<%=setting.getName().name()%>"
                           value="<%=setting.getStringValue()%>">
                    <% if (setting.getName().name().startsWith("EMAIL")) { %>
                        <div class="invalid-feedback">
                            This setting is required for sending emails.
                            Please provide a valid value, or disable emails.
                        </div>
                    <% } %>
                </div>
        <%
                        }
                        break;
                    case BOOL_VALUE:
        %>
                <div class="col-8 d-flex align-items-center">
                    <div class="form-check form-switch">
                        <input type="checkbox"
                               class="form-check-input"
                               id="<%=setting.getName().name()%>"
                               name="<%=setting.getName().name()%>"
                               <%=setting.getBoolValue() ? "checked" : ""%>>
                        <label class="form-check-label" for="<%=setting.getName().name()%>">
                            <%=readableName%>
                        </label>
                    </div>
                </div>
        <%
                        break;
                    case INT_VALUE:
        %>
                <div class="col-8">
                    <input type="number"
                           class="form-control"
                           name="<%=setting.getName().name()%>"
                           id="<%=setting.getName().name()%>"
                           value="<%=setting.getIntValue()%>">
                    <% if (setting.getName().name().startsWith("EMAIL")) { %>
                        <div class="invalid-feedback">
                            This setting is required for sending emails.
                            Please provide a valid value, or disable emails.
                        </div>
                    <% } %>
                </div>
        <%
                        break;
                }
        %>
            </div>
        <%
            }
        %>

        <div class="row g-2">
            <div class="col-auto">
                <button type="submit" class="btn btn-primary" name="saveSettingsBtn" id="saveSettingsBtn">Save</button>
            </div>
            <div class="col-auto">
                <button type="button" class="btn btn-secondary" id="cancelBtn" onclick="window.location.reload();">Cancel</button>
            </div>
        </div>
    </form>

    <script type="module">
        import $ from '${url.forPath("/js/jquery.mjs")}';

        // Validate regular contact email settings.
        $(document).ready(() => {
            const emailSwitch = document.getElementById('<%=AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED.name()%>');
            const otherEmailInputs = [
                document.getElementById('<%=AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS.name()%>'),
                document.getElementById('<%=AdminSystemSettings.SETTING_NAME.EMAIL_PASSWORD.name()%>'),
                document.getElementById('<%=AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_HOST.name()%>'),
                document.getElementById('<%=AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_PORT.name()%>')
            ];

            const validateEmailSettings = function () {
                for (const emailInput of otherEmailInputs) {
                    const valid = !emailSwitch.checked || emailInput.value.trim().length > 0;
                    emailInput.setCustomValidity(valid ? '' : 'value-missing');
                }
            };

            emailSwitch.addEventListener('change', validateEmailSettings);
            for (const emailInput of otherEmailInputs) {
                emailInput.addEventListener('input', validateEmailSettings);
            }

            validateEmailSettings();
        });

        // Validate email settings for teacher account applications.
        $(document).ready(() => {
            const teacherApplicationsSwitch = document.getElementById('<%=AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_ENABLED.name()%>');
            const teacherApplicationsEmail = document.getElementById('<%=AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL.name()%>');

            const validateEmailSettings = function () {
                const valid = !teacherApplicationsSwitch.checked || teacherApplicationsEmail.value.trim().length > 0;
                teacherApplicationsEmail.setCustomValidity(valid ? '' : 'value-missing');
            };

            teacherApplicationsSwitch.addEventListener('change', validateEmailSettings);
            teacherApplicationsEmail.addEventListener('input', validateEmailSettings);

            validateEmailSettings();
        });
    </script>
</div>
<%@ include file="/jsp/footer.jsp" %>
