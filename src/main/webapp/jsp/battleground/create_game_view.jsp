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
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS" %>
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<% String pageTitle = "Create Battleground"; %>
<%@ include file="/jsp/header_main.jsp" %>
<%
    List<GameClass> gameClasses = GameClassDAO.getAllPlayableClasses();
    boolean isClassUploadEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.CLASS_UPLOAD).getBoolValue();

    if (gameClasses.isEmpty()) {
        if (isClassUploadEnabled) {
%>
<div id="creategame" class="container">
    <p>
        Before you can start games, please <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>" class="text-center new-account">upload a class under
        test</a>.
    </p>
</div>
<%
        } else {
%>
<div id="creategame" class="container">
    <p>
        Games can only be started once at least one class under test has been uploaded.
    </p>
</div>
<%
        }
    } else {
%>
<div id="creategame" class="container">
    <form id="create" action="<%=request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>" method="post"
          class="form-creategame-mp">
        <input type="hidden" name="formType" value="createGame">
        <table class="tableform">
            <tr>
                <td width="25%">Java Class</td>
                <td id="classTd">
                    <select selectpicker id="class" name="class" class="form-control" data-size="large">
                        <% for (GameClass c : gameClasses) { %>
                        <option value="<%=c.getId()%>"><%=c.getAlias()%>
                        </option>
                        <%}%>
                    </select>
                </td>
                <%if (isClassUploadEnabled) {%>
                    <td width="17%">
                        <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>" class="text-center new-account">Upload Class</a>
                    </td>
                <%}%>
            </tr>
            <!--
            <tr>
                <td>Line Coverage Goal</td><td><input class="ws-2" type="number" value="0.8" min="0.1" max="1.0" step="0.1" name="line_cov" style="text-align: center"/></td>
            </tr>
            <tr>
                <td>Mutation Goal</td><td><input class="ws-2" type="number" value="0.5" min="0.1" max="1.0" step="0.1" name="mutant_cov" style="text-align: center"></td>
            </tr>
            -->
            <tr>
                <td>Include predefined mutants (if available)</td>
                <td>
                    <input type="checkbox" id="withMutants" name="withMutants"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </td>
            </tr>
            <tr>
                <td>Include predefined tests (if available)</td>
                <td>
                    <input type="checkbox" id="withTests" name="withTests"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </td>
            </tr>
            <tr>
                <td>Level</td>
                <td id="levelTd">
                    <input type="checkbox" id="level" name="level" class="form-control" data-size="large"
                           data-toggle="toggle" data-on="Easy" data-off="Hard" data-onstyle="info"
                           data-offstyle="warning">
                </td>
            </tr>
            <tr>
                <td title="Maximum number of assertions per test. Increase this for difficult to test classes.">
                    Max. Assertions per Test
                </td>
                <td id="maxAssertionsPerTestTd">
                    <input class="form-control" type="number" value="<%=DEFAULT_NB_ASSERTIONS%>"
                           name="maxAssertionsPerTest"
                           id="maxAssertionsPerTest" min=1 required/>
                </td>
            </tr>
            <tr>
                <td title="Click the question sign for more information on the levels">
                    Mutant validator
                </td>
                <td id="mutantValidatorLevelTd">
                    <select id="mutantValidatorLevel" name="mutantValidatorLevel" class="form-control selectpicker"
                            data-size="medium">
                        <%for (CodeValidatorLevel cvl : CodeValidatorLevel.values()) {%>
                        <option value=<%=cvl.name()%> <%=cvl.equals(CodeValidatorLevel.MODERATE) ? "selected" : ""%>>
                            <%=cvl.name().toLowerCase()%>
                        </option>
                        <%}%>
                    </select>
                    <div id="validatorExplanation" class="collapse panel panel-default" style="font-size: 12px;">
                        <%@ include file="/jsp/validator_explanation.jsp" %>
                    </div>
                </td>
                <td>
                    <a data-toggle="collapse" href="#validatorExplanation" style="color:black">
                        <span class="glyphicon glyphicon-question-sign"></span>
                    </a>
                </td>
            </tr>
            <tr>
                <td title="Chose your role for this game">
                    Role selection
                </td>
                <td id="roleSelectionTd">
                    <select id="roleSelection" name="roleSelection" class="form-control selectpicker"
                            data-size="medium">
                        <%for (Role role : Role.values()) {
                            if (role != Role.NONE) { %>
                        <option value=<%=role.name()%> <%=role.equals(Role.OBSERVER) ? "selected" : ""%>>
                            <%=role.name().toLowerCase()%>
                        </option>
                        <%  }
                        }%>
                    </select>
                </td>
            </tr>
            <tr>
                <td title="Players can chat with their team and with all players in the game">
                    Chat
                </td>
                <td id="chatEnabledTd">
                    <input type="checkbox" id="chatEnabled" name="chatEnabled"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="" checked>
                </td>
            </tr>

            <tr>
                <td>Enable Capturing Players Intention</td>
                <td>
                    <input type="checkbox" id="capturePlayersIntention" name="capturePlayersIntention"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </td>
            </tr>
            <input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">
            <tr>
                <td>
                    <button id="createButton" class="btn btn-lg btn-primary btn-block" type="submit" value="Create">
                        Create
                    </button>
                </td>
            </tr>
        </table>
    </form>
</div>
<%
    }
%>
<%@ include file="/jsp/footer.jsp" %>
