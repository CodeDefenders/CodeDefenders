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
<%@ page import="org.codedefenders.database.FeedbackDAO" %>
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.*" %>
<%@ page import="java.util.Map" %>

<jsp:include page="/jsp/header_main.jsp"/>

<link href="${pageContext.request.contextPath}/css/uploadcut.css" rel="stylesheet">

<div class="container">

    <h2>Upload Class</h2>
    <form id="formUpload" action="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>"
          method="post" enctype="multipart/form-data"
          class="needs-validation">
        <input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">

        <div class="row mb-3 g-3">
            <div class="col-sm-12">
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

            <div class="col-sm-12">
                <label class="form-label" for="fileUploadCUT">Java File</label>
                <input id="fileUploadCUT" name="fileUploadCUT" type="file" class="form-control" accept=".java" required>
                <div class="invalid-feedback">Please provide a <code>.java</code> file.</div>
                <div class="form-text">The <code>.java</code> file of the class.</div>
            </div>

            <div class="col-sm-12 col-md-6">
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

            <div class="col-sm-12 col-md-6">
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

        <div class="accordion">
            <div class="accordion-item">
                <h2 class="accordion-header" id="advanced-options-heading">
                    <button class="accordion-button collapsed" type="button"
                            data-bs-toggle="collapse" data-bs-target="#advanced-options-collapse"
                            aria-expanded="false" aria-controls="advanced-options-collapse">
                        Advanced Upload Options
                    </button>
                </h2>
                <div id="advanced-options-collapse" class="accordion-collapse collapse" aria-labelledby="advanced-options-heading">
                    <div class="accordion-body">
                        <div class="form-check">
                            <input class="form-check-input" id="mockingEnabled" type="checkbox" name="enableMocking" value="isMocking">
                            <label class="form-check-label" for="mockingEnabled">Enable Mocking for this class</label>
                        </div>

                        <h4 class="mt-4 mb-3">Upload Dependencies (optional)</h4>
                        <div class="row g-3 mb-3">
                            <div class="col-sm-12">
                                <input id="fileUploadDependency" name="fileUploadDependency" type="file" class="form-control" accept=".zip">
                            </div>
                            <div class="col-sm-12">
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
                            <div class="col-sm-12">
                                <input id="fileUploadMutant" name="fileUploadMutant" type="file" class="form-control" accept=".zip">
                            </div>
                            <div class="col-sm-12">
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
                        <div class="row g-3 mb-3">
                            <div class="col-sm-12">
                                <input id="fileUploadTest" name="fileUploadTest" type="file" class="form-control" accept=".zip">
                            </div>
                            <div class="col-sm-12">
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
                </div>
            </div>
        </div>

        <div class="row mt-3">
            <div class="col-auto">
                <button id="upload" type="submit" class="btn btn-primary">
                    Upload
                </button>
            </div>
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
                <tr><td colspan="7">No classes uploaded.</td></tr>
            <%
                } else {
                    for (GameClass c : gameClasses) {
                        Double mutationDiff = avgMutationDifficulties.get(c.getId());
                        Double testingDiff = avgTestDifficulties.get(c.getId());
            %>
                <tr>
                    <td><%=c.getId()%></td>
                    <td>
                        <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=c.getId()%>">
                            <%=c.getBaseName()%> <%=c.getBaseName().equals(c.getAlias()) ? "" : "(alias "+c.getAlias()+")"%>
                        </a>
                        <div id="modalCUTFor<%=c.getId()%>" class="modal fade" role="dialog" style="text-align: left;" tabindex="-1">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                                        <h4 class="modal-title"><%=c.getBaseName()%></h4>
                                    </div>
                                    <div class="modal-body">
                                        <pre class="readonly-pre"><textarea
                                                class="readonly-textarea classPreview" id="sut<%=c.getId()%>"
                                                name="cut<%=c.getId()%>" cols="80"
                                                rows="30"></textarea></pre>
                                    </div>
                                    <div class="modal-footer">
                                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
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
    $('.modal').on('shown.bs.modal', function() {
        let codeMirrorContainer = $(this).find(".CodeMirror")[0];
        if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
            codeMirrorContainer.CodeMirror.refresh();
        } else {
            let textarea = $(this).find('textarea')[0];
            let editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: false,
                readOnly: true,
                mode: "text/x-java",
                autoRefresh: true
            });
            editor.setSize("100%", 500);
            ClassAPI.getAndSetEditorValue(textarea, editor);
        }
    });

    $(document).ready(function () {
        $('#tableUploadedClasses').DataTable({
            paging: false,
            order: [[0, 'desc']],
            scrollY: '600px',
            scrollCollapse: true,
            paging: false,
            language: {info: 'Showing _TOTAL_ entries'}
        });
    });
</script>

<%@ include file="/jsp/footer.jsp" %>
