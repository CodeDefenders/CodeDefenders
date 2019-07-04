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
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>
<%
    List<GameClassInfo> allClasses  = (List<GameClassInfo>) request.getAttribute("classInfos");
%>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminClasses"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Classes</h3>

        <table id="tableClasses"
               class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
            <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Alias</th>
                <th>Games w/ class</th>
                <th>Limited to puzzles?</th>
                <th></th>
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
                    boolean isPuzzle = classInfo.getGameClass().isPuzzleClass();
                    boolean active = classInfo.getGameClass().isActive();
                    int gamesWithClass = classInfo.getGamesWithClass();
                    boolean deletable = classInfo.isDeletable();
                    // TODO Phil 24/06/19: add ajax call for class file content when clicking on the name
            %>

            <tr id="<%="class_row_"+classId%>" <%=active ? "" : "class=\"danger\""%>>
                <td class="col-sm-1"><%= classId%>
                </td>
                <td class="col-sm-2"><%= name %>
                </td>
                <td class="col-sm-1"><%= alias %>
                </td>
                <td class="col-sm-1"><%= gamesWithClass %>
                </td>
                <td class="col-sm-2"><%= isPuzzle ? "Yes" : "No" %>
                </td>
                <%
                    if (deletable) {
                %>
                <td style="padding-top:4px; padding-bottom:4px">
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
                <td style="padding-top:4px; padding-bottom:4px">
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

        <script>
            $(document).ready(function () {
                $('[data-toggle="tooltip"]').tooltip();

                $('#tableUsers').DataTable({
                    pagingType: "full_numbers",
                    lengthChange: false,
                    searching: true,
                    order: [[4, "desc"]],
                    "columnDefs": [{
                        "targets": 5,
                        "orderable": false
                    }, {
                        "targets": 6,
                        "orderable": false
                    }]
                });
            })
            ;
        </script>
</div>
<%@ include file="/jsp/footer.jsp" %>
