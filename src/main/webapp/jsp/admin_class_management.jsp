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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.GameClassInfo" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Class Management"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    List<GameClassInfo> allClasses  = (List<GameClassInfo>) request.getAttribute("classInfos");
%>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminClasses"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <table id="tableClasses" class="table table-v-align-middle table-striped">
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

            <% if (allClasses.isEmpty()) { %>
                <tr>
                    <td colspan="100" class="text-center">
                        There are no classes yet.
                        <a href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=<%=Paths.ADMIN_CLASSES%>">Click here</a>
                        to upload a new class.
                    </td>
                </tr>
            <% } %>

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
                        <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-<%=classId%>">
                            <%=alias%>
                        </a>
                        <% pageContext.setAttribute("classId", classId); %>
                        <% pageContext.setAttribute("classAlias", alias); %>
                        <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-${classId}"/>
                    </td>
                    <td><%=gamesWithClass%></td>
                    <td>
                        <div class="d-flex flex-row gap-1">
                            <%
                                if (active) {
                            %>
                                <form id="manageClass_<%=classId%>" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                    <input type="hidden" name="formType" value="setClassInactive">
                                    <button class="btn btn-sm btn-danger" id="<%="active_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                            title="Set class as inactive. This class won't be available for games afterwards."
                                            onclick="return confirm('Are you sure you want to set class \'<%=name%>\' to inactive?');">
                                        <i class="fa fa-power-off"></i>
                                    </button>
                                </form>
                            <%
                                } else {
                            %>
                                <form id="manageClass_<%=classId%>" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                    <input type="hidden" name="formType" value="setClassActive">
                                    <button class="btn btn-sm btn-success" id="<%="inactive_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                            title="Set class as active. This class will be available for games afterwards again."
                                            onclick="return confirm('Are you sure you want to set class \'<%=name%>\' to active?');">
                                        <i class="fa fa-power-off"></i>
                                    </button>
                                </form>
                            <%
                                }
                            %>
                            <form id="manageClass_<%=classId%>" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                <input type="hidden" name="formType" value="classRemoval">
                                <button class="btn btn-sm btn-danger" id="<%="delete_class_"+classId%>" type="submit" value="<%=classId%>" name="classId"
                                        <% if (deletable) { %>
                                            title="Delete class from the system. This class won't be available for games afterwards."
                                            onclick="return confirm('Are you sure you want to delete class \'<%=name%>\' forever? This cannot be undone.');"
                                        <% } else { %>
                                            disabled
                                            title="Class can't be deleted, since games were already played on it."
                                        <% } %>
                                    >
                                    <i class="fa fa-trash"></i>
                                </button>
                            </form>
                        </div>
                    </td>
                </tr>
            <%
                }
            %>

        </tbody>
    </table>

    <% if (!allClasses.isEmpty()) { %>
        <p>
            <a href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=<%=Paths.ADMIN_CLASSES%>">Click here</a>
            to upload a new class.
        </p>
    <% } %>

</div>

<%@ include file="/jsp/footer.jsp" %>
