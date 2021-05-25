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
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.GameClassInfo" %>

<jsp:include page="/jsp/header_main.jsp"/>

<%
    List<GameClassInfo> allClasses  = (List<GameClassInfo>) request.getAttribute("classInfos");
%>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminClasses"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <h3>Classes</h3>

    <%
        if (allClasses.isEmpty()) {
    %>

        <div class="card">
            <div class="card-body text-center text-muted">
                There are no classes yet.
                <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>">Click here</a>
                to upload a new class.
            </div>
        </div>

    <%
        } else {
    %>

        <table id="tableClasses" class="table table-striped">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Alias</th>
                    <th>#Games</th>
                    <th>Manage Class</th>
                </tr>
            </thead>
            <tbody>

                <%
                    for (GameClassInfo classInfo : allClasses) {
                        int classId = classInfo.getGameClass().getId();
                        String name = classInfo.getGameClass().getName();
                        String alias = classInfo.getGameClass().getAlias();
                        boolean active = classInfo.getGameClass().isActive();
                        int gamesWithClass = classInfo.getGamesWithClass();
                        boolean deletable = classInfo.isDeletable();
                %>

                    <tr id="<%="class_row_" + classId%>" <%=active ? "" : "class=\"text-muted\""%>>
                        <td><%=classId%></td>
                        <td><%=name%></td>
                        <td>
                            <a href="#" data-bs-toggle="modal" data-bs-target="#modalCUTFor<%=classId%>">
                                <%=alias%>
                            </a>
                            <div id="modalCUTFor<%=classId%>" class="modal fade" tabindex="-1">
                                <div class="modal-dialog">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <h5 class="modal-title"><%=alias%></h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body">
                                        <pre class="readonly-pre"><textarea
                                            class="readonly-textarea classPreview"
                                            id="sut<%=classId%>"
                                            name="cut<%=classId%>" cols="80"
                                            rows="30"></textarea></pre>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td><%=gamesWithClass%></td>
                        <td>
                            <%
                                if (deletable) {
                            %>
                                <form id="manageClass_<%=classId%>" action="<%=request.getContextPath() + Paths.ADMIN_CLASSES%>" method="post">
                                    <input type="hidden" name="formType" value="classRemoval">
                                    <button class="btn btn-sm btn-danger" id="<%="delete_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                            title="Delete class from the system. This class won't be available for games afterwards."
                                            onclick="return confirm('Are you sure you want to delete class \'<%=name%>\' forever? This cannot be undone.');">
                                        <i class="fa fa-trash"></i>
                                    </button>
                                </form>
                            <%
                                } else {
                            %>
                                <form id="manageClass_<%=classId%>" action="<%=request.getContextPath() + Paths.ADMIN_CLASSES%>" method="post">
                                    <input type="hidden" name="formType" value="classInactive">

                                    <button class="btn btn-sm btn-danger" id="<%="inactive_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                            <% if (!active) { %>
                                                title="Class is already inactive." disabled
                                            <% } else { %>
                                                title="Set class as inactive. This class won't be available for games afterwards."
                                            <% } %>
                                            onclick="return confirm('Are you sure you want to set class \'<%=name%>\' to inactive?');">
                                        <i class="fa fa-power-off"></i>
                                    </button>
                                </form>
                            <%
                                }
                            %>
                        </td>
                    </tr>
                <%
                    }
                %>
            </tbody>
        </table>

        <p>
            <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>">Click here</a>
            to upload a new class.
        </p>
    <%
        }
    %>

</div>

<script>
(function () {

    $('.modal').on('shown.bs.modal', function () {
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

})();
</script>

<%@ include file="/jsp/footer.jsp" %>
