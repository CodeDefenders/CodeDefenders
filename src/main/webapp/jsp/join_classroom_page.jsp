<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="classroomService" type="org.codedefenders.service.ClassroomService"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<c:set var="classroom" value="${requestScope.classroom}"/>
<c:set var="numMembers" value="${requestScope.numMembers}"/>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <div class="w-100 d-flex flex-row justify-content-center" style="margin-top: min(15rem, 20vh);">
        <div class="card" style="width: 40rem;">
            <div class="card-body p-4">
                <p class="card-text mb-3 fs-5">
                    You are invited to join the classroom
                </p>
                <h5 class="card-title mb-1 fs-4">
                    <c:out value="${classroom.name}"/>
                </h5>
                <p class="card-text text-muted mb-3 fs-5">
                    ${numMembers} members
                </p>
                <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
                    <input type="hidden" name="action" value="join">
                    <input type="hidden" name="classroomId" value="${classroom.id}">

                    <c:choose>
                        <c:when test="${classroom.password.isPresent()}">
                            <div class="input-group has-validation">
                                <input type="password" class="form-control" id="password-input"
                                       name="password" placeholder="Password" required>
                                <button type="submit" class="btn btn-primary btn-highlight">Join</button>
                                <div class="invalid-feedback">
                                    Please enter the password.
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" class="btn btn-primary btn-highlight">Join</button>
                        </c:otherwise>
                    </c:choose>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/jsp/footer.jsp" %>
