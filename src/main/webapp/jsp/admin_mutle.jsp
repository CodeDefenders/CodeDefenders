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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.util.Paths" %>

<p:main_page title="${i18n.tr('Mutle administration')}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath('/css/specific/admin.css')}" rel="stylesheet">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <t:admin_navigation activePage="adminMutle"/>

            <div class="d-flex justify-content-between align-items-start mb-4">
                <div>
                    <h1>${i18n.tr('Mutle administration')}</h1>
                    <p class="text-muted mb-0">
                        ${i18n.tr('Import fixture classes and publish one case for each lecture.')}
                    </p>
                </div>
            </div>

            <section class="card mb-4" aria-labelledby="mutle-fixtures-heading">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h2 class="h5 mb-0" id="mutle-fixtures-heading">${i18n.tr('Fixture inventory')}</h2>
                    <span class="text-muted small">${i18n.tr('Mutants remain hidden from players.')}</span>
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${empty mutleFixtures}">
                            <p class="mb-0 text-muted">${i18n.tr('No Mutle fixtures are installed.')}</p>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-sm align-middle mb-0">
                                    <thead>
                                    <tr>
                                        <th scope="col">${i18n.tr('Fixture')}</th>
                                        <th scope="col">${i18n.tr('Class')}</th>
                                        <th scope="col" class="text-end">${i18n.tr('Mutants')}</th>
                                        <th scope="col">${i18n.tr('Status')}</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="fixture" items="${mutleFixtures}">
                                        <tr>
                                            <td><code>${fixture.classKey}</code></td>
                                            <td><c:out value="${fixture.title}" /></td>
                                            <td class="text-end">${fixture.mutantCount}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${fixture.publishedCount > 0}">
                                                        <span class="badge text-bg-success">${i18n.tr('Scheduled')}</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge text-bg-secondary">${i18n.tr('Available')}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>

            <section class="card mb-4" aria-labelledby="mutle-upload-heading">
                <div class="card-header">
                    <h2 class="h5 mb-0" id="mutle-upload-heading">${i18n.tr('Add fixture class')}</h2>
                </div>
                <div class="card-body">
                    <p class="text-muted">
                        ${i18n.tr('Upload one class and its mutants as a Mutle fixture ZIP. Validate puzzle quality offline before upload.')}
                    </p>
                    <p class="text-muted small">
                        ${i18n.tr('The ZIP needs class.json, the class source, tests/, and mutants/ at its root or inside one class folder.')}
                    </p>
                    <form action="${url.forPath(Paths.ADMIN_MUTLE)}" method="post"
                          enctype="multipart/form-data" class="row gy-2 align-items-end">
                        <input type="hidden" name="formType" value="uploadFixture">
                        <div class="col-md-8">
                            <label class="form-label" for="mutle-fixture-zip">${i18n.tr('Fixture ZIP')}</label>
                            <input class="form-control" type="file" id="mutle-fixture-zip" name="fixtureZip"
                                   accept=".zip" required>
                        </div>
                        <div class="col-md-auto">
                            <button class="btn btn-outline-primary" type="submit">
                                ${i18n.tr('Upload fixture')}
                                <i class="fa fa-upload ms-1" aria-hidden="true"></i>
                            </button>
                        </div>
                    </form>
                </div>
            </section>

            <section class="card mb-4" aria-labelledby="mutle-schedule-heading">
                <div class="card-header">
                    <h2 class="h5 mb-0" id="mutle-schedule-heading">${i18n.tr('Publication schedule')}</h2>
                </div>
                <div class="card-body">
                    <p class="text-muted">${i18n.tr('Published cases stay locked; future dates and fixture assignments can be changed.')}</p>
                    <c:if test="${mutleNeedsFutureCase}">
                        <div class="alert alert-warning" role="status">
                            ${i18n.tr('No future case is scheduled. The current case remains available until you add one.')}
                        </div>
                    </c:if>
                    <c:if test="${empty mutlePublications}">
                        <form action="${url.forPath(Paths.ADMIN_MUTLE)}" method="post"
                              class="row gy-2 gx-3 align-items-end mb-4">
                            <input type="hidden" name="formType" value="scheduleInitial">
                            <div class="col-sm-5 col-md-4">
                                <label class="form-label" for="mutle-first-release">${i18n.tr('First release (UTC)')}</label>
                                <input class="form-control" type="datetime-local" id="mutle-first-release"
                                       name="firstReleaseAt" value="${mutleFirstReleaseAt}" required>
                                <div class="form-text">${i18n.tr('Interpreted as UTC. This schedules 15 weekly cases.')}</div>
                            </div>
                            <div class="col-sm-auto">
                                <button class="btn btn-primary" type="submit">${i18n.tr('Schedule 15 weekly cases')}</button>
                            </div>
                        </form>
                    </c:if>

                    <div class="table-responsive">
                        <table class="table table-sm align-middle mb-0">
                            <thead>
                            <tr>
                                <th scope="col">${i18n.tr('Case')}</th>
                                <th scope="col">${i18n.tr('Release (UTC)')}</th>
                                <th scope="col">${i18n.tr('Fixture')}</th>
                                <th scope="col">${i18n.tr('Status')}</th>
                                <th scope="col" class="text-end">${i18n.tr('Action')}</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty mutlePublications}">
                                    <tr><td colspan="5" class="text-muted">${i18n.tr('No cases are scheduled yet.')}</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="publication" items="${mutlePublications}">
                                        <tr>
                                            <td class="text-nowrap">
                                                <form id="mutle-publication-${publication.series}"
                                                      action="${url.forPath(Paths.ADMIN_MUTLE)}" method="post">
                                                <input type="hidden" name="formType" value="savePublication">
                                                <input type="hidden" name="series" value="${publication.series}">
                                                </form>
                                                ${publication.series}
                                            </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${publication.locked}">
                                                            ${publication.releaseAtLabel} UTC
                                                        </c:when>
                                                        <c:otherwise>
                                                            <input class="form-control form-control-sm" type="datetime-local"
                                                                   name="releaseAt" value="${publication.releaseAtInput}" form="mutle-publication-${publication.series}"
                                                                   aria-label="${i18n.tr('Release (UTC)')} ${publication.series}" required>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${publication.locked}">
                                                            <code>${publication.fixtureKey}</code>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <select class="form-select form-select-sm" name="fixtureKey"
                                                                    form="mutle-publication-${publication.series}" required>
                                                                <c:forEach var="fixture" items="${mutleFixtureEntries}">
                                                                    <option value="${fixture.fixtureKey}"
                                                                            ${fixture.fixtureKey == publication.fixtureKey ? 'selected' : ''}>
                                                                        <c:out value="${fixture.title}" /> · <c:out value="${fixture.fixtureKey}" />
                                                                    </option>
                                                                </c:forEach>
                                                            </select>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${publication.locked}">
                                                            <span class="badge text-bg-success">${i18n.tr('Published')}</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge text-bg-secondary">${i18n.tr('Future')}</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-end">
                                                    <c:if test="${not publication.locked}">
                                                        <button class="btn btn-sm btn-outline-primary"
                                                                form="mutle-publication-${publication.series}"
                                                                type="submit">${i18n.tr('Save')}</button>
                                                    </c:if>
                                                </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${not empty mutlePublications}">
                        <form action="${url.forPath(Paths.ADMIN_MUTLE)}" method="post"
                              class="row gy-2 gx-3 align-items-end mt-4">
                            <input type="hidden" name="formType" value="savePublication">
                            <input type="hidden" name="series" value="${mutleNextSeries}">
                            <div class="col-md-2">
                                <label class="form-label" for="mutle-next-series">${i18n.tr('Next case')}</label>
                                <input class="form-control" id="mutle-next-series" value="${mutleNextSeries}" disabled>
                            </div>
                            <div class="col-md-3">
                                <label class="form-label" for="mutle-next-release">${i18n.tr('Release (UTC)')}</label>
                                <input class="form-control" type="datetime-local" id="mutle-next-release"
                                       name="releaseAt" value="${mutleNextReleaseAt}" required>
                            </div>
                            <div class="col-md-5">
                                <label class="form-label" for="mutle-next-fixture">${i18n.tr('Fixture')}</label>
                                <select class="form-select" id="mutle-next-fixture" name="fixtureKey" required>
                                    <c:forEach var="fixture" items="${mutleFixtureEntries}">
                                        <option value="${fixture.fixtureKey}"><c:out value="${fixture.title}" /> · <c:out value="${fixture.fixtureKey}" /></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-auto">
                                <button class="btn btn-outline-primary" type="submit">${i18n.tr('Add case')}</button>
                            </div>
                        </form>
                    </c:if>
                </div>
            </section>
        </div>
    </jsp:body>
</p:main_page>
