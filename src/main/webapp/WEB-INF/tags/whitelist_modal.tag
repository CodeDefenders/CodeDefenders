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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="gameId" required="false" %>
<%@ attribute name="mayChooseRole" required="true" %>
<%@ attribute name="liveGame" required="true" %>
<%@ attribute name="type" required="true" %>
<!--true, if the game already exists, false, if it is being created-->

<div>
    <t:modal title="Modify whitelist" id="${htmlId}"
             modalDialogClasses="modal-lg">
        <jsp:attribute name="content">
            <div id="whitelist-inputs" type="hidden"></div>
            <div class="card" id="whitelist-modal-card">
                <div class="card-header">
                    <label for="searchInput">Search: </label>
                </div>
                <div class="card-body">
                    <input type="text" id="searchInput" class="form-control" placeholder="Invite users">
                    <div id="autocompleteList" class="list-group"></div>
                </div>
                <c:choose>
                    <c:when test="${liveGame}">
                        <div class="card-header">
                            <div class="card">
                                <div class="card-header">
                                    <h6>Current whitelist:</h6>
                                </div>
                                <div class="card-body">
                                    <div class="d-flex flex-wrap gap-2" id="already-whitelisted"></div>
                                </div>
                            </div>
                        </div>
                    </c:when>
                </c:choose>
                <div class="card">
                    <div class="card-header">
                        <h6>Users to invite:</h6>
                    </div>
                    <div class="card-body">
                        <div class="d-flex flex-wrap gap-2" id="invited"></div>
                    </div>
                </div>
            </div>
        </jsp:attribute>
        <jsp:attribute name="footer">
            <button type="button" id="copy-link-button" class="btn-primary btn">
                Copy invite link
            </button>
            <c:choose>
                <c:when test="${liveGame}">
                    <button type="button" class="btn btn-primary disabled" id="update-button">Update whitelist</button>
                </c:when>
            </c:choose>
        </jsp:attribute>
    </t:modal>

    <script type="module">
        const gameId = "${gameId}";
        const liveGame = ${liveGame};
        const mayChooseRoleConst = ${mayChooseRole};
        const melee = ${type == "melee" ? "true" : "false"};

        const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
        const suggestions = await InfoApi.getAllUserNames();

        const input = document.getElementById("searchInput");
        const list = document.getElementById("autocompleteList");
        const invitedArea = document.getElementById("invited");
        const linkButton = document.getElementById('copy-link-button');
        const whitelistModalOpener = document.getElementById("whitelist-modal-opener");

        let updateButton;
        let alreadyWhitelistedArea;
        let chooseRoleSwitch;

        const currentUsers = [];

        if (liveGame) {
            updateButton = document.getElementById("update-button");
            alreadyWhitelistedArea = document.getElementById("already-whitelisted");
            chooseRoleSwitch = null;
        } else {
            updateButton = null;
            alreadyWhitelistedArea = null;
            if (!melee) {
                chooseRoleSwitch = document.getElementById("choose-role-switch");
            } else {
                chooseRoleSwitch = null;
            }
        }

        const inputContainer = document.getElementById("whitelist-inputs");

        const choiceToAddUsers = [];
        const attackerToAddUsers = [];
        const defenderToAddUsers = [];
        const flexToAddUsers = [];

        const choiceAlreadyWhitelistedUsers = [];
        const attackerAlreadyWhitelistedUsers = [];
        const defenderAlreadyWhitelistedUsers = [];
        const flexAlreadyWhitelistedUsers = [];

        const toRemoveUsers = [];


        if (liveGame) {
            await displayAlreadyInvitedUsers();
        }

        function mayChooseRole() {
            if (melee) {
                return true; //Because the creator cannot choose for the players
            }
            if (liveGame) {
                return mayChooseRoleConst;
            } else {
                return chooseRoleSwitch.checked;
            }
        }

        /**
         * Remove a user that has already been invited from the whitelist. The change will only be applied
         * after clicking the update button.
         * @param userBadge The graphical badge element in the alreadyWhitelistedArea.
         */
        function removeSingleUser(userBadge) {
            const userButton = userBadge.querySelector('button');
            const user = userBadge.querySelector('span').textContent;

            toRemoveUsers.push(user);
            const undoButton = document.createElement("button");
            undoButton.classList.add("btn", "btn-close-white", "p-0", "ms-1");
            undoButton.innerHTML = "<i class='fa fa-undo m-0 p-0'></i>";

            const oldNameElement = userBadge.querySelector("span");
            oldNameElement.remove();

            const crossedNameElement = document.createElement("del");
            crossedNameElement.textContent = user;
            userBadge.append(crossedNameElement);

            undoButton.addEventListener("click", function () {
                toRemoveUsers.splice(toRemoveUsers.indexOf(user), 1);
                userBadge.classList.remove("bg-danger");
                userBadge.classList.add("bg-secondary");
                undoButton.remove();
                crossedNameElement.remove();
                const newNameElement = document.createElement("span");
                newNameElement.textContent = user;
                userBadge.append(newNameElement);
                //userBadge.textContent = user;
                userBadge.append(userButton);
                enableOrDisableUpdateButton();
            });

            //userBadge.classList.add("bg-danger");
            //userBadge.textContent = "<del>" + userBadge.innerHTML + "</del>";
            userButton.remove();
            userBadge.append(undoButton);
            enableOrDisableUpdateButton();
        }

        /**
         * Displays the already invited users in the alreadyWhitelistedArea. The users are fetched from the server.
         * The function is called when the modal is opened with liveGame=true and when the update button is clicked.
         */
        async function displayAlreadyInvitedUsers() {
            clearArray(choiceAlreadyWhitelistedUsers);
            clearArray(attackerAlreadyWhitelistedUsers);
            clearArray(defenderAlreadyWhitelistedUsers);
            clearArray(flexAlreadyWhitelistedUsers);
            clearArray(currentUsers);
            if (mayChooseRole()) {
                const choice = await InfoApi.getWhitelistedUserNames(gameId);
                const current = await InfoApi.getUserNamesForGame(gameId);
                choice.forEach(user => choiceAlreadyWhitelistedUsers.push(user));
                current.forEach(user => currentUsers.push(user));
            } else {
                const choice = await InfoApi.getWhitelistedUserNamesWithType(gameId, "choice");
                const attacker = await InfoApi.getWhitelistedUserNamesWithType(gameId, "attacker");
                const defender = await InfoApi.getWhitelistedUserNamesWithType(gameId, "defender");
                const flex = await InfoApi.getWhitelistedUserNamesWithType(gameId, "flex");
                const current = await InfoApi.getUserNamesForGame(gameId);

                choice.forEach(user => choiceAlreadyWhitelistedUsers.push(user));
                attacker.forEach(user => attackerAlreadyWhitelistedUsers.push(user));
                defender.forEach(user => defenderAlreadyWhitelistedUsers.push(user));
                flex.forEach(user => flexAlreadyWhitelistedUsers.push(user));
                current.forEach(user => currentUsers.push(user));
            }


            alreadyWhitelistedArea.innerHTML = "";
            displayUsers(choiceAlreadyWhitelistedUsers, "choice");
            if (!mayChooseRole()) {
                displayUsers(attackerAlreadyWhitelistedUsers, "attacker");
                displayUsers(defenderAlreadyWhitelistedUsers, "defender");
                displayUsers(flexAlreadyWhitelistedUsers, "flex");
            }
        }

        /**
         * Adds the users contained in a single array to the already whitelisted users.
         * @param users Array of users to add
         * @param type The whitelist type (attacker, defender, flex, choice) determining the color of the badge
         */
        function displayUsers(users, type) {
            let background;
            if (mayChooseRole()) {
                background = "bg-secondary";
            } else {
                if (type === "attacker") {
                    background = "bg-attacker";
                } else if (type === "defender") {
                    background = "bg-defender";
                } else {
                    background = "bg-secondary";
                }
            }

            users.forEach(user => {
                if (!currentUsers.includes(user)) {
                    const userBadge = document.createElement("div");
                    userBadge.classList.add("badge", background, "rounded-pill", "d-flex", "align-items-center", "m-1");
                    const userBadgeNameElement = document.createElement("span");
                    userBadgeNameElement.textContent = user;
                    userBadge.append(userBadgeNameElement);

                    const userButton = document.createElement("button");
                    userButton.innerHTML = "<i class='fa fa-times m-0 p-0'></i>";
                    userButton.classList.add("btn", "btn-close-white", "p-0", "ms-1");
                    userButton.addEventListener("click", () => {
                        removeSingleUser(userBadge);
                    });

                    userBadge.appendChild(userButton);
                    alreadyWhitelistedArea.appendChild(userBadge);
                }
            })
        }


        /**
         * Adds a user to the whitelist. The change will only be applied after the update button is clicked.
         * @param user Name of the user to add.
         * @param type The type of whitelist to add the user to. (attacker, defender, flex, choice)
         */
        function addUserToWhitelist(user, type) {
            let background;
            let toAddUsers;

            setInviteId();

            if (mayChooseRole()) {
                background = "bg-secondary";
                toAddUsers = choiceToAddUsers;
            } else {
                if (type === "attacker") {
                    background = "bg-attacker";
                    toAddUsers = attackerToAddUsers;
                } else if (type === "defender") {
                    background = "bg-defender";
                    toAddUsers = defenderToAddUsers;
                } else {
                    background = "bg-info";
                    if (type === "flex") {
                        toAddUsers = flexToAddUsers;
                    } else {
                        toAddUsers = choiceToAddUsers;
                    }
                }
            }

            toAddUsers.push(user);
            if (!liveGame) {
                const inputElement = document.createElement("input");
                inputElement.type = "hidden";
                inputElement.name = "whitelist-" + type + "-" + toAddUsers.length;
                inputElement.id = "whitelist-input-" + user;
                inputElement.value = user;
                inputContainer.appendChild(inputElement);
            }
            const userBadge = document.createElement("div");
            userBadge.classList.add("badge", background, "rounded-pill", "d-flex", "align-items-center", "m-1");
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
                if (!liveGame) {
                    const inputElement = document.getElementById("whitelist-input-" + user);
                    if (inputElement) {
                        inputElement.remove();
                    }
                }
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
                    && !userAlreadyAdded(item));
            matches.forEach(match => {
                const item = document.createElement("div");
                item.classList.add("list-group-item", "d-flex", "flex-row", "gap-2");

                const name = document.createElement("span");
                name.textContent = match;
                name.classList.add("flex-grow-1");
                item.appendChild(name);
                if (!mayChooseRole()) {
                    const defenderInvite = document.createElement("button");
                    defenderInvite.textContent = "Invite as defender";
                    defenderInvite.type = "button";
                    defenderInvite.classList.add("btn", "btn-primary", "btn-sm");
                    defenderInvite.addEventListener("click", function () {
                        addUserToWhitelist(match, "defender");
                        item.remove();
                        list.innerHTML = "";
                        input.value = "";
                    });
                    item.appendChild(defenderInvite);
                }

                const flexInvite = document.createElement("button");
                flexInvite.textContent = melee || mayChooseRole() ? "Invite" : "Balance teams";
                flexInvite.type = "button";
                flexInvite.classList.add("btn", "btn-secondary", "btn-sm");
                flexInvite.addEventListener("click", () => {
                    addUserToWhitelist(match, mayChooseRole() ? "choice" : "flex");
                    item.remove();
                    list.innerHTML = "";
                    input.value = "";
                });
                item.appendChild(flexInvite);

                if (!mayChooseRole()) {
                    const attackerInvite = document.createElement("button");
                    attackerInvite.textContent = "Add as attacker";
                    attackerInvite.classList.add("btn", "btn-danger", "btn-sm");
                    attackerInvite.addEventListener("click", function () {
                        addUserToWhitelist(match, "attacker");
                        item.remove();
                        list.innerHTML = "";
                        input.value = "";
                    });
                    item.appendChild(attackerInvite);
                }

                list.appendChild(item);
            });
        });

        /**
         * Check whether a user is already added to the whitelist. This includes users that are already whitelisted
         * as well as users that are about to be added to the whitelist on the next update.
         * It also includes all users that are currently part of the game, even if they are not whitelisted.
         * @param user The username to check against.
         * @returns {boolean} True, if the user is already added to the whitelist, false otherwise.
         */
        function userAlreadyAdded(user) {
            const relevantArrays = [choiceToAddUsers];
            if (liveGame) {
                relevantArrays.push(choiceAlreadyWhitelistedUsers, currentUsers);
            }
            if (!mayChooseRole() || !liveGame) {
                //Check for other whitelists even if mayChooseRole is true when liveGame is false,
                //because mayChooseRole can be switched while the game is being created
                relevantArrays.push(attackerToAddUsers, defenderToAddUsers, flexToAddUsers);
            }
            if (!mayChooseRole() && liveGame) {
                relevantArrays.push(attackerAlreadyWhitelistedUsers, defenderAlreadyWhitelistedUsers, flexAlreadyWhitelistedUsers);
            }
            return relevantArrays.some(list => list.includes(user));
        }


        function clearArray(array) {
            array.splice(0, array.length);
        }

        document.addEventListener("click", function (e) {
            if (!e.target.closest("#searchInput")) {
                list.innerHTML = "";
            }
        });

        if (chooseRoleSwitch) {
            chooseRoleSwitch.addEventListener("change", function () {
                if (this.checked) {
                    invitedArea.querySelectorAll('.badge').forEach(badge => {
                        badge.classList.remove("bg-attacker", "bg-defender");
                        badge.classList.add("bg-secondary");
                    });
                } else {
                    invitedArea.querySelectorAll('.badge').forEach(badge => {
                        if (attackerToAddUsers.includes(badge.textContent)) {
                            badge.classList.remove("bg-secondary");
                            badge.classList.add("bg-attacker");
                        } else if (defenderToAddUsers.includes(badge.textContent)) {
                            badge.classList.remove("bg-secondary");
                            badge.classList.add("bg-defender");
                        }
                    });
                }
            });
        }

        async function copyLink(inviteLink) {
            navigator.clipboard.writeText(inviteLink)
                .then(() => {
                    linkButton.innerText = "Copied!";
                    setTimeout(() => {
                        linkButton.innerText = "Copy invite link";
                    }, 2000);
                })
                .catch(err => {
                    console.error('Failed to copy: ', err);
                });
        }

        /*if (liveGame) {
            whitelistModalOpener.addEventListener("click", async function () {
                currentUsers = await InfoApi.getUserNamesForGame(gameId);
            });
        }*/

        linkButton.addEventListener('click', async () => {

            let linkData;
            if (liveGame) {
                linkData = await InfoApi.getInviteLinkData(gameId);
            } else {
                linkData = await setInviteId();
            }

            await copyLink(linkData.inviteLink)
        });

        /**
         * If no inviteId is set for the current game, it will be set by this function.
         * This only works for games that haven't been created yet.
         */
        async function setInviteId() {
            if (liveGame) {
                return;
            }

            const inviteIdInput = document.getElementById('invite-id');
            if (inviteIdInput.value !== "") {
                return;
            }
            const linkData = await InfoApi.getInviteLinkDataWithoutGameId();
            inviteIdInput.value = linkData.inviteId;
            return linkData;
        }

        if (updateButton) { //Only when liveGame=true
            updateButton.addEventListener("click", async function () {
                let params = new URLSearchParams();
                let choiceResponse, attackerResponse, defenderResponse, flexResponse;

                toRemoveUsers.forEach(user => {
                    params.append("removeNames", user);
                });

                params.append("type", "choice");
                choiceToAddUsers.forEach(user => {
                    params.append("addNames", user);
                });
                choiceResponse = await sendPostRequest(params);

                if (!mayChooseRole()) {
                    params = new URLSearchParams();
                    params.append("type", "attacker");
                    attackerToAddUsers.forEach(user => {
                        params.append("addNames", user);
                    });
                    attackerResponse = await sendPostRequest(params);

                    params = new URLSearchParams();
                    params.append("type", "defender");
                    defenderToAddUsers.forEach(user => {
                        params.append("addNames", user);
                    });
                    defenderResponse = await sendPostRequest(params);

                    params = new URLSearchParams()
                    params.append("type", "flex");
                    flexToAddUsers.forEach(user => {
                        params.append("addNames", user);
                    });
                    flexResponse = await sendPostRequest(params);
                }
                let allOk;
                if (!mayChooseRole()) {
                    allOk = [choiceResponse, attackerResponse, defenderResponse, flexResponse].every(response => response.ok);
                } else {
                    allOk = choiceResponse.ok;
                }


                if (allOk) {
                    clearArray(choiceToAddUsers);
                    clearArray(attackerToAddUsers);
                    clearArray(defenderToAddUsers);
                    clearArray(flexToAddUsers);
                    clearArray(toRemoveUsers);
                    invitedArea.innerHTML = "";
                    await displayAlreadyInvitedUsers();
                    //bootstrap.Modal.getInstance(document.getElementById("${htmlId}")).hide();
                    updateButton.classList.add("disabled");
                } else {
                    let errorMessage;
                    if (!choiceResponse.ok) {
                        errorMessage = choiceResponse.text;
                    } else if (!attackerResponse.ok) {
                        errorMessage = attackerResponse.text();
                    } else if (!defenderResponse.ok) {
                        errorMessage = defenderResponse.text();
                    } else if (!flexResponse.ok) {
                        errorMessage = flexResponse.text();
                    }
                    console.error("Error updating whitelist:", errorMessage);
                }

            });
        }

        /**
         * Performs a POST request to the whitelist API. Always sends the gameId.
         */
        async function sendPostRequest(params) {
            params.append("gameId", gameId);
            return await fetch("${url.forPath("/api/whitelist")}", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            });
        }

        function enableOrDisableUpdateButton() {
            if (!liveGame) return;
            if ([attackerToAddUsers, defenderToAddUsers, choiceToAddUsers, flexToAddUsers, toRemoveUsers].some(array => array.length > 0)) {
                updateButton.classList.remove("disabled");
            } else {
                updateButton.classList.add("disabled");
            }
        }
    </script>
</div>
