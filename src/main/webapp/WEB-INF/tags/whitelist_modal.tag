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
    <t:modal title="Whitelist" id="${htmlId}"
             modalDialogClasses="modal-lg">
        <jsp:attribute name="content">
            <div class="card" id="whitelist-modal-card">
                <div class="card-header">
                    <label for="searchInput">Search: </label>
                    <input type="text" id="searchInput" class="form-control" placeholder="Invite users">
                    <div id="autocompleteList" class="list-group"></div>
                </div>
                <div class="card-body p-0"><!-- TODO Bereits eingeladene anzeigen -->
                    <div class="tab-content">
                        <div class="d-flex flex-wrap gap-2" id="invited">
                        </div>
                    </div>
                </div>
            </div>
        </jsp:attribute>
        <jsp:attribute name="footer">
            <button type="button" class="btn btn-primary" id="inviteButton">Invite</button>
        </jsp:attribute>
    </t:modal>

    <script type="module">
        const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
        const suggestions = await InfoApi.getAllUserNames();
        const input = document.getElementById("searchInput");
        const list = document.getElementById("autocompleteList");
        const invitedArea = document.getElementById("invited");
        const whitelistUsers = [];

        function addUserToWhitelist(user) {
            whitelistUsers.push(user);
            const userBadge = document.createElement("div");
            userBadge.classList.add("badge", "bg-secondary",  "rounded-pill", "d-flex", "align-items-center",  "m-1");
            userBadge.textContent = user;

            const userButton = document.createElement("button");
            userButton.classList.add("btn-close", "btn-close-white", "ms-1");
            userButton.addEventListener("click", function () {
                const index = whitelistUsers.indexOf(user);
                if (index > -1) {
                    whitelistUsers.splice(index, 1);
                }
                userBadge.remove();
            });

            userBadge.appendChild(userButton);
            invitedArea.appendChild(userBadge);
        }

        input.addEventListener("input", function () {
            const value = this.value.trim().toLowerCase();
            list.innerHTML = "";
            if (!value) return;

            const matches = suggestions.filter(item => item.toLowerCase().startsWith(value)
                    && !whitelistUsers.includes(item));
            matches.forEach(match => {
                const item = document.createElement("button");
                item.classList.add("list-group-item", "list-group-item-action");
                item.textContent = match;
                item.type = "button";
                item.addEventListener("click", function () {
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

        document.getElementById("inviteButton").addEventListener("click", async function () {
            const params = new URLSearchParams();
            whitelistUsers.forEach(user => {
                params.append("user-name", user);
            });
            params.append("add", "true");
            params.append("gameId", "${gameId}");
            const response = await fetch("${url.forPath("/api/whitelist")}", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            })

            if (response.ok) {
                whitelistUsers.slice(0, whitelistUsers.length);
                invitedArea.innerHTML = "";
                bootstrap.Modal.getInstance(document.getElementById("${htmlId}")).hide();
            }

        });
    </script>
</div>
