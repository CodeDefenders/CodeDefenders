<%@ tag import="org.codedefenders.servlets.api.UserAPI" %><%--

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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="gameId" required="false" %>

<div>
    <t:modal title="Modify whitelist" id="${htmlId}"
             modalDialogClasses="modal-lg">
        <jsp:attribute name="content">
            <div class="card" id="whitelist-modal-card">
                <div class="card-header">
                    <label for="searchInput">Search: </label>
                </div>
                <div class="card-body">
                    <input type="text" id="searchInput" class="form-control" placeholder="Invite users">
                    <div id="autocompleteList" class="list-group"></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <h6>Current whitelist:</h6>
                    </div>
                    <div class="card-body">
                        <div class="d-flex flex-wrap gap-2" id="already-whitelisted"></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <h6>Invited users:</h6>
                    </div>
                    <div class="card-body">
                        <div class="d-flex flex-wrap gap-2" id="invited"></div>
                    </div>
                </div>
            </div>
        </jsp:attribute>
        <jsp:attribute name="footer">
            <button type="button" class="btn btn-primary disabled" id="update-button">Update whitelist</button>
        </jsp:attribute>
    </t:modal>

    <script type="module">
        const gameId = "${gameId}";
        const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
        const suggestions = await InfoApi.getAllUserNames();
        const input = document.getElementById("searchInput");
        const list = document.getElementById("autocompleteList");
        const invitedArea = document.getElementById("invited");
        const alreadyWhitelistedArea = document.getElementById("already-whitelisted");
        const updateButton = document.getElementById("update-button")

        const toAddUsers = [];
        const toRemoveUsers = [];
        const alreadyWhitelistedUsers = [];

        displayAlreadyInvitedUsers();

        async function displayAlreadyInvitedUsers() {
            alreadyWhitelistedUsers.splice(0, alreadyWhitelistedUsers.length);
            const fresh = await InfoApi.getWhitelistedUserNames(gameId);
            fresh.forEach(user => alreadyWhitelistedUsers.push(user));
            alreadyWhitelistedArea.innerHTML = "";
            alreadyWhitelistedUsers.forEach(user => {
                const userBadge = document.createElement("div");
                userBadge.classList.add("badge", "bg-success", "rounded-pill", "d-flex", "align-items-center", "m-1");
                userBadge.textContent = user;

                const userButton = document.createElement("button");
                userButton.innerHTML = "<i class='fa fa-times m-0 p-0'></i>"; //TODO Ein kleines bisschen andere Größe als undo-Button
                userButton.classList.add("btn", "btn-close-white", "p-0", "ms-1");
                userButton.addEventListener("click", function () { //TODO außerhalb definieren?
                    toRemoveUsers.push(user);
                    const undoButton = document.createElement("button");
                    undoButton.classList.add("btn", "btn-close-white", "p-0", "ms-1");
                    undoButton.innerHTML = "<i class='fa fa-undo m-0 p-0'></i>";
                    undoButton.addEventListener("click", function () {
                        toRemoveUsers.splice(toRemoveUsers.indexOf(user), 1);
                        userBadge.classList.remove("bg-danger");
                        userBadge.classList.add("bg-secondary");
                        undoButton.remove();
                        userBadge.append(userButton);
                        enableOrDisableUpdateButton();
                    });

                    userBadge.classList.add("bg-danger");
                    userButton.remove();
                    userBadge.append(undoButton);
                    enableOrDisableUpdateButton();
                });

                userBadge.appendChild(userButton);
                alreadyWhitelistedArea.appendChild(userBadge);
            });
        }


        function addUserToWhitelist(user) {
            //TODO Check for duplicates? EIgentlich unnötig
            toAddUsers.push(user);
            const userBadge = document.createElement("div");
            userBadge.classList.add("badge", "bg-info", "rounded-pill", "d-flex", "align-items-center", "m-1");
            userBadge.textContent = user;

            const userButton = document.createElement("button");
            userButton.classList.add("btn", "btn-close-white", "ms-1");
            userButton.innerHTML = "<i class='fa fa-times m-0 p-0'></i>";
            userButton.addEventListener("click", function () {
                const index = toAddUsers.indexOf(user);
                if (index > -1) {
                    toAddUsers.splice(index, 1);
                }
                userBadge.remove();
                enableOrDisableUpdateButton();
            });

            userBadge.appendChild(userButton);
            invitedArea.appendChild(userBadge);
            enableOrDisableUpdateButton();
        }

        input.addEventListener("input", function () {
            const value = this.value.trim().toLowerCase();
            list.innerHTML = "";
            if (!value) return;

            const matches = suggestions.filter(item => item.toLowerCase().startsWith(value)
                && !toAddUsers.includes(item) && !alreadyWhitelistedUsers.includes(item));
            matches.forEach(match => {
                const item = document.createElement("button");
                item.classList.add("list-group-item", "list-group-item-action");
                item.textContent = match;
                item.type = "button";
                item.addEventListener("click", function () {
                    item.remove();
                    addUserToWhitelist(match);
                    list.innerHTML = "";
                    input.value = "";
                });
                list.appendChild(item);
            });
        });

        document.addEventListener("click", function (e) {
            if (!e.target.closest("#searchInput")) { //TODO soll das so??
                list.innerHTML = "";
            }
        });

        updateButton.addEventListener("click", async function () {
            const params = new URLSearchParams();
            toAddUsers.forEach(user => {
                params.append("addNames", user);
            });
            toRemoveUsers.forEach(user => {
                params.append("removeNames", user);
            });
            params.append("gameId", gameId);
            const response = await fetch("${url.forPath("/api/whitelist")}", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            })

            if (response.ok) {
                toAddUsers.splice(0, toAddUsers.length);
                toRemoveUsers.splice(0, toRemoveUsers.length);
                invitedArea.innerHTML = "";
                await displayAlreadyInvitedUsers();
                //bootstrap.Modal.getInstance(document.getElementById("${htmlId}")).hide();
                updateButton.classList.add("disabled");
            } else {
                const errorMessage = await response.text();
                console.error("Error updating whitelist:", errorMessage);
                alert("Error updating whitelist: " + errorMessage); //TODO Better error handling
            }

        });

        function enableOrDisableUpdateButton() {
            if (toAddUsers.length > 0 || toRemoveUsers.length > 0) {
                updateButton.classList.remove("disabled");
            } else {
                updateButton.classList.add("disabled");
            }
        }
    </script>
</div>
