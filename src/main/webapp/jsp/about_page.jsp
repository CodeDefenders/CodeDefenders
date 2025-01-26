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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<c:set var="title" value="About Code Defenders"/>

<p:main_page title="${title}">
    <div class="container">

        <h2 class="mb-4">${title}</h2>

        <c:if test="${aboutPage.version != null}">
            <h3>Version</h3>
            <div class="bg-light rounded-3 p-3 mb-3">
                <p class="mb-0">
                    This is Code Defenders running on git commit ${aboutPage.version}.
                </p>
                <c:if test="${aboutPage.dirty}">
                    <p class="mb-0">
                        The version is dirty, i.e., the working directory contains uncommitted changes.
                    </p>
                </c:if>
            </div>
        </c:if>

        <h3 class="mt-4 mb-3">Source Code</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <p class="mb-0">
                CodeDefenders is developed and maintained at the
                <a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Chair of Software Engineering&nbspII</a>
                at the University of Passau and the
                <a href="https://www2.le.ac.uk/departments/informatics/people/jrojas">University of Leicester</a>.
            </p>
            <p class="mb-0">
                Code Defenders is an open source project.
                See the
                <a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a>
                project page.
            </p>
        </div>

        <h3 class="mt-4 mb-3">Contributors</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <ul>
                <li><a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Gordon Fraser (University of Passau)</a></li>
                <li><a href="http://jmrojas.github.io/">Jose Miguel Rojas (University of Leicester)</a></li>
            </ul>
            <ul class="mb-0">
                <li>Ben Clegg (The University of Sheffield)</li>
                <li>Alexander Degenhart (University of Passau)</li>
                <li>Sabina Galdobin (University of Passau)</li>
                <li><a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Alessio Gambi (University of Passau)</a></li>
                <li>Marvin Kreis (University of Passau)</li>
                <li>Kassian K&ouml;ck (University of Passau)</li>
                <li>Rob Sharp (The University of Sheffield)</li>
                <li>Lorenz Wendlinger (University of Passau)</li>
                <li><a href="https://github.com/werli">Phil Werli</a> (University of Passau)</li>
                <li>Thomas White (The University of Sheffield)</li>
            </ul>
        </div>

        <h3 class="mt-4 mb-3">Supporters</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <ul class="mb-0">
                <li><a href="https://impress-project.eu/">IMPRESS Project</a> (Improving Engagement of Students in Software Engineering Courses through Gamification)</li>
                <li><a href="https://www.sheffield.ac.uk/sure">SURE (Sheffield Undergraduate Research Experience)</a></li>
                <li><a href="http://royalsociety.org/">Royal Society (Grant RG160969)</a></li>
            </ul>
        </div>

        <h3 class="mt-4 mb-3">Research</h3>
        <div class="bg-light rounded-3 p-3 mb-3">
            <div class="ps-3">
                <jsp:include page="research.jsp"/>
            </div>
        </div>

    </div>
</p:main_page>
