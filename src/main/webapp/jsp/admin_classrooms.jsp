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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Classrooms"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "classrooms"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="loading loading-height-200 loading-border-card mb-4">
        <div class="d-flex justify-content-between flex-wrap align-items-baseline">
            <h3 class="mb-3">Classrooms</h3>
            <input type="search" id="search-classrooms" placeholder="Search"
                   class="form-control form-control-sm" style="width: 15em;">
        </div>
        <table id="classrooms-table" class="table"></table>
    </div>
    <button id="create-classroom" type="button" class="btn btn-primary"
            data-bs-toggle="modal" data-bs-target="#create-classroom-modal">
        Create Classroom
    </button>

    <form action="${url.forPath(Paths.ADMIN_CLASSROOMS)}" method="post" class="needs-validation">
        <input type="hidden" name="action" value="create-classroom"/>

        <t:modal title="Create classroom" id="create-classroom-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                <div class="mb-3">
                    <label for="name-input" class="form-label">Name</label>
                    <input type="text" class="form-control" id="name-input" name="name"
                           required maxlength="100" placeholder="Name">
                    <div class="invalid-feedback">
                        Please enter a valid name.
                    </div>
                    <div class="form-text">
                        Maximum length: 100 characters.
                    </div>
                </div>

                <div>
                    <label for="room-code-input" class="form-label">Room Code (optional)</label>
                    <div class="input-group has-validation">
                        <input type="text" class="form-control" id="room-code-input" name="room-code"
                               minlength="4" maxlength="20" pattern="[a-zA-Z0-9_\-]*"
                               placeholder="Room Code (optional)">
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
                        Leave empty to generate a random room code.
                    </div>
                </div>

                <%-- Set up room code randomization and checking. --%>
                <script>
                    const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
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
                        if (roomCode !== '' && await checkRoomCodeExists(roomCode)) {
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
                <button type="submit" class="btn btn-primary">Create Classroom</button>
            </jsp:attribute>
        </t:modal>
    </form>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
        const CLASSROOM_URL = '${url.forPath(Paths.CLASSROOM)}';

        const getClassrooms = async function() {
            const params = new URLSearchParams({
                type: 'classrooms'
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

        const renderLinkButton = function(data, type, row, meta) {
            switch (type) {
                case 'type':
                case 'sort':
                case 'filter':
                    return null;
                case 'display':
                    const params = new URLSearchParams({
                        room: data.roomCode
                    });
                    return `
                        <a href="\${CLASSROOM_URL}?\${params}" class="cursor-pointer float-end px-2">
                            <i class="fa fa-external-link text-primary"></i>
                        </a>
                    `;
            }
        };

        const classrooms = await getClassrooms();

        const classroomsTable = new DataTable('#classrooms-table', {
            data: classrooms,
            columns: [
                {
                    data: 'id',
                    type: 'number',
                    title: 'ID'
                },
                {
                    data: 'name',
                    type: 'string',
                    title: 'Name'
                },
                {
                    data: 'roomCode',
                    type: 'string',
                    title: 'Room Code'
                },
                {
                    data: 'open',
                    type: 'boolean',
                    title: 'Open'
                },
                {
                    data: null,
                    title: 'Link',
                    render: renderLinkButton,
                    width: '2em'
                },
            ],
            order: [[1, 'asc']],
            scrollY: '600px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: "There aren't any classrooms... yet."}
        });
        LoadingAnimation.hideAnimation(classroomsTable.table().container());

        /* Search bar. */
        document.getElementById('search-classrooms').addEventListener('input', function(event) {
            classroomsTable.search(this.value).draw();
        });
    </script>
</div>

<%@ include file="/jsp/footer.jsp" %>
