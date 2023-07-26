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

<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS" %>
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.GameLevel" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Create Melee Game"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    List<GameClass> gameClasses = GameClassDAO.getAllPlayableClasses();
    boolean isClassUploadEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.CLASS_UPLOAD).getBoolValue();
%>

<div class="container-fluid">

    <h2 class="text-center mb-4">${pageInfo.pageTitle}</h2>

    <%
        if (gameClasses.isEmpty()) {
            if (isClassUploadEnabled) {
    %>
    <p class="text-center">
        Before you can start games, please
        <a href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=<%=Paths.MELEE_CREATE%>" class="text-center">upload
            a class under test</a>.
    </p>
    <%
            } else {
    %>
    <p class="text-center">
        Games can only be started once at least one class under test has been uploaded.
    </p>
    <%
            }
        } else {
    %>
    <div class="d-flex flex-wrap justify-content-center gap-5">
        <div id="create-game-settings" class="form-width">
            <form id="create" action="${url.forPath(Paths.MELEE_SELECTION)}" method="post"
                  class="needs-validation" autocomplete="off">
                <input type="hidden" name="formType" value="createGame">

                <input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">

                <div class="row mb-3">
                    <label class="col-4 col-form-label" id="class-label" for="class-select">Class Under Test</label>
                    <div class="col-8 mb-3">
                        <div class="input-group has-validation">
                            <select class="form-select" id="class-select" name="class" required>
                                <% for (GameClass clazz : gameClasses) { %>
                                <option value="<%=clazz.getId()%>"><%=clazz.getAlias()%>
                                </option>
                                <% } %>
                            </select>
                            <% if (isClassUploadEnabled) { %>
                            <span class="input-group-text position-relative cursor-pointer"
                                  title="Upload a class.">
                            <a class="stretched-link text-decoration-none"
                               href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=<%=Paths.MELEE_CREATE%>">
                                <i class="fa fa-upload"></i>
                            </a>
                        </span>
                            <% } %>
                            <div class="invalid-feedback">Please select a class.</div>
                        </div>
                    </div>
                    <div class="offset-4 col-8">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="predefined-mutants-switch"
                                   name="withMutants">
                            <label class="form-check-label" for="predefined-mutants-switch">Include predefined mutants
                                (if available)</label>
                        </div>
                    </div>
                    <div class="offset-4 col-8">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="predefined-tests-switch"
                                   name="withTests">
                            <label class="form-check-label" for="predefined-tests-switch">Include predefined tests (if
                                available)</label>
                        </div>
                    </div>
                </div>

                <div class="row mb-3">
                    <label class="col-4 col-form-label pt-0" id="level-label" for="level-group">
                        <a class="text-decoration-none text-reset cursor-pointer text-nowrap"
                           data-bs-toggle="modal" data-bs-target="#levelExplanation">
                            Game Level
                            <i class="fa fa-question-circle ms-1"></i>
                        </a>
                    </label>
                    <div class="col-8" id="level-group">
                        <div class="form-check">
                            <input class="form-check-input" type="radio" id="level-radio-hard" name="level"
                                   value="<%=GameLevel.HARD%>" required
                                   checked>
                            <label class="form-check-label" for="level-radio-hard">Hard</label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" id="level-radio-easy" name="level"
                                   value="<%=GameLevel.EASY%>" required>
                            <label class="form-check-label" for="level-radio-easy">Easy</label>
                            <div class="invalid-feedback">Please select a level.</div>
                        </div>
                    </div>
                </div>

                <div class="row mb-3">
                    <label class="col-4 col-form-label pt-0" id="mutant-validator-label" for="mutant-validator-group">
                        <a class="text-decoration-none text-reset cursor-pointer text-nowrap"
                           data-bs-toggle="modal" data-bs-target="#validatorExplanation">
                            Mutant Validator Level
                            <i class="fa fa-question-circle ms-1"></i>
                        </a>
                    </label>
                    <div class="col-8" id="mutant-validator-group">
                        <% for (CodeValidatorLevel level : CodeValidatorLevel.values()) { %>
                        <div class="form-check">
                            <input class="form-check-input" type="radio"
                                   id="mutant-validator-radio-<%=level.name().toLowerCase()%>"
                                   name="mutantValidatorLevel"
                                   value="<%=level.name()%>" required
                                <%=level == CodeValidatorLevel.MODERATE ? "checked" : ""%>>
                            <label class="form-check-label"
                                   for="mutant-validator-radio-<%=level.name().toLowerCase()%>">
                                <%=level.getDisplayName()%>
                            </label>
                            <% if (level == CodeValidatorLevel.values()[CodeValidatorLevel.values().length - 1]) { %>
                            <div class="invalid-feedback">Please select a mutant validator level.</div>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                </div>

                <div class="row mb-3"
                     title="Maximum number of assertions per test. Increase this for difficult to test classes.">
                    <label class="col-4 col-form-label" id="max-assertions-label" for="max-assertions-input">
                        Max. Assertions Per Test
                    </label>
                    <div class="col-8">
                        <input type="number" class="form-control" id="max-assertions-input" name="maxAssertionsPerTest"
                               value="<%=DEFAULT_NB_ASSERTIONS%>" min="1" required>
                        <div class="invalid-feedback">Please provide a valid number. Must be greater than zero.</div>
                    </div>
                </div>

                <div class="row mb-3">
                    <label class="col-4 col-form-label" id="equiv-threshold-label" for="equiv-threshold-input">
                        <a class="text-decoration-none text-reset cursor-pointer"
                           data-bs-toggle="modal" data-bs-target="#automaticEquivalenceTriggerExplanation">
                            Auto Equiv. Threshold
                            <span class="fa fa-question-circle ms-1"></span>
                        </a>
                    </label>
                    <div class="col-8">
                        <input class="form-control" type="number" id="equiv-threshold-input"
                               name="automaticEquivalenceTrigger"
                               value="0" min="0" required>
                        <div class="invalid-feedback">Please provide a valid number. Must be positive or zero.</div>
                    </div>
                </div>

                <div class="row mb-1"
                     title="Forces players to specify the intentions of their mutants/tests before they can submit them.">
                    <label class="col-4 col-form-label" id="capture-intentions-label" for="capture-intentions-switch">Capture Intentions</label>
                    <div class="col-8 d-flex align-items-center">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="capture-intentions-switch"
                                   name="capturePlayersIntention">
                            <label class="form-check-label" for="capture-intentions-switch">Enable Capturing Players' Intentions</label>
                        </div>
                    </div>
                </div>

                <div class="row mb-3"
                     title="Allows players to chat with each other.">
                    <label class="col-4 col-form-label" id="chat-label" for="chat-switch">Game Chat</label>
                    <div class="col-8 d-flex align-items-center">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="chat-switch" name="chatEnabled" checked>
                            <label class="form-check-label" for="chat-switch">Enable Chat</label>
                        </div>
                    </div>
                </div>

                <div class="row mb-3"
                     title="Select the role the creator (you) will have in the game.">
                    <label class="col-4 col-form-label" id="role-label" for="role-select">Creator Role</label>
                    <div class="col-8">
                        <select class="form-select" id="role-select" name="roleSelection" required>
                            <% for (Role role : Role.meleeRoles()) { %>
                            <option value=<%=role.name()%> <%=role.equals(Role.OBSERVER) ? "selected" : ""%>>
                                <%=role.getFormattedString()%>
                            </option>
                            <% } %>
                        </select>
                        <div class="invalid-feedback">Please select a role.</div>
                    </div>
                </div>

                <div class="row mb-3" title="The duration for how long the games will be open.">
                    <input type="hidden" name="gameDurationMinutes" id="gameDurationMinutes">
                    <%
                        request.setAttribute("defaultDuration", AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue());
                        request.setAttribute("maximumDuration", AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue());
                    %>

                    <label class="col-4 col-form-label">Set the game's duration:</label>
                    <div class="col-8 input-group input-group-sm has-validation"
                         style="width: 66.6666666667%;"><!-- col-8 is overridden by input-group -->
                        <input type="number" name="days" class="form-control" id="days-input" min="0">
                        <label for="days-input" class="input-group-text">days</label>
                        <input type="number" name="hours" class="form-control" id="hours-input" min="0">
                        <label for="hours-input" class="input-group-text">hours</label>
                        <input type="number" name="minutes" class="form-control" id="minutes-input" min="0">
                        <label for="minutes-input" class="input-group-text">minutes</label>
                        <div class="invalid-feedback">
                            Please input a valid duration.
                            Maximum duration: <span id="displayMaxDuration">&hellip;</span>
                        </div>
                    </div>

                    <script type="module">
                        import {GameTimeValidator, GameTime} from '${url.forPath("/js/codedefenders_game.mjs")}';

                        const gameTimeValidator = new GameTimeValidator(
                            Number(${maximumDuration}),
                            Number(${defaultDuration}),
                            document.getElementById('minutes-input'),
                            document.getElementById('hours-input'),
                            document.getElementById('days-input'),
                            document.getElementById('gameDurationMinutes')
                        );

                        document.getElementById('displayMaxDuration').innerText =
                                GameTime.formatTime(${maximumDuration});
                    </script>
                </div>

                <c:choose>
                    <c:when test="${empty param.origin}">
                        <button type="submit" class="btn btn-primary" id="createButton">Create Game</button>
                    </c:when>
                    <c:otherwise>
                        <button type="submit" class="btn btn-primary" id="createButton">Create Game</button>
                        <a href="${url.forPath(param.origin)}" id="cancel"
                           class="btn btn-outline-primary">Cancel</a>
                    </c:otherwise>
                </c:choose>

            </form>

            <t:modal id="levelExplanation" title="Level Explanation">
                <jsp:attribute name="content">
                    <t:level_explanation_melee/>
                </jsp:attribute>
            </t:modal>

            <t:modal id="validatorExplanation" title="Validator Explanation">
                <jsp:attribute name="content">
                    <t:validator_explanation_mutant/>
                    <div class="mt-3"></div> <%-- spacing --%>
                    <t:validator_explanation_test/>
                </jsp:attribute>
            </t:modal>

            <t:modal id="automaticEquivalenceTriggerExplanation" title="Auto Equivalence Duel Threshold Explanation">
                <jsp:attribute name="content">
                    <t:automatic_duels_explanation/>
                </jsp:attribute>
            </t:modal>

        </div>

        <div class="form-width w-100">
            <t:cut_preview/>
        </div>
    </div>
    <%
        }
    %>

</div>

<%@ include file="/jsp/footer.jsp" %>
