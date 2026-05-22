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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="i18nService" type="org.codedefenders.service.I18nService"--%>
<%--@elvariable id="language" type="java.lang.String"--%>
<%--@elvariable id="settings" type="java.util.List"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.util.Paths" %>

<p:main_page title="${i18n.tr('Text Settings')}">

    <div class="container">
        <t:admin_navigation activePage="adminTextSettings"/>

        <form id="textSettings"
              name="textSettings"
              action="${url.forPath(Paths.ADMIN_TEXT_SETTINGS)}"
              method="post"
              autocomplete="off">
            <input type="hidden" name="formType" value="saveTextSettings">

            <c:set var="supportedLocales" value="${i18nService.supportedLocales}"/>
            <c:if test="${fn:length(supportedLocales) > 1}">
                <div class="row mb-4 align-items-center">
                    <label for="language-switch" class="col-4 col-form-label">${i18n.tr("Select a language")}</label>
                    <div class="col-8">
                        <select id="language-switch" class="form-control" name="language">
                            <c:forEach items="${supportedLocales}" var="l">
                                <option value="${l.language}"
                                        <c:if test="${language == l.language}">selected</c:if>
                                >
                                    <c:choose>
                                        <c:when test="${login.user.locale == l}">
                                            ${l.getDisplayLanguage(login.user.locale)}
                                        </c:when>
                                        <c:otherwise>
                                            ${l.getDisplayLanguage(login.user.locale)} (${l.getDisplayLanguage(l)})
                                        </c:otherwise>
                                    </c:choose>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <hr>
            </c:if>

            <c:forEach var="setting" items="${settings}">
                <%--@elvariable id="setting" type="org.codedefenders.dto.TextSetting"--%>
                <c:set var="readableName" value="${i18n.tr(setting.name().readableName)}"/>
                <c:set var="explanation" value="${i18n.tr(setting.name().description)}"/>
                <c:set var="settingId" value="${setting.name().name()}"/>

                <div class="mb-4">
                    <label class="" for="${settingId}">
                        <strong>${readableName}</strong>
                        <br>
                        <small>${explanation}</small>
                    </label>
                    <div class="">
                        <textarea
                                class="form-control"
                                rows="3"
                                name="${settingId}"
                                id="${settingId}"
                                data-original-value="${setting.value()}"
                        >${setting.value()}</textarea>
                    </div>
                </div>
            </c:forEach>

            <div class="row g-2">
                <div class="col-auto">
                    <button type="submit" class="btn btn-primary" name="saveSettingsBtn"
                            id="saveSettingsBtn">${i18n.tr('Save')}</button>
                </div>
                <div class="col-auto">
                    <button type="button" class="btn btn-secondary" id="cancelBtn"
                            onclick="window.location.reload();">${i18n.tr('Cancel')}</button>
                </div>
            </div>
        </form>

        <script type="module">
            const languageSwitch = document.getElementById("language-switch");
            languageSwitch.addEventListener("change", function (e) {
                const selectedLanguage = e.target.value;
                const changeLanguage = !formHasChanges() || confirm('${i18n.tr(
                    "Changing the language will reload the page and discard any unsaved changes. Do you want to continue?")}');

                if (changeLanguage) {
                    const url = new URL(window.location.href);
                    url.searchParams.set("language", selectedLanguage);
                    window.location.href = url.toString();
                } else {
                    // revert the selection back to the original language
                    e.target.value = '${language}';
                }
            });

            function formHasChanges() {
                const ts = document.querySelectorAll("#textSettings textarea");
                for (const textarea of ts) {
                    if (textarea.value !== textarea.dataset.originalValue) {
                        return true;
                    }
                }
                return false;
            }
        </script>
    </div>
</p:main_page>
