<%--
  ~ Copyright (C) 2023 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="achievement" required="true" type="org.codedefenders.model.Achievement" %>

<div class="achievement-card achievement-level-${achievement.level}">
    <div class="pie animate" style="--percentage: ${achievement.progress.orElse(100)}">
        <img src="${url.forPath("/images/achievements/")}codedefenders_achievements_${achievement.id.asInt}_lvl_${achievement.level}.png"
             alt="${achievement.name} (Level ${achievement.level})">
    </div>
    <p>
        <strong>
            ${achievement.name}
            <c:if test="${achievement.level > 0}">
                (Level ${achievement.level})
            </c:if>
        </strong>
        <br>
        ${achievement.description}
        <br>
        ${achievement.progressText}
    </p>
</div>
