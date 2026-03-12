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
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="teacherApplicationTemplate" type="org.codedefenders.beans.contact.TeacherApplicationTemplate"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.*" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>

<%
    boolean emailsEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
    String contactNotice = AdminDAO.getSystemSetting(CONTACT_NOTICE).getStringValue();
    boolean teacherApplicationsEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_ENABLED).getBoolValue();
    String teacherApplicationsEmail = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL).getStringValue();
    pageContext.setAttribute("emailsEnabled", emailsEnabled);
    pageContext.setAttribute("contactNotice", contactNotice);
    pageContext.setAttribute("teacherApplicationsEnabled", teacherApplicationsEnabled);
    pageContext.setAttribute("teacherApplicationsEmail", teacherApplicationsEmail);
%>
<c:set var="title" value="${i18n.tr('Contact Us')}"/>

<p:main_page title="${title}">
    <div class="container form-width">

        <h2>${title}</h2>

        <div class="mb-4">
            <c:choose>
                <c:when test="${!contactNotice.blank}">
                    <c:out value="${contactNotice}" escapeXml="false"/>
                </c:when>
                <c:otherwise>
                    ${i18n.tr('You can use the form below to contact the site owners.')}
                    ${i18n.tr('For technical questions about Code Defenders, please contact the developers.')}
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${emailsEnabled}">
            <form action="${url.forPath(Paths.API_SEND_EMAIL)}" method="post" class="needs-validation w-75">
                <input type="hidden" name="formType" value="login">

                <div class="row g-3">
                    <div class="col-12">
                        <label class="form-label" for="name-input">${i18n.tr('Name')}</label>
                        <input type="text" id="name-input" name="name" class="form-control"
                               placeholder="${i18n.tr('Name')}" required autofocus>
                    </div>

                    <div class="col-12">
                        <label class="form-label" for="email-input">${i18n.tr('Email')}</label>
                        <input type="email" id="email-input" name="email" class="form-control"
                               placeholder="${i18n.tr('Email')}" required>
                    </div>

                    <div class="col-12">
                        <label class="form-label" for="subject-input">${i18n.tr('Subject')}</label>
                        <input type="text" id="subject-input" name="subject" class="form-control"
                               placeholder="${i18n.tr('Subject')}" required>
                    </div>
                    <div class="col-12">
                        <label class="form-label" for="message-input">${i18n.tr('Message')}</label>
                        <textarea id="message-input" name="message" class="form-control"
                                  placeholder="${i18n.tr('Message')}" rows="8" required></textarea>
                    </div>
                    <div class="col-12">
                        <button class="btn btn-primary" type="submit">
                            <i class="fa fa-envelope me-2" aria-hidden="true"></i>
                                ${i18n.tr('Send')}
                        </button>
                    </div>
                </div>
            </form>
        </c:if>

        <c:if test="${teacherApplicationsEnabled}">
            <h4 class="mt-5">${i18n.tr('Teacher Account Applications')}</h4>
            <c:choose>
                <c:when test="${empty teacherApplicationsEmail}">
                    ${i18n.tr('Error: No email address was configured for teacher account applications.')}
                </c:when>
                <c:when test="${!login.loggedIn}">
                    <p>
                            ${i18n.tr('A teacher account lets you create classrooms for your students, so you can manage games more easily. Being logged-in is required to apply for a teacher account. Please log in to your account and come back to this page.')}
                    </p>
                </c:when>
                <c:otherwise>
                    <p>
                            ${i18n.tr('A teacher account lets you create classrooms for your students, so you can manage games more easily. If you\'re interested in turning your account into a teacher account, please use the template below to email us. You can also use this <a href=\'{0}\'>mailto link</a> to automatically create an email with the template.', teacherApplicationTemplate.getMailtoLink(i18n))}
                    </p>
                    <a id="show-email-template" class="btn btn-sm btn-outline-secondary" role="button">
                        <i class="fa fa-envelope me-2" aria-hidden="true"></i>
                            ${i18n.tr('Show template')}
                    </a>
                    <div id="email-template" class="fade" hidden>
                        <div class="bg-light p-4 border rounded mt-3">
                            <style>
                                table td:nth-child(1) {
                                    padding-right: 1em;
                                    font-weight: bold;
                                }
                                table td {
                                    vertical-align: top;
                                    padding-bottom: .2em;
                                }
                            </style>
                            <table>
                                <tr>
                                    <td>${i18n.tr('To:')}</td>
                                    <td>${teacherApplicationTemplate.address}</td>
                                </tr>
                                <tr>
                                    <td>${i18n.tr('Subject:')}</td>
                                    <td>${teacherApplicationTemplate.getSubject(i18n)}</td>
                                </tr>
                                <tr>
                                    <td>${i18n.tr('Body:')}</td>
                                    <td>
                                        <p class="mb-0" style="white-space: pre-wrap;">${teacherApplicationTemplate.getBody(i18n)}</p>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                    <script>
                        const button = document.getElementById("show-email-template");
                        const template = document.getElementById("email-template");
                        // Show the template and hide the button on click.
                        button.addEventListener('click', function(event) {
                            template.removeAttribute('hidden')
                            /*
                             * Force a browser re-paint so the browser will realize the element is no longer `hidden` and
                             * allow transitions. (workaround taken from cloudfour.com/thinks/transitioning-hidden-elements)
                             */
                            const reflow = template.offsetHeight;
                            template.classList.add('show');
                            button.setAttribute('hidden', '')
                        });
                    </script>
                </c:otherwise>
            </c:choose>
        </c:if>

        <h4 class="mt-5">${i18n.tr('Contact the Developers')}</h4>
        <p>
                ${i18n.tr('Code Defenders is an open source project.')}
                ${i18n.tr('Please check out the')} <a
                href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> ${i18n.tr('project page for more details.')}
        </p>
    </div>
</p:main_page>
