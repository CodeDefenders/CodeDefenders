<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="gameActive" required="true" %>
<%@ attribute name="intentionCollectionEnabled" required="true" %>

<c:choose>
    <c:when test="${!intentionCollectionEnabled}">
        <button type="submit" form="atk"
                id="submitMutant" class="btn btn-attacker btn-highlight"
                onclick="CodeDefenders.objects.mutantProgressBar.activate(); this.form.submit(); this.disabled=true;"
                <c:if test="${!gameActive}">disabled</c:if>>
            Attack
        </button>
    </c:when>
    <c:otherwise>
        <div id="attacker-intention-dropdown" class="dropdown">
            <button type="button" class="btn btn-attacker btn-highlight dropdown-toggle"
                    data-bs-toggle="dropdown" id="submitMutant" aria-expanded="false"
                    <c:if test="${!gameActive}">disabled</c:if>>
                Attack
            </button>
            <ul class="dropdown-menu" aria-labelledby="submitMutant" style="">
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="KILLABLE">
                        My mutant is killable
                    </a>
                </li>
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="EQUIVALENT">
                        My mutant is equivalent
                    </a>
                </li>
                <li>
                    <a class="dropdown-item cursor-pointer" data-intention="DONTKNOW">
                        I don't know if my mutant is killable
                    </a>
                </li>
            </ul>
        </div>
    </c:otherwise>
</c:choose>
