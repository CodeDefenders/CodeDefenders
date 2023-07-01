<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.model.ClassroomRole" %>

<%--@elvariable id="login" type="org.codedefenders.service.AuthService"--%>
<%--@elvariable id="classroomService" type="org.codedefenders.service.ClassroomService"--%>
<%--@elvariable id="userService" type="org.codedefenders.service.UserService"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<c:set var="classroom" value="${requestScope.classroom}"/>
<c:set var="member" value="${requestScope.member}"/>
<c:set var="link" value="${requestScope.link}"/>

<c:set var="canEdit" value="${(member != null && member.role == ClassroomRole.OWNER) || login.admin}"/>
<c:set var="canLeave" value="${member != null && member.role != ClassroomRole.OWNER}"/>

<c:set var="disabledIfArchved" value="${classroom.archived ? 'disabled' : ''}"/>
<c:set var="mutedIfArchved" value="${classroom.archived ? 'text-muted' : ''}"/>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">

    <h2 class="d-flex align-items-center gap-3 mb-4 ms-4">
        <c:out value="${classroom.name}"/>
        <c:if test="${classroom.archived}">
            <span class="badge bg-secondary">Archived</span>
        </c:if>
    </h2>

    <div class="p-4 border rounded mb-4 loading loading-height-200">
        <div class="d-flex justify-content-between flex-wrap align-items-baseline">
            <h4 class="mb-3">Games</h4>
            <div class="d-flex gap-3">
                <div>
                    <input type="radio" class="btn-check" name="classroom-type" id="radio-active" autocomplete="off" checked>
                    <label class="btn btn-sm btn-outline-secondary rounded-pill" for="radio-active">Active</label>

                    <input type="radio" class="btn-check" name="classroom-type" id="radio-archived" autocomplete="off">
                    <label class="btn btn-sm btn-outline-secondary rounded-pill" for="radio-archived">Archived</label>
                </div>
                <input type="search" id="search-games" placeholder="Search"
                       class="form-control form-control-sm" style="width: 15em;">
            </div>
        </div>
        <table id="games-table" class="table table-no-last-border" style="width: 100%;"></table>
    </div>

    <div class="row g-4">

        <%-- Members table --%>
        <div class="col-lg-6 col-12">
            <div class="p-4 border rounded loading loading-height-200">
                <div class="d-flex justify-content-between flex-wrap align-items-baseline">
                    <h4 class="mb-3">Members</h4>
                    <input type="search" id="search-members" placeholder="Search"
                           class="form-control form-control-sm" style="width: 15em;">
                </div>
                <table id="members-table" class="table table-no-last-border" style="width: 100%;"></table>
            </div>
        </div>

        <%-- Settigns --%>
        <div class="col-lg-6 col-12 d-flex flex-column gap-4">

            <%-- Join as admin --%>
            <c:if test="${login.admin && (member == null || member.role != ClassroomRole.OWNER)}">
                <div class="p-4 border rounded ${mutedIfArchved}">
                    <c:choose>
                        <c:when test="${member == null}">
                            You are able to view and edit this classroom because you are logged in as admin.
                        </c:when>
                        <c:otherwise>
                            You are able to edit this classroom because you are logged in as admin.
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${classroom.open && !classroom.archived && member == null}">
                        If you would like to join the classroom, please go
                        <a href="${url.forPath(Paths.CLASSROOM)}?classroomUid=${classroom.UUID}&join">here</a>.
                    </c:if>
                </div>
            </c:if>

            <%-- Join Settigns --%>
            <c:if test="${canEdit}">
                <div class="p-4 border rounded ${mutedIfArchved}">

                    <h4 class="mb-4">Join Settings</h4>

                    <div class="d-flex gap-2 mb-1">
                        <c:choose>
                            <c:when test="${classroom.open}">
                                <span>Joining is <span class="text-success">enabled</span>.</span>
                                <button id="disable-joining" class="btn btn-xs btn-secondary"
                                    data-bs-toggle="modal" data-bs-target="#disable-joining-modal"
                                    ${disabledIfArchved}>
                                    Disable Joining
                                </button>
                            </c:when>
                            <c:otherwise>
                                <span>Joining is <span class="text-danger">disabled</span>.</span>
                                <button id="enable-joining" class="btn btn-xs btn-secondary"
                                        data-bs-toggle="modal" data-bs-target="#enable-joining-modal"
                                        ${disabledIfArchved}>
                                    Enable Joining
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <div class="d-flex gap-2 mb-1">
                        <c:choose>
                            <c:when test="${classroom.visible}">
                                <span>Visibility is <span class="text-success">public</span>.</span>
                                <button id="disable-joining" class="btn btn-xs btn-secondary"
                                        data-bs-toggle="modal" data-bs-target="#make-private-modal"
                                        ${disabledIfArchved}>
                                    Make Private
                                </button>
                            </c:when>
                            <c:otherwise>
                                <span>Visibility is <span class="text-danger">private</span>.</span>
                                <button id="enable-joining" class="btn btn-xs btn-secondary"
                                        data-bs-toggle="modal" data-bs-target="#make-public-modal"
                                        ${disabledIfArchved}>
                                    Make Public
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <div class="d-flex gap-2 mb-3">
                        <c:choose>
                            <c:when test="${classroom.password.isPresent()}">
                                <span>Password is <span class="text-success">set</span>.</span>
                            </c:when>
                            <c:otherwise>
                                <span>Password is <span class="text-danger">not set</span>.</span>
                            </c:otherwise>
                        </c:choose>
                        <div class="d-flex gap-0">
                            <button id="set-password" class="btn btn-xs btn-secondary me-1"
                                    data-bs-toggle="modal" data-bs-target="#set-password-modal"
                                    ${disabledIfArchved}>
                                Set Password
                            </button>
                            <c:if test="${classroom.password.isPresent()}">
                                <button id="remove-password" class="btn btn-xs btn-secondary"
                                        data-bs-toggle="modal" data-bs-target="#remove-password-modal"
                                        ${disabledIfArchved}>
                                    Remove Password
                                </button>
                            </c:if>
                        </div>
                    </div>

                    <div>
                        <span class="me-1">Invite link is</span>
                        <span id="invite-link" class="border rounded px-2"><c:out value="${link}"/></span>
                        <i class="fa fa-clipboard copy cursor-pointer text-primary ms-1"
                           data-copy-target="#invite-link"></i>
                    </div>

                </div>
            </c:if>

            <%-- Classroom Settigns --%>
            <c:if test="${canEdit}">
                <div class="p-4 border rounded ${mutedIfArchved}">

                    <h4 class="mb-4">Classroom Settings</h4>

                    <div class="mb-1">
                        <button id="change-name" class="btn btn-xs btn-secondary"
                                data-bs-toggle="modal" data-bs-target="#change-name-modal"
                                ${disabledIfArchved}>
                            Change Name
                        </button>
                    </div>

                    <div>
                        <c:choose>
                            <c:when test="${classroom.archived}">
                                <button id="change-name" class="btn btn-xs btn-success"
                                        data-bs-toggle="modal" data-bs-target="#restore-classroom-modal">
                                    Restore Classroom
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button id="change-name" class="btn btn-xs btn-danger"
                                        data-bs-toggle="modal" data-bs-target="#archive-classroom-modal">
                                    Archive Classroom
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </div>

                </div>
            </c:if>

            <%-- Member Actions --%>
            <c:if test="${canLeave}">
                <div class="p-4 border rounded ${mutedIfArchved}">

                    <h4 class="mb-4">Member Actions</h4>

                    <div>
                        <button id="leave" class="btn btn-xs btn-secondary"
                                data-bs-toggle="modal" data-bs-target="#leave-modal">
                            Leave Classroom
                        </button>
                    </div>

                </div>
            </c:if>

        </div>
    </div>

    <%-- Change classroom name modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
            <input type="hidden" name="action" value="change-name"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Change classroom name" id="change-name-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    <label for="name-input" class="form-label">Name</label>
                    <input type="text" class="form-control" id="name-input" name="name"
                           value="<c:out value="${classroom.name}"/>"
                           required maxlength="100"
                           placeholder="Name">
                    <div class="invalid-feedback">
                        Please enter a valid name.
                    </div>
                    <div class="form-text">
                        Maximum length: 100 characters.
                    </div>
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Save changes</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Set password modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
            <input type="hidden" name="action" value="set-password"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Set password" id="set-password-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    <div class="mb-2">
                        <label for="password-input" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password-input" name="password"
                               placeholder="Password"
                               required maxlength="100">
                        <div class="invalid-feedback">
                            Please enter a valid password.
                        </div>
                    </div>
                    <div>
                        <input type="password" class="form-control" id="confirm-password-input"
                               name="confirm" placeholder="Confirm Password" required>
                        <div class="invalid-feedback" id="confirm-password-feedback">
                            Please confirm your password.
                        </div>
                        <div class="form-text">
                            Maximum length: 100 characters.
                        </div>
                    </div>
                    <script>
                        const passwordInput = document.getElementById('password-input');
                        const confirmPasswordInput = document.getElementById('confirm-password-input');
                        const confirmPasswordFeedback = document.getElementById('confirm-password-feedback');

                        const validateConfirmPassword = function () {
                            if (confirmPasswordInput.validity.valueMissing) {
                                confirmPasswordFeedback.innerText = 'Please confirm your password.';
                            } else {
                                if (passwordInput.value === confirmPasswordInput.value)  {
                                    confirmPasswordInput.setCustomValidity('');
                                    confirmPasswordFeedback.innerText = '';
                                } else {
                                    confirmPasswordInput.setCustomValidity('password-mismatch');
                                    confirmPasswordFeedback.innerText = "Passwords don't match.";
                                }
                            }
                        };

                        passwordInput.addEventListener('input', validateConfirmPassword);
                        confirmPasswordInput.addEventListener('input', validateConfirmPassword);
                    </script>
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Set password</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Remove password modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="remove-password"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Remove classroom password" id="remove-password-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to remove the password?
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-danger">Remove password</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Enable joining modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="enable-joining"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Enable joining" id="enable-joining-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to enable joining?
                    Students will be able to join via room code or invite link.
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-success">Enable joining</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Disable joining modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="disable-joining"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Disable joining" id="disable-joining-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to disable joining?
                    Students won't be able to join via room code or invite link.
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-danger">Disable joining</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Make public modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="make-public"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Change visibility" id="make-public-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                Are you sure you want to make the classroom publicly visible?
                The classroom will be shown in the public list.
                If the classroom is open, players will be able to join from there.
            </jsp:attribute>
                <jsp:attribute name="footer">
                <button type="submit" class="btn btn-success">Make Public</button>
            </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Make private modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="make-private"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Change visibility" id="make-private-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                Are you sure you want to change the classroom's visibility to private?
                The classroom will no longer be shown in the public list and players won't be able to join from there.
            </jsp:attribute>
                <jsp:attribute name="footer">
                <button type="submit" class="btn btn-danger">Make Private</button>
            </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Archive classroom modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="archive"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Archive classroom" id="archive-classroom-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                Are you sure you want to archive the classroom?
                This will make the classroom read-only and prevent players from joining.
                You can undo this later by restoring the classroom.
            </jsp:attribute>
                <jsp:attribute name="footer">
                <button type="submit" class="btn btn-danger">Archive</button>
            </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Restore classroom modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="restore"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Restore classroom" id="restore-classroom-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                Are you sure you want to restore the classroom?
            </jsp:attribute>
                <jsp:attribute name="footer">
                <button type="submit" class="btn btn-success">Restore</button>
            </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Leave modal --%>
    <c:if test="${!isOwner}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post">
            <input type="hidden" name="action" value="leave"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Leave classroom" id="leave-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                Are you sure you want to leave the classroom?
            </jsp:attribute>
                <jsp:attribute name="footer">
                <button type="submit" class="btn btn-danger">Leave</button>
            </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Change role modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" id="change-role-form">
            <input type="hidden" name="action" value="change-role"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>
            <input type="hidden" name="userId" value=""/>
            <input type="hidden" name="role" value=""/>

            <t:modal title="Change role" id="change-role-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to make
                    <span data-fill="username" class="border rounded px-1"></span>
                    a
                    <span data-fill="role" class="border rounded px-1"></span>
                    ?
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Change role</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Change owner modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" id="change-owner-form">
            <input type="hidden" name="action" value="change-owner"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>
            <input type="hidden" name="userId" value=""/>

            <t:modal title="Change role" id="change-role-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to make
                    <span data-fill="username" class="border rounded px-1"></span>
                    the owner of this classroom?
                    This will change the current owner's role to Moderator.
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Change owner</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Kick member modal --%>
    <c:if test="${canEdit}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" id="kick-member-form">
            <input type="hidden" name="action" value="kick-member"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>
            <input type="hidden" name="userId" value=""/>

            <t:modal title="Kick member" id="kick-member-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want kick
                    <span data-fill="username" class="border rounded px-1"></span>
                    from this classroom?
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-danger">Kick</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {Tooltip, Modal} from '${url.forPath("/js/bootstrap.mjs")}';
        import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';
        import {GameTime} from '${url.forPath("/js/codedefenders_game.mjs")}';

        const STUDENT = '${ClassroomRole.STUDENT.name()}';
        const MODERATOR = '${ClassroomRole.MODERATOR.name()}';
        const OWNER = '${ClassroomRole.OWNER.name()}';

        const MELEE = '${GameMode.MELEE.name()}';
        const PARTY = '${GameMode.PARTY.name()}';

        const ATTACKER = '${Role.ATTACKER.name()}';
        const DEFENDER = '${Role.DEFENDER.name()}';
        const PLAYER = '${Role.PLAYER.name()}';
        const OBSERVER = '${Role.OBSERVER.name()}';
        const NONE = '${Role.NONE.name()}';

        const CREATED = '${GameState.CREATED.name()}';
        const ACTIVE = '${GameState.ACTIVE.name()}';
        const FINISHED = '${GameState.FINISHED.name()}';

        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
        const BATTLEGROUND_URL = '${url.forPath(Paths.BATTLEGROUND_GAME)}';
        const MELEE_URL = '${url.forPath(Paths.MELEE_GAME)}';

        const classroomId = ${classroom.id};
        const ownUserId = ${login.userId};

        const canEdit = ${canEdit ? "true" : "false"};
        const isArchived = ${classroom.archived ? "true" : "false"};

        /**
         * Fetches members from the API.
         */
        const getMembers = async function() {
            const params = new URLSearchParams({
                type: 'members',
                classroomId
            });
            const response = await fetch(`\${API_URL}?\${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                // TODO: Show toast message here? But we don't have toasts :(
                return null;
            }
            return await response.json();
        };

        /**
         * Fetches games from the API.
         */
        const getGames = async function() {
            const params = new URLSearchParams({
                type: 'games',
                classroomId
            });
            const response = await fetch(`\${API_URL}?\${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                // TODO: Show toast message here? But we don't have toasts :(
                return null;
            }
            return await response.json();
        };

        /**
         * Renders roles with correct capitalization and sorts owners first.
         */
        const renderClassroomRole = function(role, type, row, meta) {
            switch (type) {
                case 'type':
                    return role;
                case 'sort':
                    // Sort owner(s) first
                    if (role === OWNER) {
                        return 'a';
                    } else {
                        return role;
                    }
                case 'filter':
                case 'display':
                    // Capitalize first letter, make rest lower case
                    return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
            }
        };

        const renderMemberActions = function(data, type, row, meta) {
            if (isArchived) {
                return `
                    <div class="float-end">
                        <span class="text-muted px-3">
                            <i class="fa fa-ellipsis-v"></i>
                        </span>
                    </div>
                `;
            }

            const div = document.createElement('div');
            div.innerHTML = `
                <div class="float-end">
                    <span class="cursor-pointer px-3 member-actions" data-bs-toggle="dropdown">
                        <i class="fa fa-ellipsis-v"></i>
                    </span>
                    <div class="dropdown-menu">
                        <li><h6 class="dropdown-header">Change Role to ...</h6></li>
                        <li>
                            <a href="#" class="dropdown-item change-role" data-role="STUDENT">
                                <i style="width: 1.25em;" class="fa fa-user text-primary"></i>
                                Student
                            </a>
                            <a href="#" class="dropdown-item change-role" data-role="MODERATOR">
                                <i style="width: 1.25em;" class="fa fa-user text-primary"></i>
                                Moderator
                            </a>
                            <a href="#" class="dropdown-item change-role" data-role="OWNER">
                                <i style="width: 1.25em;" class="fa fa-user text-primary"></i>
                                Owner
                            </a>
                        </li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <a class="dropdown-item kick-member" href="#">
                                <i class="fa fa-trash text-danger" style="width: 1.25em;"></i>
                                Kick
                            </a>
                        </li>
                    </div>
                </div>
            `;

            const changeRoleStudent = div.querySelector('.change-role[data-role="STUDENT"]');
            const changeRoleModerator = div.querySelector('.change-role[data-role="MODERATOR"]');
            const changeRoleOwner = div.querySelector('.change-role[data-role="OWNER"]');
            const kickMember = div.querySelector('.kick-member');

            switch (data.role) {
                case OWNER:
                    changeRoleStudent.classList.add('disabled');
                    changeRoleModerator.classList.add('disabled');
                    changeRoleOwner.setAttribute('hidden', '');
                    kickMember.classList.add('disabled');
                    break;
                case MODERATOR:
                    changeRoleModerator.setAttribute('hidden', '')
                    break;
                case STUDENT:
                    changeRoleStudent.setAttribute('hidden', '');
                    changeRoleOwner.classList.add('disabled');
                    break;
            }

            return div.innerHTML;
        };

        const kickMember = function(data) {
            const form = document.getElementById('kick-member-form');
            form.querySelector('input[name="userId"]').value = data.user.id;
            form.querySelector('[data-fill="username"]').innerText = data.user.name;
            new Modal(form.querySelector('.modal')).show();
        };

        const changeRole = function(data, role) {
            if (role === OWNER) {
                const form = document.getElementById('change-owner-form');
                form.querySelector('input[name="userId"]').value = data.user.id;
                form.querySelector('[data-fill="username"]').innerText = data.user.name;
                new Modal(form.querySelector('.modal')).show();
            } else {
                const roleStr = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
                const form = document.getElementById('change-role-form');
                form.querySelector('input[name="userId"]').value = data.user.id;
                form.querySelector('input[name="role"]').value = role;
                form.querySelector('[data-fill="username"]').innerText = data.user.name;
                form.querySelector('[data-fill="role"]').innerText = roleStr;
                new Modal(form.querySelector('.modal')).show();
            }
        };

        const initMembersTable = function(data) {
            const membersTable = new DataTable('#members-table', {
                data,
                columns: [
                    {
                        data: 'user.id',
                        title: 'User ID',
                        type: 'number',
                        width: '5em',
                        visible: canEdit
                    },
                    {
                        data: 'user.name',
                        title: 'Name',
                        type: 'string'
                    },
                    {
                        data: 'role',
                        title: 'Role',
                        type: 'string',
                        render: renderClassroomRole,
                    },
                    {
                        data: null,
                        title: 'Actions',
                        orderable: false,
                        searchable: false,
                        render: renderMemberActions,
                        type: 'html',
                        width: '4em',
                        visible: canEdit
                    }
                ],
                order: [[2, 'asc'], [1, 'asc']],
                scrollY: '500px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'This classroom has no students... yet.'}
            });
            LoadingAnimation.hideAnimation(membersTable.table().container());

            document.getElementById('search-members').addEventListener('keyup', function(event) {
                setTimeout(() => membersTable.search(this.value).draw(), 0);
            });

            for (const button of membersTable.table().container().getElementsByClassName('change-role')) {
                const data = membersTable.row(button.closest('tr')).data();
                const role = button.dataset.role;
                button.addEventListener('click', e => changeRole(data, role));
            }

            for (const button of membersTable.table().container().getElementsByClassName('kick-member')) {
                const data = membersTable.row(button.closest('tr')).data();
                button.addEventListener('click', e => kickMember(data));
            }
        };

        const renderGameMode = function(mode, type, row, meta) {
            switch (mode) {
                case PARTY:
                    return 'Battleground';
                case MELEE:
                    return 'Melee';
                default:
                    return 'unknown';
            }
        };

        const renderGameRole = function(role, type, row, meta) {
            switch (type) {
                case 'type':
                    return role;
                case 'sort':
                    // Sort owner(s) first
                    if (role === OBSERVER) {
                        return 'a';
                    } else if (role === NONE) {
                        return 'z';
                    }
                case 'filter':
                case 'display':
                    switch (role) {
                        case ATTACKER:
                            return '<span class="badge bg-attacker" style="font-size: .85rem;">Attacker</span>';
                        case DEFENDER:
                            return '<span class="badge bg-defender" style="font-size: .85rem;">Defender</span>';
                        case PLAYER:
                            return '<span class="badge bg-player" style="font-size: .85rem;">Player</span>';
                        case OBSERVER:
                            return '<span class="badge bg-warning text-dark" style="font-size: .85rem;">Observer</span>';
                        case NONE:
                            return '';
                    }
            }
        };

        const renderGameStateActive = function(data, type, row, meta) {
            const timeLeft = GameTime.calculateRemainingTime(data.startTime, data.duration);

            switch (type) {
                case 'sort':
                    return timeLeft;
                case 'filter':
                case 'type':
                    return GameTime.formatTime(timeLeft);
                case 'display':
                    const timeLeftStr = GameTime.formatTime(timeLeft);
                    const percentElapsed = GameTime.calculateElapsedPercentage(data.startTime, data.duration);
                    return `
                        <div style="margin-top: -5px;">
                            <span class="small">Running: \${timeLeftStr} left</span>
                            <div class="progress" style="max-width: 15rem; height: 4px;">
                                <div class="progress-bar" style="width: \${percentElapsed * 100}%;"></div>
                            </div>
                        </div>
                    `;
            }
        };

        const renderGameStateFinished = function(data, type, row, meta) {
            const endDate = GameTime.calculateEndDate(data.startTime, data.duration);

            switch (type) {
                case 'sort':
                    return endDate.getTime();
                case 'filter':
                case 'type':
                    return GameTime.formatDate(endDate);
                case 'display':
                    return 'Ended at ' + GameTime.formatDate(endDate);
            }
        };

        const renderGameState = function(data, type, row, meta) {
            switch (data.state) {
                case CREATED:
                    return 'Not Started';
                case ACTIVE:
                    return renderGameStateActive(data, type, row, meta);
                case FINISHED:
                    return renderGameStateFinished(data, type, row, meta);
                default:
                    return '';
            }
        };

        const renderGameActions = function(data, type, row, meta) {
            if (data.role === NONE) {
                return '';
            }

            let url;
            if (data.mode === PARTY) {
                url = BATTLEGROUND_URL;
            } else if (data.mode === MELEE) {
                url = MELEE_URL;
            } else {
                return '';
            }

            switch (type) {
                case 'type':
                case 'sort':
                case 'filter':
                    return null;
                case 'display':
                    const params = new URLSearchParams({
                        gameId: data.gameId
                    });
                    return `
                        <a href="\${url}?\${params}" class="cursor-pointer float-end px-2">
                            <i class="fa fa-external-link text-primary"></i>
                        </a>
                    `;
            }
        };

        const initGamesTable = function(data) {
            const activeRadio = document.getElementById("radio-active");
            const archivedRadio = document.getElementById("radio-archived");

            const searchFunction = (settings, renderedData, index, data, counter) => {
                /* Let this only affect the "My Classrooms" table. */
                if (settings.nTable.id !== 'games-table') {
                    return true;
                }

                if (activeRadio.checked) {
                    return data.state === CREATED || data.state === ACTIVE;
                } else {
                    return data.state === FINISHED;
                }
            };
            DataTable.ext.search.push(searchFunction);

            activeRadio.addEventListener('change', e => gamesTable.draw());
            archivedRadio.addEventListener('change', e => gamesTable.draw());
            const gamesTable = new DataTable('#games-table', {
                data,
                columns: [
                    {
                        data: 'gameId',
                        title: 'Game ID',
                        type: 'number',
                        width: '5em'
                    },
                    {
                        data: 'mode',
                        title: 'Game Mode',
                        type: 'string',
                        render: renderGameMode
                    },
                    {
                        data: 'role',
                        title: 'Your Role',
                        type: 'html',
                        render: renderGameRole
                    },
                    {
                        data: null,
                        title: 'State',
                        type: 'html',
                        render: renderGameState
                    },
                    {
                        data: null,
                        title: 'Link',
                        type: 'html',
                        render: renderGameActions,
                        width: '2em'
                    }
                ],
                order: [[0, 'asc']],
                scrollY: '500px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'This classroom has no games... yet.'}
            });
            LoadingAnimation.hideAnimation(gamesTable.table().container());

            document.getElementById('search-games').addEventListener('keyup', function(event) {
                setTimeout(() => gamesTable.search(this.value).draw(), 0);
            });
        };

        /**
         * Initializes the buttons to copy room code and invite link.
         */
        const initCopyButtons = function() {
            for (const element of document.getElementsByClassName('copy')) {
                element.addEventListener('click', function (event) {
                    const targetSelector = this.dataset.copyTarget;
                    const target = document.querySelector(targetSelector);
                    navigator.clipboard.writeText(target.textContent);

                    const tooltip = new Tooltip(this, {
                        trigger: 'manual',
                        title: 'copied'
                    });
                    tooltip.show();
                    setTimeout(() => tooltip.hide(), 1000);
                });
            }
        };

        /**
         * Moves dropdowns out of the table, so they don't get clipped by the scroll area.
         */
        const initMoveDropdowns = function() {
            document.addEventListener('show.bs.dropdown', function(event) {
                const trigger = event.target;
                const menu = trigger.nextElementSibling;
                if (!trigger.classList.contains('member-actions')) {
                    return;
                }

                trigger.menu = menu;
                document.getElementById('content').appendChild(menu);
            });
            document.addEventListener('hide.bs.dropdown', function(event) {
                const trigger = event.target;
                const menu = trigger.menu;
                if (!trigger.classList.contains('member-actions')) {
                    return;
                }

                trigger.insertAdjacentElement('afterend', menu);
            });
        };

        getMembers().then(members => initMembersTable(members));
        getGames().then(games => initGamesTable(games));
        initCopyButtons();
        initMoveDropdowns();
    </script>

</div>

<%@ include file="/jsp/footer.jsp" %>
