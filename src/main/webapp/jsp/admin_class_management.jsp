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
    <div>
        <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>">Click here</a> to upload a new class.
    </div>

    <table id="tableClasses"
               class="table table-striped table-hover table-responsive table-center dataTable display">
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Alias</th>
                <th>Games w/ class</th>
                <th>Delete / Set as inactive</th>
            </tr>
        </thead>
        <tbody>

            <%
                if (allClasses.isEmpty()) {
            %>

            <div class="panel panel-default">
                <div class="panel-body" style="    color: gray;    text-align: center;">
                    There are currently no classes yet.
                    <p>
                    <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>">Upload them here.</a>
                </div>
            </div>

            <%
            } else {
                for (GameClassInfo classInfo : allClasses) {
                    int classId = classInfo.getGameClass().getId();
                    String name = classInfo.getGameClass().getName();
                    String alias = classInfo.getGameClass().getAlias();
                    boolean active = classInfo.getGameClass().isActive();
                    int gamesWithClass = classInfo.getGamesWithClass();
                    boolean deletable = classInfo.isDeletable();
            %>

            <tr id="<%="class_row_"+classId%>" <%=active ? "" : "class=\"danger\""%>>
                <td class="col-sm-1"><%= classId%>
                </td>
                <td class="col-sm-2"><%= name %>
                </td>
                <td class="col-sm-2">
                    <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=classId%>">
                        <%=alias%>
                    </a>
                    <div id="modalCUTFor<%=classId%>" class="modal fade" role="dialog" style="text-align: left;" >
                        <div class="modal-dialog">
                            <!-- Modal content-->
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title"><%=alias%></h4>
                                </div>
                                <div class="modal-body">
                                <pre class="readonly-pre"><textarea
                                    class="readonly-textarea classPreview"
                                    id="sut<%=classId%>"
                                    name="cut<%=classId%>" cols="80"
                                    rows="30"></textarea></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
                <td class="col-sm-2"><%= gamesWithClass %>
                </td>
                <%
                    if (deletable) {
                %>
                <td class="col-sm-2" style="padding-top:4px; padding-bottom:4px">
                    <form id="manageClass_<%=classId%>" action="<%=request.getContextPath() + Paths.ADMIN_CLASSES%>" method="post">
                        <input type="hidden" name="formType" value="classRemoval">

                        <button class="btn btn-sm btn-danger" id="<%="delete_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                title="Delete Class from the system. No one can create games with this class anymore."
                                onclick="return confirm('Are you sure you want to delete class \'<%=name%>\' forever? You must re-upload it.');" >
                            <span class="glyphicon glyphicon-trash"></span>
                        </button>
                    </form>
                </td>
                <%
                    } else { // not deletable, so set as inactive
                %>
                <td class="col-sm-2" style="padding-top:4px; padding-bottom:4px">
                    <form id="manageClass_<%=classId%>" action="<%=request.getContextPath() + Paths.ADMIN_CLASSES%>" method="post">
                        <input type="hidden" name="formType" value="classInactive">

                    <button class="btn btn-sm btn-danger" id="<%="inactive_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                            <% if(!active) { %>
                            title="Class is already set as inactive." disabled
                            <% } else { %>
                            title="Set class as inactive. No one can create games with this class anymore."
                            <%
                                }
                            %>
                            onclick="return confirm('Are you sure you want to set class \'<%=name%>\' to inactive?');">
                        <span class="glyphicon glyphicon-trash"></span>
                    </button>
                    </form>
                </td>
                <%
                    }
                %>
            </tr>
            <%
                    }
                }
            %>
        </tbody>
    </table>
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
                mode: "text/x-java"
            });
            editor.setSize("100%", 500);
            ClassAPI.getAndSetEditorValue(textarea, editor);
        }
    });

})();
</script>

<%@ include file="/jsp/footer.jsp" %>
