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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<c:set var="classroom" value="${requestScope.classroom}"/>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Create Games"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <div class="d-flex align-items-center mb-4 gap-3">
        <h2 class="m-0"><c:out value="${classroom.name}"/></h2>
        <a href="${url.forPath(Paths.CLASSROOM)}?classroomUid=${classroom.UUID}"
           class="btn btn-sm rounded-pill btn-outline-secondary flex-shrink-0">
            Back to Classroom
            <i class="fa fa-external-link ms-1"></i>
        </a>
    </div>

    <t:create_games/>
</div>

<%@ include file="/jsp/footer.jsp" %>
