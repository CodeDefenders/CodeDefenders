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
<%@ page import="org.codedefenders.model.UserEntity" %>
<%@ page import="org.codedefenders.persistence.database.UserStatsDAO" %>
<%@ page import="java.util.function.BiFunction" %>
<!--%@ page import="javax.enterprise.inject.spi.CDI" %-->

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<%
    final UserEntity user = (UserEntity) request.getAttribute("user");
    final boolean isSelf = (boolean) request.getAttribute("self");
    final UserStatsDAO userStats = (UserStatsDAO) request.getAttribute("userStats");
    //    UserStatsDAO userStats = CDI.current().select(UserStatsDAO.class).get();

    pageInfo.setPageTitle(isSelf ? "My Profile" : "Profile of " + user.getUsername());

    final int userId = user.getId();

    final BiFunction<Integer, Integer, Integer> percentage
            = (subject, total) -> (subject * 100) / Math.max(total, 1); // avoid division by 0

    final int killedMutants = userStats.getNumKilledMutantsByUser(userId);
    final int aliveMutants = userStats.getNumAliveMutantsByUser(userId);
    final int totalMutants = killedMutants + aliveMutants;
    final int aliveMutantsPercentage = percentage.apply(aliveMutants, totalMutants);

    final int killingTests = userStats.getNumKillingTestsByUser(userId);
    final int nonKillingTests = userStats.getNumNonKillingTestsByUser(userId);
    final int totalTests = killingTests + nonKillingTests;
    final int killingTestsPercentage = percentage.apply(killingTests, totalTests);

    final int testPoints = userStats.getTotalPointsTestsByUser(userId);
    final int mutantPoints = userStats.getTotalPointsMutantByUser(userId);
    final double avgPointsPerTest = userStats.getAveragePointsTestByUser(userId);
    final double avgPointsPerMutant = userStats.getAveragePointsMutantByUser(userId);
    final int totalPoints = testPoints + mutantPoints;
    final int testPointsPercentage = percentage.apply(testPoints, totalPoints);
%>

<% if (login.isLoggedIn()) { %>
<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<link rel="stylesheet" href="css/piechart.css">

<div class="container form-width">
    <h1>${pageInfo.pageTitle}</h1>

    <section class="mt-5 clearfix" aria-labelledby="stats">
        <h2 class="mb-3" id="stats">Public Statistics</h2>
        <dl>
            <dt>User-ID:</dt>
            <dd><%=userId%></dd>
        </dl>

        <div class="w-50 float-start text-center">
            <h3>Mutants</h3>
            <div class="pie animate" style="--percentage:<%=aliveMutantsPercentage%>"><%=totalMutants%></div><br>
            <span class="mutants-killed">Killed mutants:</span><span><%=killedMutants%></span><br>
            <span class="mutants-alive">Alive mutants:</span><span><%=aliveMutants%></span>
        </div>

        <div class="w-50 float-start text-center">
            <h3>Tests</h3>
            <div class="pie animate" style="--percentage:<%=killingTestsPercentage%>"><%=totalTests%></div><br>
            <span class="mutants-killed">Killing tests:</span><span><%=killingTests%></span><br>
            <span class="mutants-alive">Non-killing tests:</span><span><%=nonKillingTests%></span>
        </div>

        <div class="w-50 float-start text-center">
            <h3>Points</h3>
            <div class="pie animate" style="--percentage:<%=testPointsPercentage%>"><%=totalPoints%></div><br>
            <span class="test-points">From tests:</span><span><%=testPoints%></span><br>
            <span class="mutant-points">From mutants:</span><span><%=mutantPoints%></span><br>
            <span class="test-points-avg">Average points per tests:</span><span><%=avgPointsPerTest%></span><br>
            <span class="test-points-avg">Average points per mutant:</span><span><%=avgPointsPerMutant%></span>
        </div>
    </section>

    <% if (isSelf) { %>
    <section class="mt-5" aria-labelledby="played-games">
        <h2 class="mb-3" id="played-games">Played games</h2>
        <p>
            You can find a list of your past games in the
            <a href="<%=request.getContextPath() + Paths.GAMES_HISTORY%>">games history</a>.
        </p>
    </section>

    <section class="mt-5" aria-labelledby="account-information">
        <h2 class="mb-3" id="account-information">Account Information</h2>
        <p>
            Your current email:
            <span class="d-inline-block px-2 ms-2 border"><%=user.getEmail()%></span>
        </p>
        <p>
            Change your account information, password or delete your account in the
            <a href="<%=request.getContextPath() + Paths.USER_SETTINGS%>"
               title="Edit or delete your CodeDefenders account.">account settings</a>.
        </p>
    </section>
    <% } %>

</div>

<%@ include file="/jsp/footer.jsp" %>
