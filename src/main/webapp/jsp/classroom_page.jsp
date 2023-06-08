<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.model.ClassroomRole" %>

<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="classroomService" type="org.codedefenders.service.ClassroomService"--%>
<%--@elvariable id="userService" type="org.codedefenders.service.UserService"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<c:set var="classroom" value="${requestScope.classroom}"/>
<c:set var="member" value="${requestScope.member}"/>
<c:set var="isOwner" value="${member.role == ClassroomRole.OWNER}"/>
<c:set var="link" value="${url.getAbsoluteURLForPath(Paths.CLASSROOM)}?room=${classroom.roomCode}"/>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">

    <%-- Heading --%>
    <div class="d-flex flex-row align-items-center gap-3 mb-5">
        <h2 class="m-0"><c:out value="${classroom.name}"/></h2>
        <c:if test="${isOwner}">
            <button id="change-name" type="button" class="btn btn-sm btn-secondary"
                    data-bs-toggle="modal" data-bs-target="#change-name-modal">
                <i class="fa fa-edit" title="Change classroom name"></i>
            </button>
        </c:if>
    </div>

    <%-- Tables --%>
    <div class="row g-5 mb-5" id="tables-container">
        <div class="col-lg-6 col-12">
            <div class="loading loading-height-200 loading-border-card">
                <div class="d-flex justify-content-between flex-wrap align-items-baseline">
                    <h4 class="mb-3">Students</h4>
                    <input type="search" id="search-students" placeholder="Search"
                           class="form-control form-control-sm" style="width: 15em;">
                </div>
                <table id="students-table" class="table" style="width: 100%;"></table>
            </div>
        </div>
        <div class="col-lg-6 col-12">
            <div class="loading loading-height-200 loading-border-card">
                <div class="d-flex justify-content-between flex-wrap align-items-baseline">
                    <h4 class="mb-3">Moderators</h4>
                    <input type="search" id="search-moderators" placeholder="Search"
                           class="form-control form-control-sm" style="width: 15em;">
                </div>
                <table id="moderators-table" class="table" style="width: 100%;"></table>
            </div>
        </div>
    </div>

    <%-- Join Settigns --%>
    <c:if test="${isOwner}">
        <div class="row">
            <div class="col-lg-6 col-12 ps-0">
                <div class="card">
                    <div class="card-body">

                        <h4 class="card-title mb-3">Join Settings</h4>

                        <div class="d-flex flex-column align-items-baseline gap-1 mb-3">
                            <c:choose>
                                <c:when test="${classroom.open}">
                                    <span>Joining this classroom is <span class="text-success">enabled</span>.</span>
                                    <button id="disable-joining" class="btn btn-sm btn-danger"
                                        data-bs-toggle="modal" data-bs-target="#disable-joining-modal">
                                        Disable Joining
                                    </button>
                                </c:when>
                                <c:otherwise>
                                    <span>Joining this classroom is <span class="text-danger">disabled</span>.</span>
                                    <button id="enable-joining" class="btn btn-sm btn-success"
                                            data-bs-toggle="modal" data-bs-target="#enable-joining-modal">
                                        Enable Joining
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div class="d-flex flex-column align-items-baseline gap-1">
                            <c:choose>
                                <c:when test="${classroom.password.isPresent()}">
                                    <span>Password is <span class="text-success">set</span>.</span>
                                </c:when>
                                <c:otherwise>
                                    <span>Password is <span class="text-danger">not set</span>.</span>
                                </c:otherwise>
                            </c:choose>
                            <div class="d-flex flex-row gap-1">
                                <button id="set-password" class="btn btn-sm btn-secondary"
                                        data-bs-toggle="modal" data-bs-target="#set-password-modal">
                                    Set Password
                                </button>
                                <c:if test="${classroom.password.isPresent()}">
                                    <button id="remove-password" class="btn btn-sm btn-danger"
                                            data-bs-toggle="modal" data-bs-target="#remove-password-modal">
                                        Remove Password
                                    </button>
                                </c:if>
                            </div>
                        </div>

                        <div class="d-flex flex-column align-items-baseline gap-1 mt-3">
                            <span>
                                Room Code is
                                <span class="d-inline-block border rounded ms-1 px-2">
                                    <span id="room-code"><c:out value="${classroom.roomCode}"/></span>
                                    <i class="fa fa-clipboard copy ms-1 cursor-pointer text-primary"
                                       data-copy-target="#room-code"></i>
                                </span>
                            </span>
                            <span>
                                Invite link is
                                <span class="d-inline-block border rounded ms-1 px-2">
                                    <span id="invite-link"><c:out value="${link}"/></span>
                                    <i class="fa fa-clipboard copy ms-1 cursor-pointer text-primary"
                                       data-copy-target="#invite-link"></i>
                                </span>
                            </span>
                            <button id="change-room-code" class="btn btn-sm btn-secondary"
                                    data-bs-toggle="modal" data-bs-target="#change-room-code-modal">
                                Change room code
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </c:if>

    <%-- Change classroom name modal --%>
    <c:if test="${isOwner}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
            <input type="hidden" name="action" value="change-name"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Change classroom name" id="change-name-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    <label for="name-input" class="form-label">Name</label>
                    <input type="text" class="form-control" id="name-input" name="name"
                           value="<c:out value="${classroom.name}"/>"
                           required maxlength="100">
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
    <c:if test="${isOwner}">
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
    <c:if test="${isOwner}">
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

    <%-- Change room code modal --%>
    <c:if test="${isOwner}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
            <input type="hidden" name="action" value="change-room-code"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>

            <t:modal title="Change room code" id="change-room-code-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    <p>Changing the room code will invalidate the old room code and invite link.</p>
                    <label for="room-code-input" class="form-label">Room Code</label>
                    <div class="input-group has-validation">
                        <input type="text" class="form-control" id="room-code-input" name="room-code"
                               value="<c:out value="${classroom.roomCode}"/>"
                               required minlength="4" maxlength="20" pattern="[a-zA-Z0-9_\-]*">
                        <button type="button" id="randomize-room-code" class="btn btn-outline-primary"
                                title="Generate random room code." data-bs-toggle="tooltip">
                            <i class="fa fa-random"></i>
                        </button>
                        <div class="invalid-feedback" id="room-code-feedback">
                            Please enter a valid room code.
                        </div>
                    </div>
                    <div class="form-text">
                        4-20 alphanumeric characters (a-z, A-Z, 0-9).
                        Dashes (-) are allowed, but spaces aren't.
                    </div>

                    <script>
                        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
                        const ORIGINAL_ROOM_CODE = '${classroom.roomCode}';
                        const ROOM_CODE_RANDOM_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';

                        const roomCodeInput = document.getElementById('room-code-input');
                        const randomizeButton = document.getElementById('randomize-room-code');
                        const feedback = document.getElementById('room-code-feedback');

                        const generateRoomCode = function() {
                            let code = '';
                            for (let i = 0; i < 4; i++) {
                                const choice = Math.floor(Math.random() * ROOM_CODE_RANDOM_CHARS.length);
                                code += ROOM_CODE_RANDOM_CHARS[choice];
                            }
                            return code;
                        };

                        const checkRoomCodeExists = async function(roomCode) {
                            const params = new URLSearchParams({
                                type: 'exists',
                                room: roomCode
                            });
                            const response = await fetch(`\${API_URL}?\${params}`, {
                                method: 'GET',
                                headers: {
                                    'Content-Type': 'application/json'
                                }
                            });
                            if (!response.ok) {
                                console.error('Failed to check if room code exists.');
                                return false;
                            }
                            return response.json();
                        };

                        const validateRoomCode = async function() {
                            const roomCode = roomCodeInput.value;
                            if (roomCode !== ''
                                    && roomCode.trimEnd() !== ORIGINAL_ROOM_CODE
                                    && await checkRoomCodeExists(roomCode)) {
                                roomCodeInput.setCustomValidity('room-code-in-use');
                                feedback.innerText = 'Room code is already in use. Please choose a different one.';
                            } else {
                                roomCodeInput.setCustomValidity('');
                                feedback.innerText = 'Please enter a valid room code.';
                            }
                        };

                        let timeout = null;
                        const validateRoomCodeDelayed = function() {
                            if (timeout != null) {
                                clearTimeout(timeout);
                            }
                            timeout = setTimeout(() => {
                                validateRoomCode()
                                timeout = null;
                            }, 200);
                        }

                        randomizeButton.addEventListener('click', function(event) {
                            roomCodeInput.value = generateRoomCode();
                            validateRoomCodeDelayed();
                        });

                        roomCodeInput.addEventListener('input', validateRoomCodeDelayed);
                    </script>
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Save changes</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Enable joining modal --%>
    <c:if test="${isOwner}">
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
    <c:if test="${isOwner}">
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

    <%-- Promote/Demote student/moderator modal --%>
    <c:if test="${isOwner}">
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
    <c:if test="${isOwner}">
        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" id="change-owner-form">
            <input type="hidden" name="action" value="change-owner"/>
            <input type="hidden" name="classroomId" value="${classroom.id}"/>
            <input type="hidden" name="userId" value=""/>

            <t:modal title="Change role" id="change-role-modal" closeButtonText="Cancel">
                <jsp:attribute name="content">
                    Are you sure you want to make
                    <span data-fill="username" class="border rounded px-1"></span>
                    the owner of this classroom?
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Change owner</button>
                </jsp:attribute>
            </t:modal>

        </form>
    </c:if>

    <%-- Kick member modal --%>
    <c:if test="${isOwner}">
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

        const STUDENT = '${ClassroomRole.STUDENT.name()}';
        const OWNER = '${ClassroomRole.OWNER.name()}';
        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';

        const classroomId = ${classroom.id};
        const ownUserId = ${member.userId};
        const isOwner = ${isOwner ? "true" : "false"};


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
         * Renders roles with correct capitalization and sorts owners first.
         */
        const renderRole = function(role, type, row, meta) {
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

        const renderStudentButtons = function(data, type, row, meta) {
            return `
                <div class="float-end">
                    <span type="button" class="cursor-pointer px-3" data-bs-toggle="dropdown">
                        <i class="fa fa-ellipsis-v"></i>
                    </span>
                    <div class="dropdown-menu">
                        <li><h6 class="dropdown-header">Change Role to ...</h6></li>
                        <li>
                            <a href="#" class="dropdown-item promote-student">
                                <i style="width: 1.25em;" class="fa fa-user text-primary"></i>
                                Moderator
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
        };

        const renderModeratorButtons = function(data, type, row, meta) {
            if (data.role === '<%=ClassroomRole.OWNER.name()%>') {
                return '';
            }
            return `
                <div class="float-end">
                    <span type="button" class="cursor-pointer px-3" data-bs-toggle="dropdown">
                        <i class="fa fa-ellipsis-v"></i>
                    </span>
                    <div class="dropdown-menu">
                        <li><h6 class="dropdown-header">Change Role to ...</h6></li>
                        <li>
                            <a href="#" class="dropdown-item demote-moderator">
                                <i style="width: 1.25em;" class="fa fa-user text-primary"></i>
                                Student
                            </a>
                            <a href="#" class="dropdown-item change-owner">
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
        };

        const kickMember = function(data) {
            const form = document.getElementById('kick-member-form');
            form.querySelector('input[name="userId"]').value = data.user.id;
            form.querySelector('[data-fill="username"]').innerText = data.user.name;
            new Modal(form.querySelector('.modal')).show();
        };

        const demoteModerator = function(data) {
            const form = document.getElementById('change-role-form');
            form.querySelector('input[name="userId"]').value = data.user.id;
            form.querySelector('input[name="role"]').value = '<%=ClassroomRole.STUDENT.name()%>';
            form.querySelector('[data-fill="username"]').innerText = data.user.name;
            form.querySelector('[data-fill="role"]').innerText = 'Student';
            new Modal(form.querySelector('.modal')).show();
        };

        const promoteStudent = function(data) {
            const form = document.getElementById('change-role-form');
            form.querySelector('input[name="userId"]').value = data.user.id;
            form.querySelector('input[name="role"]').value = '<%=ClassroomRole.MODERATOR.name()%>';
            form.querySelector('[data-fill="username"]').innerText = data.user.name;
            form.querySelector('[data-fill="role"]').innerText = 'Moderator';
            new Modal(form.querySelector('.modal')).show();
        };

        const changeOwner = function(data) {
            const form = document.getElementById('change-owner-form');
            form.querySelector('input[name="userId"]').value = data.user.id;
            form.querySelector('[data-fill="username"]').innerText = data.user.name;
            new Modal(form.querySelector('.modal')).show();
        };

        const initStudentsTable = function(data) {
            const studentsTable = new DataTable('#students-table', {
                data,
                columns: [
                    {
                        data: 'user.id',
                        title: 'User ID',
                        type: 'number',
                        width: '5em',
                        visible: isOwner
                    },
                    {
                        data: 'user.name',
                        title: 'Name',
                        type: 'string'
                    },
                    {
                        data: null,
                        title: 'Actions',
                        orderable: false,
                        searchable: false,
                        render: renderStudentButtons,
                        type: 'html',
                        width: '4em',
                        visible: isOwner
                    }
                ].filter(e => e !== null),
                order: [[1, 'asc']],
                scrollY: '500px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'This classroom has no students... yet.'}
            });
            LoadingAnimation.hideAnimation(studentsTable.table().container());

            document.getElementById('search-students').addEventListener('keyup', function(event) {
                setTimeout(() => studentsTable.search(this.value).draw(), 0);
            });
            for (const button of studentsTable.table().container().getElementsByClassName('kick-member')) {
                const data = studentsTable.row(button.closest('tr')).data();
                button.addEventListener('click', _ => kickMember(data));
            }
            for (const button of studentsTable.table().container().getElementsByClassName('promote-student')) {
                const data = studentsTable.row(button.closest('tr')).data();
                button.addEventListener('click', _ => promoteStudent(data));
            }
        };

        const initModeratorsTable = function(data) {
            const moderatorsTable = new DataTable('#moderators-table', {
                data,
                columns: [
                    {
                        data: 'user.id',
                        title: 'User ID',
                        type: 'number',
                        width: '5em',
                        visible: isOwner
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
                        render: renderRole,
                    },
                    {
                        data: null,
                        title: 'Actions',
                        orderable: false,
                        searchable: false,
                        type: 'html',
                        render: renderModeratorButtons,
                        width: '4em',
                        visible: isOwner
                    }
                ],
                order: [[2, 'asc'], [1, 'asc']],
                scrollY: '500px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'This classroom has no moderators.'}
            });
            LoadingAnimation.hideAnimation(moderatorsTable.table().container());

            document.getElementById('search-moderators').addEventListener('keyup', function(event) {
                setTimeout(() => moderatorsTable.search(this.value).draw(), 0);
            });
            for (const button of moderatorsTable.table().container().getElementsByClassName('kick-member')) {
                const data = moderatorsTable.row(button.closest('tr')).data();
                button.addEventListener('click', _ => kickMember(data));
            }
            for (const button of moderatorsTable.table().container().getElementsByClassName('demote-moderator')) {
                const data = moderatorsTable.row(button.closest('tr')).data();
                button.addEventListener('click', _ => demoteModerator(data));
            }
            for (const button of moderatorsTable.table().container().getElementsByClassName('change-owner')) {
                const data = moderatorsTable.row(button.closest('tr')).data();
                button.addEventListener('click', _ => changeOwner(data));
            }
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
            const tablesContainer = document.getElementById('tables-container');
            tablesContainer.addEventListener('show.bs.dropdown', function(event) {
                const trigger = event.target;
                const menu = trigger.nextElementSibling;

                trigger.menu = menu;
                document.getElementById('content').appendChild(menu);
            });
            tablesContainer.addEventListener('hide.bs.dropdown', function(event) {
                const trigger = event.target;
                const menu = trigger.menu;

                trigger.insertAdjacentElement('afterend', menu);
            });
        };

        const members = await getMembers();
        const students = members.filter(member => member.role === STUDENT);
        const moderators = members.filter(member => member.role !== STUDENT);

        initStudentsTable(students);
        initModeratorsTable(moderators);
        initCopyButtons();
        initMoveDropdowns();
    </script>

</div>

<%@ include file="/jsp/footer.jsp" %>
