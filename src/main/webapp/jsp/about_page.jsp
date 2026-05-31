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
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="aboutPage" type="org.codedefenders.beans.about.AboutPageBean"--%>

<c:set var="title" value="${i18n.tr('About Code Defenders')}"/>

<p:main_page title="${title}">
    <div class="container">

        <h2 class="mb-4">${title}</h2>

        <c:if test="${aboutPage.version != null || aboutPage.gitCommitHash != null}">
            <h3>${i18n.tr('Version')}</h3>
            <div class="bg-light rounded-3 p-3 mb-3">
                <c:if test="${aboutPage.version != null}">
                    <p class="mb-0">
                            ${i18n.tr('This is Code Defenders version {0}.', aboutPage.version)}
                    </p>
                </c:if>
                <c:if test="${aboutPage.gitCommitHash != null}">
                    <p class="mb-0">
                        <c:choose>
                            <c:when test="${aboutPage.version == null}">
                                ${i18n.tr('This is Code Defenders running on git commit {0}.', aboutPage.gitCommitHash)}
                            </c:when>
                            <c:otherwise>
                                ${i18n.tr('The git commit hash is {0}.', aboutPage.gitCommitHash)}
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <c:if test="${aboutPage.dirty}">
                        <p class="mb-0">
                                ${i18n.tr('The version is dirty, i.e., the working directory contains uncommitted changes.')}
                        </p>
                    </c:if>
                </c:if>
            </div>
        </c:if>

        <h3 class="mt-4 mb-3">${i18n.tr('Source Code')}</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <p class="mb-0">
                    ${i18n.tr('CodeDefenders is developed and maintained at the')}
                        <a href="https://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">${i18n.tr('Chair of Software Engineering&nbsp;II')}</a>
                    ${i18n.tr('at the University of Passau.')}
            </p>
            <p class="mb-0">
                    ${i18n.tr('Code Defenders is an open source project.')}
                    ${i18n.tr('See the')}
                <a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a>
                    ${i18n.tr('project page.')}
            </p>
        </div>

        <h3 class="mt-4 mb-3">${i18n.tr('Contributors')}</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <ul class="mb-0 links-no-color">
                <li>Ben Clegg (${i18n.tr('The University of Sheffield')})</li>
                <li>Alexander Degenhart (${i18n.tr('University of Passau')})</li>
                <li><a href="https://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Gordon Fraser (${i18n.tr('University of Passau')})</a></li>
                <li>Sabina Galdobin (${i18n.tr('University of Passau')})</li>
                <li><a href="https://publications.ait.ac.at/de/persons/alessio-gambi/">Alessio Gambi (AIT)</a></li>
                <li><a href="https://tim-greller.de">Tim Greller (${i18n.tr('University of Passau')})</a></li>
                <li>Marvin Kreis (${i18n.tr('University of Passau')})</li>
                <li>Kassian K&ouml;ck (${i18n.tr('University of Passau')})</li>
                <li>Aaron Prott (${i18n.tr('University of Passau')})</li>
                <li><a href="https://jmrojas.github.io/">Jose Miguel Rojas (${i18n.tr('The University of Sheffield')})</a></li>
                <li>Rob Sharp (${i18n.tr('The University of Sheffield')})</li>
                <li>Philipp Straubinger (${i18n.tr('University of Passau')})</li>
                <li>Lorenz Wendlinger (${i18n.tr('University of Passau')})</li>
                <li><a href="https://github.com/werli">Phil Werli (${i18n.tr('University of Passau')})</a></li>
                <li>Thomas White (${i18n.tr('The University of Sheffield')})</li>
            </ul>
        </div>

        <h3 class="mt-4 mb-3">${i18n.tr('Supporters')}</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <ul class="mb-0">
                <li><a href="https://www.dfg.de">DFG Project</a> FR 2955/2-1 (QuestWare: ${i18n.tr('Gamifying the Quest for Software Tests')})</li>
                <li><a href="https://impress-project.eu/">IMPRESS Project</a> (${i18n.tr('Improving Engagement of Students in Software Engineering Courses through Gamification')})
                </li>
                <li><a href="https://www.sheffield.ac.uk/sure">SURE
                    (${i18n.tr('Sheffield Undergraduate Research Experience')})</a>
                </li>
                <li><a href="https://royalsociety.org/">Royal Society</a> (${i18n.tr('Grant RG160969')})</li>
            </ul>
        </div>

        <h3 class="mt-4 mb-3">${i18n.tr('Research')}</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <div class="ps-3">
                <jsp:include page="research.jsp"/>
            </div>
        </div>

    </div>
</p:main_page>
