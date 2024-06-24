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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="teacherApplicationTemplate" type="org.codedefenders.beans.contact.TeacherApplicationTemplate"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.*" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Contact Us"); %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    boolean teacherApplicationsEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_ENABLED).getBoolValue();
    String teacherApplicationsEmail = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL).getStringValue();
    pageContext.setAttribute("teacherApplicationsEnabled", teacherApplicationsEnabled);
    pageContext.setAttribute("teacherApplicationsEmail", teacherApplicationsEmail);
%>

<% if (login.isLoggedIn()) { %>
	<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
	<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<%
    boolean emailsEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
    String contactNotice = AdminDAO.getSystemSetting(CONTACT_NOTICE).getStringValue();
    pageContext.setAttribute("emailsEnabled", emailsEnabled);
    pageContext.setAttribute("contactNotice", contactNotice);
%>

<div class="container form-width">

	<h2>${pageInfo.pageTitle}</h2>

    <div class="mb-4">
        <c:choose>
            <c:when test="${!contactNotice.blank}">
                <c:out value="${contactNotice}" escapeXml="false"/>
            </c:when>
            <c:otherwise>
                You can use the form below to contact the site owners.
                For technical questions about Code Defenders, please contact the developers.
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${emailsEnabled}">
        <form action="${url.forPath(Paths.API_SEND_EMAIL)}" method="post" class="needs-validation w-75">
            <input type="hidden" name="formType" value="login">

            <div class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="name-input">Name</label>
                    <input type="text" id="name-input" name="name" class="form-control" placeholder="Name" required autofocus>
                </div>

                <div class="col-12">
                    <label class="form-label" for="email-input">Email</label>
                    <input type="email" id="email-input" name="email" class="form-control" placeholder="Email" required>
                </div>

                <div class="col-12">
                    <label class="form-label" for="subject-input">Subject</label>
                    <input type="text" id="subject-input" name="subject" class="form-control" placeholder="Subject" required>
                </div>

                <div class="col-12">
                    <label class="form-label" for="message-input">Message</label>
                    <textarea id="message-input" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
                </div>

                <div class="col-12">
                    <button class="btn btn-primary" type="submit">
                        <i class="fa fa-envelope me-2" aria-hidden="true"></i>
                        Send
                    </button>
                </div>
            </div>
        </form>
    </c:if>

    <c:if test="${teacherApplicationsEnabled}">
        <h4 class="mt-5">Teacher Account Applications</h4>
        <c:choose>
            <c:when test="${empty teacherApplicationsEmail}">
                Error: No email address was configured for teacher account applications.
            </c:when>
            <c:when test="${!login.loggedIn}">
                <p>
                    A teacher account lets you create classrooms for your students, so you can manage games more easily.
                    Being logged-in is required to apply for a teacher account.
                    Please log in to your account and come back to this page.
                </p>
            </c:when>
            <c:otherwise>
                <p>
                    A teacher account lets you create classrooms for your students, so you can manage games more easily.
                    If you're interested in turning your account into a teacher account, please use the template below
                    to email us.
                    You can also use this
                    <a href="<c:out value="${teacherApplicationTemplate.mailtoLink}" escapeXml="false"/>">mailto link</a>
                    to automatically create an email with the template.
                </p>
                <a id="show-email-template" class="btn btn-sm btn-outline-secondary" role="button">
                    <i class="fa fa-envelope me-2" aria-hidden="true"></i>
                    Show template
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
                                <td>To:</td>
                                <td>${teacherApplicationTemplate.address}</td>
                            </tr>
                            <tr>
                                <td>Subject:</td>
                                <td>${teacherApplicationTemplate.subject}</td>
                            </tr>
                            <tr>
                                <td>Body:</td>
                                <td>
                                    <p class="mb-0" style="white-space: pre-wrap;">${teacherApplicationTemplate.body}</p>
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

    <h4 class="mt-5">Contact the Developers</h4>
    <p>
        Code Defenders is an open source project.
        Please check out the
        <a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a>
        project page for more details.
    </p>
</div>

<%@ include file="/jsp/footer.jsp" %>
