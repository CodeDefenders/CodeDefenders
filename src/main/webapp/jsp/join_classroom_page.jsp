<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="classroomService" type="org.codedefenders.service.ClassroomService"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<c:set var="classroom" value="${requestScope.classroom}"/>
<c:set var="numMembers" value="${requestScope.numMembers}"/>

<jsp:include page="/jsp/header.jsp"/>

<style>
    .center-container {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    .center-container > * {
        flex: 0 0 auto;
    }
    .center-container::before,
    .center-container::after {
        content: '';
        flex: 0 1 7.5rem;
    }

    #join-card {
        width: max-content;
        min-width: 30em;
    }
</style>

<div class="container">
    <div class="center-container">
        <div class="card" id="join-card">
            <div class="card-body">
                <p class="card-text mb-3">
                    You are invited to join the classroom
                </p>
                <h5 class="card-title mb-1">
                    <c:out value="${classroom.name}"/>
                </h5>
                <p class="card-text text-muted mb-3">
                    ${numMembers} members
                </p>
                <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
                    <c:if test="${classroom.password.isPresent()}">
                        <label for="password-input" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password-input"
                               name="password" placeholder="Password" required>
                        <div class="invalid-feedback">
                            Please enter the password.
                        </div>
                    </c:if>
                    <input type="hidden" name="action" value="join">
                    <input type="hidden" name="classroomId" value="${classroom.id}">
                    <button type="submit" class="btn btn-primary">Join</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/jsp/footer.jsp" %>
