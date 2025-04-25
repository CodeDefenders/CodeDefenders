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

<%@ attribute name="gameActive" required="true" %>
<%@ attribute name="intentionCollectionEnabled" required="true" %>

<c:choose>
    <c:when test="${!intentionCollectionEnabled}">
        <button type="submit" form="atk"
                id="submitMutant" class="btn btn-attacker btn-highlight"
                <c:if test="${!gameActive}">disabled</c:if>>
            Attack
        </button>

        <script type="module">
            import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

            const mutantProgressBar = await objects.await('mutantProgressBar');


            document.getElementById('submitMutant').addEventListener('click', function (event) {
                this.form.submit();
                this.disabled = true;
                mutantProgressBar.activate();
            });
        </script>
    </c:when>
    <c:otherwise>
        <div id="attacker-intention-dropdown" class="dropdown">
            <button type="button" class="btn btn-attacker btn-highlight dropdown-toggle"
                    data-bs-toggle="dropdown" id="submitMutant" aria-expanded="false"
                    <c:if test="${!gameActive}">disabled</c:if>>
                Attack
            </button>
            <ul class="dropdown-menu" aria-labelledby="submitMutant" style="">
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="KILLABLE">
                        My mutant is killable
                    </a>
                </li>
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="EQUIVALENT">
                        My mutant is equivalent
                    </a>
                </li>
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="DONTKNOW">
                        I don't know if my mutant is killable
                    </a>
                </li>
            </ul>
        </div>
    </c:otherwise>
</c:choose>
