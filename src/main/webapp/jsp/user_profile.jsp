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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:useBean id="profile" class="org.codedefenders.beans.user.UserProfileBean" scope="request"/>
<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>
<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<% pageInfo.setPageTitle(profile.isSelf() ? "My Profile" : "Profile of " + profile.getUser().getUsername()); %>

<% if (login.isLoggedIn()) { %>
<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<link rel="stylesheet" href="${url.forPath("/css/specific/dashboard.css")}">

<div class="container">
    <h1>${pageInfo.pageTitle}</h1>

    <section class="mt-5 statistics" aria-labelledby="stats">
        <h2 class="mb-3" id="stats">Player Statistics</h2>

        <div class="dashboards">
            <div class="dashboard-box dashboard-mutants">
                <h3>Mutants created</h3>
                <div class="pie animate no-round ${profile.stats.totalMutants == 0 ? "no-data" : ""}"
                     style="--percentage: ${profile.stats.aliveMutantsPercentage}">
                    ${profile.stats.totalMutants}
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">Mutants still alive:</span>
                        <span class="legend-value">${profile.stats.aliveMutants}</span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">Killed mutants:</span>
                        <span class="legend-value">${profile.stats.killedMutants}</span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-tests">
                <h3>Tests written</h3>

                <div class="pie animate no-round ${profile.stats.totalTests == 0 ? "no-data" : ""}"
                     style="--percentage: ${profile.stats.killingTestsPercentage}">
                    ${profile.stats.totalTests}
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">Tests that killed mutants:</span>
                        <span class="legend-value">${profile.stats.killingTests}</span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">Non-killing tests:</span>
                        <span class="legend-value">${profile.stats.nonKillingTests}</span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-points">
                <h3>Points earned</h3>

                <div class="pie animate no-round ${profile.stats.totalPoints == 0 ? "no-data" : ""}"
                     style="--percentage: ${profile.stats.testPointsPercentage}">
                    ${profile.stats.totalPoints}
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">By writing tests:</span>
                        <span class="legend-value">${profile.stats.totalPointsTests}</span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">By creating mutants:</span>
                        <span class="legend-value">${profile.stats.totalPointsMutants}</span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-games">
                <h3>Games played</h3>

                <div class="pie animate no-round ${profile.stats.totalGames == 0 ? "no-data" : ""}"
                     style="--percentage: ${profile.stats.defenderGamesPercentage}">
                    ${profile.stats.totalGames}
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">As defender:</span>
                        <span class="legend-value">${profile.stats.defenderGames}</span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">As attacker:</span>
                        <span class="legend-value">${profile.stats.attackerGames}</span>
                    </div>
                </div>
            </div>
        </div>

        <dl class="other-stats mt-3">
            <dt>Average points per tests:</dt>
            <dd>${profile.stats.avgPointsTests}</dd>

            <dt>Average points per mutant:</dt>
            <dd>${profile.stats.avgPointsMutants}</dd>
        </dl>
    </section>

    <c:if test="${profile.self}">
        <section class="mt-5" aria-labelledby="played-games">
            <h2 class="mb-3" id="played-games">Played games</h2>
            <p>
                You can find a list of your past games in the
                <a href="${url.forPath(Paths.GAMES_HISTORY)}">games history</a>.
            </p>
        </section>

        <section class="mt-5" aria-labelledby="account-information">
            <h2 class="mb-3" id="account-information">Account Information</h2>
            <p>
                Your current email:
                <span class="d-inline-block px-2 ms-2 border">${profile.user.email}</span>
            </p>
            <p>
                Change your account information, password or delete your account in the
                <a href="${url.forPath(Paths.USER_SETTINGS)}"
                   title="Edit or delete your CodeDefenders account.">account settings</a>.
            </p>
        </section>
    </c:if>

</div>

<%@ include file="/jsp/footer.jsp" %>
