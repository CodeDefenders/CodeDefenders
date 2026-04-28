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
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.UserInfo" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.LinkUtils" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.auth.roles.AuthRole" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.codedefenders.auth.roles.AuthRole" %>
<%@ page import="org.codedefenders.auth.roles.TeacherRole" %>
<%@ page import="org.codedefenders.auth.roles.AdminRole" %>
<%@ page import="org.codedefenders.dto.User" %>
<%@ page import="java.util.function.Function" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="settingsRepository" type="org.codedefenders.persistence.database.SettingsRepository"--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    @SuppressWarnings("unchecked")
    List<UserInfo> allUserInfos = (List<UserInfo>) request.getAttribute("userInfos");
    @SuppressWarnings("unchecked")
    Multimap<Integer, AuthRole> allUserRoles = (Multimap<Integer, AuthRole>) request.getAttribute("userRoles");
    pageContext.setAttribute("allUserInfos", allUserInfos);
    pageContext.setAttribute("allUserRoles", allUserRoles);

    int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
    pageContext.setAttribute("pwMinLength", pwMinLength);

    User editedUser = (User) request.getAttribute("editedUser");
    if (editedUser != null) {
        boolean isTeacher = allUserRoles.get(editedUser.getId()).contains(new TeacherRole());
        boolean isAdmin = allUserRoles.get(editedUser.getId()).contains(new AdminRole());
        pageContext.setAttribute("editedUser", editedUser);
        pageContext.setAttribute("editedUserRoles", allUserRoles);
        pageContext.setAttribute("isTeacher", isTeacher);
        pageContext.setAttribute("isAdmin", isAdmin);
    }

    Function<List<AuthRole>, String> joinRoles = roles -> roles.stream()
            .map(AuthRole::getName)
            .collect(Collectors.joining(", "));
    pageContext.setAttribute("joinRoles", joinRoles);
%>

<p:main_page title="${i18n.tr('User Management')}">
    <div class="container">
        <t:admin_navigation activePage="adminUserMgmt"/>

        <c:if test="${editedUser != null}">
            <h3>${i18n.tr('Editing User {0}', editedUser.id)}</h3>

            <form id="editUser" action="${url.forPath(Paths.ADMIN_USERS)}" method="post"
                  class="needs-validation form-width mb-4"
                  autocomplete="off">
                <input type="hidden" name="formType" value="editUser">
                <input type="hidden" name="uid" value="${editedUser.id}">

                <div class="row g-3">
                    <div class="col-12">
                        <label for="name" class="form-label">${i18n.tr('Username')}</label>
                        <input id="name" type="text" class="form-control" name="name" value="${editedUser.name}"
                               placeholder="${i18n.tr('Username')}"
                               required minlength="3" maxlength="20" pattern="[a-z][a-zA-Z0-9]*" autofocus>
                        <div class="invalid-feedback">
                                ${i18n.tr('Please enter a valid username.')}
                        </div>
                        <div class="form-text">
                                ${i18n.tr('3-20 alphanumerics starting with a lowercase letter (a-z), no space or special characters.')}
                        </div>
                    </div>

                    <div class="col-12">
                        <label for="email" class="form-label">${i18n.tr('Email')}</label>
                        <input id="email" type="email" class="form-control" name="email" value="${editedUser.email}"
                               placeholder="${i18n.tr('Email')}"
                               required>
                        <div class="invalid-feedback">
                                ${i18n.tr('Please enter a valid email address.')}
                        </div>
                    </div>

                    <div class="col-12">
                        <label for="email" class="form-label">${i18n.tr('Roles')}</label>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="" id="role-teacher" name="role-teacher"
                                   autocomplete="off" ${isTeacher ? 'checked' : ''}>
                            <label class="form-check-label" for="role-teacher">${i18n.tr('Teacher')}</label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="" id="role-admin" name="role-admin"
                                   autocomplete="off" ${isAdmin ? 'checked' : ''}>
                            <label class="form-check-label" for="role-admin">${i18n.tr('Admin')}</label>
                        </div>
                    </div>

                    <div class="col-12">
                        <div class="mb-2">
                            <label for="password" class="form-label">${i18n.tr('Password')}</label>
                            <input id="password" type="password" class="form-control"
                                   name="password" placeholder="${i18n.tr('Password')}"
                                   minlength="${pwMinLength}" maxlength="20" pattern="[a-zA-Z0-9]*"
                                   autocomplete="new-password">
                            <div class="invalid-feedback">
                                    ${i18n.tr('Please enter a valid password.')}
                            </div>
                        </div>

                        <div>
                            <input id="confirm_password" type="password" class="form-control"
                                   name="confirm_password" placeholder="${i18n.tr('Confirm Password')}"
                                   aria-label="${i18n.tr('Confirm Password')}"
                                   autocomplete="new-password">
                            <div class="invalid-feedback" id="confirm-password-feedback">
                                    ${i18n.tr('Please confirm your password.')}
                            </div>
                            <div class="form-text">
                                    ${i18n.tr('{0}-20 alphanumeric characters, no whitespace or special characters.', pwMinLength)}<br>
                                    ${i18n.tr('Leave empty to keep unchanged.')}
                            </div>
                        </div>
                    </div>

                    <div class="col-12">
                        <a href="${url.forPath(Paths.ADMIN_USERS)}"
                           class="btn btn-secondary me-2">${i18n.tr('Cancel')}</a>
                        <button type="submit" class="btn btn-primary" name="submit_edit_user"
                                id="submit_edit_user">${i18n.tr('Save')}</button>
                    </div>
                </div>

                <script type="module">
                    import $ from '${url.forPath("/js/jquery.mjs")}';


                    $(document).ready(() => {
                        const passwordInput = document.getElementById('password');
                        const confirmPasswordInput = document.getElementById('confirm_password');
                        const confirmPasswordFeedback = document.getElementById('confirm-password-feedback');

                        const validateConfirmPassword = function () {
                            if (passwordInput.value === confirmPasswordInput.value)  {
                                confirmPasswordInput.setCustomValidity('');
                                confirmPasswordFeedback.innerText = '';
                            } else {
                                confirmPasswordInput.setCustomValidity('password-mismatch');
                                confirmPasswordFeedback.innerText = i18n.tr("Passwords don't match.");
                            }
                        };

                        passwordInput.addEventListener('input', validateConfirmPassword);
                        confirmPasswordInput.addEventListener('input', validateConfirmPassword);
                    });
                </script>
            </form>
        </c:if>

        <form id="manageUsers" action="${url.forPath(Paths.ADMIN_USERS)}" method="post">
            <input type="hidden" name="formType" value="manageUsers">

            <table id="tableUsers" class="table table-striped table-v-align-middle">
                <thead>
                    <tr>
                        <th>${i18n.tr('ID')}</th>
                        <th>${i18n.tr('User')}</th>
                        <th>${i18n.tr('Email')}</th>
                        <th>${i18n.tr('Roles')}</th>
                        <th>${i18n.tr('Total Score')}</th>
                        <th>${i18n.tr('Last Login')}</th>
                        <th></th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="userInfo" items="${allUserInfos}">
                        <%--@elvariable id="userInfo" type="org.codedefenders.model.UserInfo"--%>
                        <c:set var="user" value="${userInfo.user}"/>
                        <%--@elvariable id="user" type="org.codedefenders.model.UserEntity"--%>
                        <c:set var="rolesStr" value="${joinRoles.apply(allUserRoles.get(user.id))}"/>

                        <tr id="user_row_${user.id}" ${user.active ? '' : 'class="text-muted"'}>
                            <td>
                                ${user.id}
                                <input type="hidden" name="added_uid" value=${user.id}>
                            </td>
                            <td>${LinkUtils.getUserProfileAnchorOrText(user.username)}</td>
                            <td>${user.email}</td>
                            <td>${rolesStr}</td>
                            <td>${userInfo.totalScore}</td>
                            <td>${userInfo.lastLogin}</td>
                            <td>
                                <button class="btn btn-sm btn-primary" id="edit_user_${user.id}" name="editUserInfo"
                                        type="submit" value="${user.id}">
                                    <i class="fa fa-edit"></i>
                                </button>
                            </td>
                            <td>
                                <c:if test="${login.userId != user.id}">
                                    <button class="btn btn-sm btn-danger" id="inactive_user_${user.id}" type="submit" value="${user.id}" name="setUserInactive"
                                            <c:if test="${!user.active}">
                                                title="${i18n.tr("User is already set inactive.")}" disabled
                                            </c:if>
                                            onclick="return confirm('${i18n.tr("Are you sure you want to set {0}\'s account to inactive?", user.username)}');">
                                        <i class="fa fa-power-off"></i>
                                    </button>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>

            <script type="module">
                import DataTable from '${url.forPath("/js/datatables.mjs")}';
                import {DataTablesUtils} from '${url.forPath("/js/codedefenders_main.mjs")}';
                import $ from '${url.forPath("/js/jquery.mjs")}';


                $(document).ready(function () {
                    new DataTable('#tableUsers', {
                        searching: true,
                        order: [[4, "desc"]],
                        "columnDefs": [{
                            "targets": 5,
                            "orderable": false
                        }, {
                            "targets": 6,
                            "orderable": false
                        }],
                        scrollY: '800px',
                        scrollCollapse: true,
                        paging: false,
                        language: DataTablesUtils.language({info: "${i18n.tr('Showing _TOTAL_ entries')}"})
                    });
                });
            </script>
        </form>

        <h3 class="mt-4 mb-3">${i18n.tr('Create Accounts')}</h3>

        <form id="createUsers" action="${url.forPath(Paths.ADMIN_USERS)}" method="post" autocomplete="off">
            <input type="hidden" name="formType" value="createUsers">

            <div class="row g-3">
                <div class="col-12">
                    <label for="user_name_list" class="form-label">
                        <a data-bs-toggle="modal" data-bs-target="#user-info-format" class="text-decoration-none text-reset cursor-pointer">
                                ${i18n.tr('List of user credentials')}
                            <span class="fa fa-question-circle ms-1"></span>
                        </a>
                    </label>
                    <textarea class="form-control" rows="5" id="user_name_list" name="user_name_list"
                              oninput="document.getElementById('submit_users_btn').disabled = this.value.length === 0;"></textarea>
                </div>

                <div class="col-12">
                    <button class="btn btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled>
                            ${i18n.tr('Create Accounts')}
                    </button>
                </div>
            </div>

        </form>

        <t:modal title="${i18n.tr('User Info Format Explanation')}" id="user-info-format">
            <jsp:attribute name="content">
                <p>${i18n.tr('List of usernames, passwords and (optional) emails.')}</p>
                <ul>
                    <li>${i18n.tr('Fields are separated by commas (<code>,</code>) or semicolons (<code>;</code>).')}</li>
                    <li>${i18n.tr('Users are separated by new lines.')}</li>
                    <li>${i18n.tr('If an email is provided and sending emails is enabled, created users receive an email with their credentials.')}</li>
                    <li>${i18n.tr('The password rules are: At least {0} alphanumeric characters (a-z, A-Z, 0-9) without whitespaces.', settingsRepository.minPasswordLength)}
                    </li>
                    <li>${i18n.tr('Please consider that you can\'t reuse a username from an inactive user.')}</li>
                </ul>
                <p class="mb-2">${i18n.tr('Valid input format examples:')}</p>
                <pre class="bg-light p-3 m-0"><code>username,password
username2,password,example@mail.com
username3;password
username4;password;example@mail.com</code></pre>
            </jsp:attribute>
        </t:modal>
    </div>
</p:main_page>
