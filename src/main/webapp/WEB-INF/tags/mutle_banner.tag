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
<%@ tag import="org.codedefenders.util.Paths" %>
<%@ attribute name="fixture" required="false" type="org.codedefenders.arcade.mutle.service.MutleFixtureView" %>
<%@ attribute name="currentCase" required="false" type="org.codedefenders.arcade.mutle.service.MutleStateView" %>
<%@ attribute name="homepage" required="false" type="java.lang.Boolean" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<c:if test="${not homepage or not empty fixture}">
<c:set var="titleId" value="${homepage ? 'landing-mutle-title' : 'current-mutle-title'}"/>
<section class="mutle-hero p-4 ${homepage ? '' : 'p-md-5'} mt-4 mb-4" aria-labelledby="${titleId}">
    <c:if test="${homepage}">
        <div class="d-md-flex gap-5">
            <div class="left">
    </c:if>

    <h2 id="${titleId}">
        ${homepage
            ? i18n.tr('Mutle: weekly mutation puzzles')
            : (empty fixture ? i18n.tr('Mutle coming soon') : i18n.tr('This week\'s mutle'))}
    </h2>

    <c:choose>
        <c:when test="${not empty currentCase}">
            <p class="mb-2">${i18n.tr('Find the hidden change in Java code with six tests.')}</p>
            <c:if test="${not homepage}">
                <p class="mb-2">${i18n.tr('Series')} ${currentCase.series} · <c:out value="${currentCase.title}" /></p>
            </c:if>
        </c:when>
        <c:when test="${not empty fixture}">
            <p class="mb-2">${i18n.tr('Find the hidden change in Java code with six tests.')}</p>
            <c:if test="${not homepage}">
                <p class="mb-2">${i18n.tr('Series')} ${fixture.series} · <c:out value="${fixture.title}" /></p>
            </c:if>
        </c:when>
        <c:otherwise>
            <p class="lead mb-2">
                ${i18n.tr('The first mutle has not been published yet.')} <br>
                ${i18n.tr('Check back when the semester begins.')}
            </p>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty currentCase}">
        <p class="mb-3">
            <span class="badge bg-${currentCase.solved ? 'success' : (currentCase.failed ? 'danger' : (currentCase.expired ? 'secondary' : 'primary'))} text-white">
                <i class="fa fa-${currentCase.solved ? 'check' : (currentCase.failed ? 'close' : (currentCase.expired ? 'hourglass-end' : 'cog'))}"></i>
                ${currentCase.solved ? i18n.tr('Solved') : (currentCase.failed ? i18n.tr('Failed in six') : (currentCase.expired ? i18n.tr('Expired') : i18n.tr('In progress')))}
            </span>
            <span class="ms-2">${currentCase.acceptedGuesses}/${currentCase.acceptedGuesses + currentCase.remainingGuesses} ${i18n.tr('guesses')}</span>
        </p>
    </c:if>

    <c:if test="${homepage}">
                <p class="small mb-2">${i18n.tr('Guest progress stays in this browser session. Log in to play with your account and earn achievements.')}</p>
            </div>
            <div class="right d-flex flex-column justify-content-center flex-shrink-0">
    </c:if>
    <c:if test="${not homepage}">
        <div class="mt-3">
    </c:if>

    <c:choose>
        <c:when test="${empty fixture}">
            <!-- no button -->
        </c:when>
        <c:when test="${empty currentCase}">
            <a class="btn btn-primary btn-lg" href="${url.forPath(Paths.MUTLE_PLAY)}">
                ${homepage ? i18n.tr('Play Mutle') : i18n.tr('Solve current mutle')}
            </a>
        </c:when>
        <c:otherwise>
            <a class="btn btn-primary btn-lg" href="${url.forPath(Paths.MUTLE_PLAY)}?gameId=${currentCase.gameId}">
                ${currentCase.solved || currentCase.failed || currentCase.expired
                        ? i18n.tr('View current mutle') : i18n.tr('Continue current mutle')}
            </a>
        </c:otherwise>
    </c:choose>

            </div>
    <c:if test="${homepage}">
        </div>
    </c:if>
</section>
</c:if>
