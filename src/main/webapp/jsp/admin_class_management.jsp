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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.GameClassInfo" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%
    @SuppressWarnings("unchecked")
    List<GameClassInfo> allClasses  = (List<GameClassInfo>) request.getAttribute("classInfos");
    pageContext.setAttribute("allClasses", allClasses);
%>

<p:main_page title="Class Management">
    <div class="container">
        <t:admin_navigation activePage="adminClasses"/>

        <table id="tableClasses" class="table table-v-align-middle table-striped">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Alias</th>
                    <th>Assertion Library</th>
                    <th>#Games</th>
                    <th>Manage Class</th>
                </tr>
            </thead>
            <tbody>

                <c:if test="${empty allClasses}">
                    <tr>
                        <td colspan="100" class="text-center">
                            There are no classes yet.
                            <a href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=${Paths.ADMIN_CLASSES}">Click here</a>
                            to upload a new class.
                        </td>
                    </tr>
                </c:if>

                <c:forEach var="classInfo" items="${allClasses}">
                    <c:set var="gameClass" value="${classInfo.gameClass}"/>
                    <c:set var="classId" value="${gameClass.id}"/>

                    <tr id="class_row_${classId}" ${gameClass.active ? '' : 'class="text-muted"'}>
                        <td>${classId}</td>
                        <td>${gameClass.name}</td>
                        <td>
                            <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-${classId}">
                                ${gameClass.alias}
                            </a>
                            <t:class_modal classId="${classId}" classAlias="${gameClass.alias}" htmlId="class-modal-${classId}"/>
                        </td>
                        <td>${gameClass.assertionLibrary.description}</td>
                        <td>${classInfo.gamesWithClass}</td>
                        <td>
                            <div class="d-flex flex-row gap-1">
                                <c:choose>
                                    <c:when test="${gameClass.active}">
                                        <form id="manageClass_${classId}" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                            <input type="hidden" name="formType" value="setClassInactive">
                                            <button class="btn btn-sm btn-danger" id="active_class_${classId}" type="submit" value="${classId}" name="classId"
                                                    title="Set class as inactive. This class won't be available for games afterwards."
                                                    onclick="return confirm('Are you sure you want to set class \'${gameClass.name}\' to inactive?');">
                                                <i class="fa fa-power-off"></i>
                                            </button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form id="manageClass_${classId}" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                            <input type="hidden" name="formType" value="setClassActive">
                                            <button class="btn btn-sm btn-success" id="inactive_class_${classId}" type="submit" value="${classId}" name="classId"
                                                    title="Set class as active. This class will be available for games afterwards again."
                                                    onclick="return confirm('Are you sure you want to set class \'${gameClass.name}\' to active?');">
                                                <i class="fa fa-power-off"></i>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                                <form id="manageClass_${classId}" action="${url.forPath(Paths.ADMIN_CLASSES)}" method="post">
                                    <input type="hidden" name="formType" value="classRemoval">
                                    <button class="btn btn-sm btn-danger" id="delete_class_${classId}" type="submit" value="${classId}" name="classId"
                                            <c:choose>
                                                <c:when test="${classInfo.deletable}">
                                                    title="Delete class from the system. This class won't be available for games afterwards."
                                                    onclick="return confirm('Are you sure you want to delete class \'${gameClass.name}\' forever? This cannot be undone.');"
                                                </c:when>
                                                <c:otherwise>
                                                    disabled
                                                    title="Class can't be deleted, since games were already played on it."
                                                </c:otherwise>
                                            </c:choose>
                                        >
                                        <i class="fa fa-trash"></i>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>

            </tbody>
        </table>

        <c:if test="${!empty allClasses}">
            <p>
                <a href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=${Paths.ADMIN_CLASSES}">Click here</a>
                to upload a new class.
            </p>
        </c:if>

    </div>
</p:main_page>
