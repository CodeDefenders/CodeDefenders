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

    final int attackerGames = userStats.getAttackerGamesByUser(userId);
    final int defenderGames = userStats.getDefenderGamesByUser(userId);
    final int totalGames = attackerGames + defenderGames;
    final int defenderGamesPercentage = percentage.apply(defenderGames, totalGames);
%>

<% if (login.isLoggedIn()) { %>
<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<link rel="stylesheet" href="css/dashboard.css">

<div class="container">
    <h1>${pageInfo.pageTitle}</h1>

    <section class="mt-5 statistics" aria-labelledby="stats">
        <h2 class="mb-3" id="stats">Player Statistics</h2>

        <div class="dashboards">
            <div class="dashboard-box dashboard-mutants">
                <h3>Mutants created</h3>
                <div class="pie animate no-round <%=totalMutants == 0 ? "no-data" : ""%>"
                     style="--percentage:<%=aliveMutantsPercentage%>">
                    <%=totalMutants%>
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">Killed mutants:</span>
                        <span class="legend-value"><%=killedMutants%></span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">Mutants still alive:</span>
                        <span class="legend-value"><%=aliveMutants%></span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-tests">
                <h3>Tests written</h3>

                <div class="pie animate no-round <%=totalTests == 0 ? "no-data" : ""%>"
                     style="--percentage:<%=killingTestsPercentage%>">
                    <%=totalTests%>
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">Tests that killed mutants:</span>
                        <span class="legend-value"><%=killingTests%></span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">Non-killing tests:</span>
                        <span class="legend-value"><%=nonKillingTests%></span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-points">
                <h3>Points earned</h3>

                <div class="pie animate no-round <%=totalPoints == 0 ? "no-data" : ""%>"
                     style="--percentage:<%=testPointsPercentage%>">
                    <%=totalPoints%>
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">By writing tests:</span>
                        <span class="legend-value"><%=testPoints%></span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">By creating mutants:</span>
                        <span class="legend-value"><%=mutantPoints%></span>
                    </div>
                </div>
            </div>

            <div class="dashboard-box dashboard-games">
                <h3>Games played</h3>

                <div class="pie animate no-round <%=totalGames == 0 ? "no-data" : ""%>"
                     style="--percentage:<%=defenderGamesPercentage%>">
                    <%=totalGames%>
                </div>

                <div>
                    <div class="legend">
                        <span class="legend-title">As defender:</span>
                        <span class="legend-value"><%=defenderGames%></span>
                    </div>
                    <div class="legend">
                        <span class="legend-title">As attacker:</span>
                        <span class="legend-value"><%=attackerGames%></span>
                    </div>
                </div>
            </div>
        </div>

        <dl class="other-stats mt-3">
            <dt>Average points per tests:</dt>
            <dd><%=avgPointsPerTest%>
            </dd>

            <dt>Average points per mutant:</dt>
            <dd><%=avgPointsPerMutant%>
            </dd>
        </dl>
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
