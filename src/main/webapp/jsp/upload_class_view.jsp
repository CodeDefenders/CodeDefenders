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

<%@ page import="org.codedefenders.database.FeedbackDAO" %>
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.*" %>
<%@ page import="java.util.Map" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Upload Class"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">

    <h2 class="mb-3">${pageInfo.pageTitle}</h2>
    <form id="formUpload" action="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>"
          method="post" enctype="multipart/form-data"
          class="needs-validation form-width"
          autocomplete="off">
        <input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">

        <div class="row mb-3 g-3">
            <div class="col-12">
                <label class="form-label" for="classAlias">Class Alias</label>
                <input id="classAlias" name="classAlias" type="text" class="form-control" placeholder="Class Alias"
                       pattern="[a-zA-Z0-9]*">
                <div class="invalid-feedback">Please provide a valid alias (or leave empty).</div>
                <div class="form-text">
                    Optional class alias.
                    Name with no whitespaces or special characters.
                    If empty, the name of the class will be used.
                </div>
            </div>

            <div class="col-12">
                <label class="form-label" for="fileUploadCUT">Java File</label>
                <input id="fileUploadCUT" name="fileUploadCUT" type="file" class="form-control" accept=".java" required>
                <div class="invalid-feedback">Please provide a <code>.java</code> file.</div>
                <div class="form-text">The .java file of the class.</div>
            </div>

            <div class="col-md-6 col-12">
                <label for="testingFramework" class="form-label">Testing Framework</label>
                <select id="testingFramework" name="testingFramework" class="form-select" required>
                    <% for (TestingFramework tf : TestingFramework.values()) { %>
                        <option value="<%=tf.name()%>" <%=tf == TestingFramework.JUNIT4 ? "selected" : ""%>>
                            <%=tf.getDescription()%>
                        </option>
                    <% } %>
                </select>
                <div class="form-text">The testing framework that will be used in games with the class.</div>
            </div>

            <div class="col-md-6 col-12">
                <label for="assertionLibrary" class="form-label">Assertion Library</label>
                <select id="assertionLibrary" name="assertionLibrary" class="form-select" required>
                    <% for (AssertionLibrary al : AssertionLibrary.values()) { %>
                        <option value="<%=al.name()%>" <%=al == AssertionLibrary.JUNIT4_HAMCREST ? "selected" : ""%>>
                            <%=al.getDescription()%>
                        </option>
                    <% } %>
                </select>
                <div class="form-text">The assertion library used to write tests for this class.</div>
            </div>
        </div>

        <details>
            <summary>
                Advanced Upload Options
            </summary>
            <div class="mt-3">
                <div class="form-check">
                    <input class="form-check-input" id="mockingEnabled" type="checkbox" name="enableMocking" value="isMocking">
                    <label class="form-check-label" for="mockingEnabled">Enable Mocking for this class</label>
                </div>

                <h4 class="mt-4 mb-3">Upload Dependencies (optional)</h4>
                <div class="row g-3 mb-3">
                    <div class="col-12">
                        <input id="fileUploadDependency" name="fileUploadDependency" type="file" class="form-control" accept=".zip">
                    </div>
                    <div class="col-12">
                        <p>If the class under test has dependencies, you can upload them here.</p>
                        <ul>
                            <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                            <li>Dependencies may contain packages.</li>
                        </ul>
                        <p>Example:</p>
                        <pre class="mb-0 p-3 bg-light">deps
&#x251c;&#x2500; Event.java
&#x2514;&#x2500; events
    &#x251c;&#x2500; StartEvent.java
    &#x2514;&#x2500; StopEvent.java</pre>
                    </div>
                </div>

                <h4 class="mt-4 mb-3">Upload Mutants (optional)</h4>
                <div class="row g-3 mb-3">
                    <div class="col-12">
                        <input id="fileUploadMutant" name="fileUploadMutant" type="file" class="form-control" accept=".zip">
                    </div>
                    <div class="col-12">
                        <p>Mutants uploaded with a class under test can be used to initialize games with existing mutants.</p>
                        <ul>
                            <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                            <li>All mutants must have the same class name as the class under test.</li>
                        </ul>
                        <p>Example:</p>
                        <pre class="mb-0 p-3 bg-light">mutants
&#x251c;&#x2500; 01
&#x2502;   &#x2514;&#x2500; EventBus.java
&#x2514;&#x2500; 02
    &#x2514;&#x2500; EventBus.java</pre>
                    </div>
                </div>

                <h4 class="mt-4 mb-3">Upload Tests (optional)</h4>
                <div class="row g-3">
                    <div class="col-12">
                        <input id="fileUploadTest" name="fileUploadTest" type="file" class="form-control" accept=".zip">
                    </div>
                    <div class="col-12">
                        <p>Tests uploaded with a class under test can be used to initialize games with existing tests.</p>
                        <ul>
                            <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                            <li>Multiple tests can have the same name, but they don't have to.</li>
                        </ul>
                        <p>Example:</p>
                        <pre class="mb-0 p-3 bg-light">tests
&#x251c;&#x2500; 01
&#x2502;   &#x2514;&#x2500; TestEventBus.java
&#x2514;&#x2500; 02
    &#x2514;&#x2500; TestEventBus.java</pre>
                    </div>
                </div>
            </div>
        </details>

        <div class="row mt-3">
            <c:choose>
                <c:when test="${empty param.origin}">
                    <div class="col-auto">
                        <button id="upload" type="submit" class="btn btn-primary">
                            Upload
                        </button>
                        <%-- TODO Where do we cancel to in this case? --%>
                    </div>
                </c:when>
                <c:otherwise>
                    <input type="hidden" name="origin" value="${param.origin}"/>
                    <div class="col-auto">
                        <div class="btn-group">
                            <button id="upload" type="submit" class="btn btn-primary">
                                Upload
                            </button>
                            <button id="upload-and-stay" type="submit" class="btn btn-outline-primary" name="disableAutomaticRedirect" value="disabled">
                                Upload and stay on this page
                            </button>
                        </div>
                        <a href="${pageContext.request.contextPath}${param.origin}" id="cancel" class="btn btn-outline-primary">Cancel</a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </form>

    <h2 class="mt-4">Uploaded Classes</h2>
    <%
        List<GameClass> gameClasses = GameClassDAO.getAllPlayableClasses();
        Map<Integer, Double> avgMutationDifficulties = FeedbackDAO.getAverageMutationDifficulties();
        Map<Integer, Double> avgTestDifficulties = FeedbackDAO.getAverageTestDifficulties();
    %>
    <table class="table table-striped" id="tableUploadedClasses">
        <thead>
            <tr>
                <th>ID</th>
                <th>Class name (alias)</th>
                <th>Available dependencies/tests/mutants</th>
                <th>Mutation Difficulty</th>
                <th>Testing Difficulty</th>
                <th>Testing Framework</th>
                <th>Assertion Library</th>
            </tr>
        </thead>
        <tbody>
            <%
                if (gameClasses.isEmpty()) {
            %>
                <tr>
                    <td colspan="100" class="text-center">No classes uploaded.</td>
                </tr>
            <%
                } else {
                    for (GameClass c : gameClasses) {
                        Double mutationDiff = avgMutationDifficulties.get(c.getId());
                        Double testingDiff = avgTestDifficulties.get(c.getId());
            %>
                <tr>
                    <td><%=c.getId()%></td>
                    <td>
                        <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-<%=c.getId()%>">
                            <%=c.getBaseName()%> <%=c.getBaseName().equals(c.getAlias()) ? "" : "(alias "+c.getAlias()+")"%>
                        </a>
                        <% pageContext.setAttribute("classId", c.getId()); %>
                        <% pageContext.setAttribute("classAlias", c.getAlias()); %>
                        <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-${classId}"/>
                    </td>
                    <td><%=GameClassDAO.getMappedDependencyIdsForClassId(c.getId()).size()%>/<%=GameClassDAO.getMappedTestIdsForClassId(c.getId()).size()%>/<%=GameClassDAO.getMappedMutantIdsForClassId(c.getId()).size()%></td>
                    <td><%=mutationDiff != null ? String.valueOf(mutationDiff) : ""%></td>
                    <td><%=testingDiff != null ? String.valueOf(testingDiff) : ""%></td>
                    <td><%=c.getTestingFramework().getDescription()%></td>
                    <td><%=c.getAssertionLibrary().getDescription()%></td>
                </tr>
            <%
                    }
                }
            %>
        </tbody>
    </table>
</div>

<script>
    $(document).ready(function () {
        $('#tableUploadedClasses').DataTable({
            paging: false,
            order: [[0, 'desc']],
            scrollY: '600px',
            scrollCollapse: true,
            language: {info: 'Showing _TOTAL_ entries'}
        });
    });
</script>

<%@ include file="/jsp/footer.jsp" %>
