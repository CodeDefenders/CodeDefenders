<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_TYPE" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%
    // Get the enum constants here, since EL doesn't seem to work with inner-class enums.
    pageContext.setAttribute("BOOL_VALUE", SETTING_TYPE.BOOL_VALUE);
    pageContext.setAttribute("INT_VALUE", SETTING_TYPE.INT_VALUE);
    pageContext.setAttribute("STRING_VALUE", SETTING_TYPE.STRING_VALUE);

    pageContext.setAttribute("AUTOMATIC_KILLMAP_COMPUTATION", SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION);
    pageContext.setAttribute("SITE_NOTICE", SETTING_NAME.SITE_NOTICE);
    pageContext.setAttribute("PRIVACY_NOTICE", SETTING_NAME.PRIVACY_NOTICE);
    pageContext.setAttribute("CONTACT_NOTICE", SETTING_NAME.CONTACT_NOTICE);
    pageContext.setAttribute("EMAILS_ENABLED", SETTING_NAME.EMAILS_ENABLED);
    pageContext.setAttribute("EMAIL_ADDRESS", SETTING_NAME.EMAIL_ADDRESS);
    pageContext.setAttribute("EMAIL_PASSWORD", SETTING_NAME.EMAIL_PASSWORD);
    pageContext.setAttribute("EMAIL_SMTP_HOST", SETTING_NAME.EMAIL_SMTP_HOST);
    pageContext.setAttribute("EMAIL_SMTP_PORT", SETTING_NAME.EMAIL_SMTP_PORT);
    pageContext.setAttribute("TEACHER_APPLICATIONS_ENABLED", SETTING_NAME.TEACHER_APPLICATIONS_ENABLED);
    pageContext.setAttribute("TEACHER_APPLICATIONS_EMAIL", SETTING_NAME.TEACHER_APPLICATIONS_EMAIL);
%>

<p:main_page title="System Settings">
    <div class="container">
        <t:admin_navigation activePage="adminSystemSettings"/>

        <form id="changeSettings" name="changeSettings"
              class="needs-validation"
              action="${url.forPath(Paths.ADMIN_SETTINGS)}" method="post"
              autocomplete="off">
            <input type="hidden" name="formType" value="saveSettings">

            <c:forEach var="setting" items="${AdminDAO.getSystemSettings()}">
                <%--@elvariable id="setting" type="org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO"--%>
                <c:set var="readableName" value="${setting.name.readableName}"/>
                <c:set var="explanation" value="${setting.name.toString()}"/>
                <c:set var="settingId" value="${setting.name.name()}"/>

                <%-- Do not show Killmap Setting here. --%>
                <c:if test="${setting.name != AUTOMATIC_KILLMAP_COMPUTATION}">
                    <div class="row mb-3">
                        <label class="col-4 col-form-label" id="class-label"
                               for="${settingId}"
                               title="${explanation}">
                            ${readableName}
                        </label>
                        <c:choose>
                            <c:when test="${setting.type == STRING_VALUE}">
                                <c:choose>
                                    <c:when test="${setting.name == SITE_NOTICE || setting.name == PRIVACY_NOTICE || setting.name == CONTACT_NOTICE}">
                                        <div class="col-8">
                                            <textarea class="form-control" rows="3"
                                                      name="${settingId}"
                                                      id="${settingId}">${setting.stringValue}</textarea>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="col-8">
                                            <input type="${settingId.contains("PASSWORD") ? 'password' : 'text'}"
                                                   class="form-control"
                                                   name="${settingId}"
                                                   id="${settingId}"
                                                   value="${setting.stringValue}">
                                            <c:if test="${settingId.startsWith('EMAIL')}">
                                                <div class="invalid-feedback">
                                                    This setting is required for sending emails.
                                                    Please provide a valid value, or disable emails.
                                                </div>
                                            </c:if>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:when test="${setting.type == BOOL_VALUE}">
                                <div class="col-8 d-flex align-items-center">
                                    <div class="form-check form-switch">
                                        <input type="checkbox"
                                               class="form-check-input"
                                               id="${settingId}"
                                               name="${settingId}"
                                               ${setting.boolValue ? 'checked' : ''}>
                                        <label class="form-check-label" for="${settingId}">
                                            ${readableName}
                                        </label>
                                    </div>
                                </div>
                            </c:when>
                            <c:when test="${setting.type == INT_VALUE}">
                                <div class="col-8">
                                    <input type="number"
                                           class="form-control"
                                           name="${settingId}"
                                           id="${settingId}"
                                           value="${setting.intValue}">
                                    <c:if test="${settingId.startsWith('EMAIL')}">
                                        <div class="invalid-feedback">
                                            This setting is required for sending emails.
                                            Please provide a valid value, or disable emails.
                                        </div>
                                    </c:if>
                                </div>
                            </c:when>
                        </c:choose>
                    </div>
                </c:if>
            </c:forEach>

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
                const emailSwitch = document.getElementById('${EMAILS_ENABLED.name()}');
                const otherEmailInputs = [
                    document.getElementById('${EMAIL_ADDRESS.name()}'),
                    document.getElementById('${EMAIL_PASSWORD.name()}'),
                    document.getElementById('${EMAIL_SMTP_HOST.name()}'),
                    document.getElementById('${EMAIL_SMTP_PORT.name()}')
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
                const teacherApplicationsSwitch = document.getElementById('${TEACHER_APPLICATIONS_ENABLED.name()}');
                const teacherApplicationsEmail = document.getElementById('${TEACHER_APPLICATIONS_EMAIL.name()}');

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
</p:main_page>

