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

<jsp:useBean id="profile" class="org.codedefenders.beans.user.UserProfileBean" scope="request"/>
<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
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
            <t:dashboard_pie
                    type="mutants" title="Mutants created"
                    total="${profile.stats.totalMutants}"
                    percentage="${profile.stats.aliveMutantsPercentage}"
                    label1="Mutants still alive:" value1="${profile.stats.aliveMutants}"
                    label2="Killed mutants:" value2="${profile.stats.killedMutants}"
            />

            <t:dashboard_pie
                    type="tests" title="Tests written"
                    total="${profile.stats.totalTests}"
                    percentage="${profile.stats.killingTestsPercentage}"
                    label1="Tests that killed mutants:" value1="${profile.stats.killingTests}"
                    label2="Non-killing tests:" value2="${profile.stats.nonKillingTests}"
            />

            <t:dashboard_pie
                    type="points" title="Points earned"
                    total="${profile.stats.totalPoints}"
                    percentage="${profile.stats.testPointsPercentage}"
                    label1="By writing tests:" value1="${profile.stats.totalPointsTests}"
                    label2="By creating mutants:" value2="${profile.stats.totalPointsMutants}"
            />

            <t:dashboard_pie
                    type="games" title="Games played"
                    total="${profile.stats.totalGames}"
                    percentage="${profile.stats.defenderGamesPercentage}"
                    label1="As defender:" value1="${profile.stats.defenderGames}"
                    label2="As attacker:" value2="${profile.stats.attackerGames}"
            />
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
