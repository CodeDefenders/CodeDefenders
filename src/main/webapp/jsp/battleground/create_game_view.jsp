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
                <td>Defenders</td>
                <td class="crow fly" id="defendersTd">
                    <label style="font-weight: normal;" for="minDefenders">Min</label>
                    <input type="number" name="minDefenders" id="minDefenders" value="2"/>
                    <label style="font-weight: normal;" for="defenderLimit">Max</label>
                    <input type="number" value="4" id="defenderLimit" name="defenderLimit"/>
                </td>
            </tr>
            <tr>
                <td>Attackers</td>
                <td class="crow fly" id="attackersTd">
                    <label style="font-weight: normal;" for="minAttackers">Min</label>
                    <input type="number" value="2" name="minAttackers" id="minAttackers"/>
                    <label style="font-weight: normal;" for="attackerLimit">Max</label>
                    <input type="number" value="4" name="attackerLimit" id="attackerLimit"/>
                </td>
            </tr>
            <tr>
                <td>Start Time</td>
                <td id="startTimeTd">
                    <div class="crow">
                        <input type="hidden" id="startTime" name="startTime"/>
                        <input class="ws-5" name="start_dateTime" id="start_dateTime"
                               data-toggle="popover"
                               data-content="Invalid date or format (expected: YYYY/MM/DD)"
                               data-placement="left"
                               data-trigger="manual"/>
                        <div class="ws-7 nest">
                            <input class="ws-1" type="text" name="start_hours" id="start_hours"
                                   style="text-align: center;" data-toggle="popover"
                                   data-content="Hours must be a number between 0 and 23"
                                   data-placement="top"
                                   data-trigger="manual"/>
                            <span>:</span>
                            <input class="ws-1" type="text" name="start_minutes" id="start_minutes"
                                   style="text-align: center;" data-toggle="popover"
                                   data-content="Minutes must be a number between 0 and 59"
                                   data-placement="right"
                                   data-trigger="manual"/>
                        </div>
                    </div>
                    <script>
                        $(document).ready(function () {
                            var initialStartDate = new Date();
                            $("#startTime").val(initialStartDate.getTime());
                            $("#start_dateTime").datepicker({dateFormat: "yy/mm/dd"});
                            $("#start_dateTime").datepicker("setDate", initialStartDate);
                            $("#start_hours").val(initialStartDate.getHours());
                            var mins = initialStartDate.getMinutes();
                            var hours = initialStartDate.getHours();
                            if (mins < 10) {
                                // add leading zero to minute representation
                                mins = "0" + mins;
                            }
                            if (hours < 10) {
                                // add leading zero to minute representation
                                hours = "0" + hours;
                            }
                            $("#start_minutes").val(mins);
                            $("#start_hours").val(hours);
                        });

                        $("#start_dateTime").on("change", function () {
                            var date = ($("#start_dateTime")).val();

                            if (isValidDate(date)) {
                                updateStartTimestamp();
                            } else {
                                document.getElementById("createButton").disabled = true;
                                $("#start_dateTime").popover("show");
                                setTimeout(function () {
                                    $("#start_dateTime").popover("hide")
                                }, 6000);
                                document.getElementById("finishTimeWarning").style.display = "none";
                            }
                        });

                        $("#start_hours").on("change", function () {
                            var hours = $("#start_hours").val();

                            if (hours < 0 || hours > 23 || hours === "" || isNaN(hours)) {
                                document.getElementById("createButton").disabled = true;
                                $("#start_hours").popover("show");
                                setTimeout(function () {
                                    $("#start_hours").popover("hide");
                                }, 6000);
                            } else {
                                if (hours < 10) {
                                    // add leading zero to hour representation
                                    $("#start_hours").val("0" + hours);
                                }
                                updateStartTimestamp();
                            }
                        });

                        $("#start_minutes").on("change", function () {
                            var mins = $("#start_minutes").val();

                            if (mins === "" || isNaN(mins) || (mins < 0 || mins > 59)) {
                                document.getElementById("createButton").disabled = true;
                                $("#start_minutes").popover("show");
                                setTimeout(function () {
                                    $("#start_minutes").popover("hide");
                                }, 6000);
                            } else {
                                if (mins < 10) {
                                    // add leading zero to minute representation
                                    $("#start_minutes").val("0" + mins);
                                }
                                updateStartTimestamp();
                            }
                        });

                        // update the input of hidden startTime field with selected timestamp
                        var updateStartTimestamp = function () {
                            var startDate = $("#start_dateTime").val();
                            var hours = $("#start_hours").val();
                            var mins = $("#start_minutes").val();

                            // update hidden start timestamp only if whole input is valid
                            if (isValidDate(startDate)
                                && !(hours < 0 || hours > 23 || hours === "" || isNaN(hours))
                                && !(mins === "" || isNaN(mins) || (mins < 0 || mins > 59))) {
                                var newStartTime = new Date(startDate).getTime();
                                newStartTime += parseInt(hours * 60 * 60 * 1000);
                                newStartTime += parseInt(mins * 60 * 1000);
                                var finishTime = parseInt($("#finishTime").val());

                                var finishHours = $("#finish_hours").val();
                                var finishMins = $("#finish_minutes").val();

                                if (finishTime > newStartTime) {

                                    if (isValidDate($("#finish_dateTime").val())
                                        && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                                        && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                                        document.getElementById("createButton").disabled = false;
                                    }
                                } else {
                                    if (isValidDate($("#finish_dateTime").val())
                                        && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                                        && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                                        $("#finishBeforeStartWarning").popover("show");
                                        setTimeout(function () {
                                            $("#finishBeforeStartWarning").popover("hide");
                                        }, 6000);                                    }
                                    document.getElementById("createButton").disabled = true;
                                }
                                $("#startTime").val(newStartTime);
                            }
                        };

                        // date validation used in start and finish date
                        function isValidDate(dateString) {
                            // check pattern for YYYY/MM/DD
                            if (!/^\d{4}\/\d{1,2}\/\d{1,2}$/.test(dateString))
                                return false;

                            // parse the date parts to integers
                            var parts = dateString.split("/");
                            var year = parseInt(parts[0], 10);
                            var month = parseInt(parts[1], 10);
                            var day = parseInt(parts[2], 10);

                            // check the ranges of month and year
                            if (year < 1000 || year > 3000 || month === 0 || month > 12)
                                return false;

                            var monthLength = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

                            // check for leap years
                            if (year % 400 === 0 || (year % 100 !== 0 && year % 4 === 0)) {
                                monthLength[1] = 29;
                            }

                            // check the range of the day
                            return day > 0 && day <= monthLength[month - 1];
                        };


                    </script>
                </td>
            </tr>
            <tr>
                <td>Finish Time</td>
                <td id="finishTimeTd">
                    <div class="crow">
                        <input type="hidden" id="finishTime" name="finishTime"/>
                        <span class="alert alert-warning" id="finishTimeWarning" style="display: none"></span>
                        <input class="ws-5" name="finish_dateTime" id="finish_dateTime"
                               data-toggle="popover" data-content="Invalid date or format (expected: YYYY/MM/DD)"
                               data-placement="left"
                               data-trigger="manual"/>
                        <div class="ws-7 nest">
                            <input class="ws-1" type="text" name="finish_hours" id="finish_hours"
                                   style="text-align: center;" data-toggle="popover"
                                   data-content="Hours must be a number between 0 and 23"
                                   data-placement="bottom"
                                   data-trigger="manual"/>
                            <span>:</span>
                            <input class="ws-1" type="text" name="finish_minutes" id="finish_minutes"
                                   style="text-align: center;" data-toggle="popover"
                                   data-content="Minutes must be a number between 0 and 59"
                                   data-placement="right"
                                   data-trigger="manual"/>
                        </div>
                    </div>
                    <span id="finishBeforeStartWarning" data-toggle="popover"
                          data-content="Finish time must be later than selected start time!"
                          data-placement="bottom"
                          data-trigger="manual"></span>

                    <script>
                        $(document).ready(function () {
                            var initialFinishDate = new Date();
                            // add default 3 days to initial finish date
                            initialFinishDate.setDate(initialFinishDate.getDate() + 3);
                            $("#finishTime").val(initialFinishDate.getTime());
                            $("#finish_dateTime").datepicker({dateFormat: "yy/mm/dd"});
                            $("#finish_dateTime").datepicker("setDate", initialFinishDate);
                            $("#finish_hours").val(initialFinishDate.getHours());
                            var mins = initialFinishDate.getMinutes();
                            var hours = initialFinishDate.getHours();
                            if (mins < 10) {
                                mins = "0" + mins;
                            }
                            if (hours < 10) {
                                hours = "0" + hours;
                            }
                            $("#finish_minutes").val(mins);
                            $("#finish_hours").val(hours);
                        });

                        $("#finish_dateTime").on("change", function () {
                            var date = ($("#finish_dateTime")).val();

                            if (isValidDate(date)) {
                                updateFinishTimestamp();
                            } else {
                                document.getElementById("createButton").disabled = true;
                                $("#finish_dateTime").popover("show");
                                setTimeout(function () {
                                    $("#finish_dateTime").popover("hide")
                                }, 6000);
                                document.getElementById("finishTimeWarning").style.display = "none";
                            }
                        });

                        $("#finish_hours").on("change", function () {
                            var hours = $("#finish_hours").val();

                            if (hours < 0 || hours > 23 || hours === "" || isNaN(hours)) {
                                document.getElementById("createButton").disabled = true;
                                $("#finish_hours").popover("show");
                                setTimeout(function () {
                                    $("#finish_hours").popover("hide");
                                }, 6000);
                            } else {
                                if (hours < 10) {
                                    // add leading zero to hour representation
                                    $("#finish_hours").val("0" + hours);
                                }
                                updateFinishTimestamp();
                            }
                        });

                        $("#finish_minutes").on("change", function () {
                            var mins = $("#finish_minutes").val();

                            if (mins === "" || isNaN(mins) || (mins < 0 || mins > 59)) {
                                document.getElementById("createButton").disabled = true;
                                $("#finish_minutes").popover("show");
                                setTimeout(function () {
                                    $("#finish_minutes").popover("hide");
                                }, 6000);
                            } else {
                                if (mins < 10) {
                                    // add leading zero to minute representation
                                    $("#finish_minutes").val("0" + mins);
                                }
                                updateFinishTimestamp();

                            }
                        });

                        var updateFinishTimestamp = function () {
                            var finishDate = $("#finish_dateTime").val();
                            var finishHours = $("#finish_hours").val();
                            var finishMins = $("#finish_minutes").val();

                            if (isValidDate(finishDate)
                                && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                                && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                                var newFinishTime = new Date($("#finish_dateTime").val()).getTime();
                                newFinishTime += parseInt($("#finish_hours").val() * 60 * 60 * 1000);
                                newFinishTime += parseInt($("#finish_minutes").val() * 60 * 1000);
                                var startTime = parseInt($("#startTime").val());

                                var startHours = $("#start_hours").val();
                                var startMins = $("#start_minutes").val();

                                if (newFinishTime > startTime) {
                                    if (isValidDate($("#start_dateTime").val())
                                        && !(startHours < 0 || startHours > 23 || startHours === "" || isNaN(startHours))
                                        && !(startMins === "" || isNaN(startMins) || (startMins < 0 || startMins > 59))) {
                                        document.getElementById("createButton").disabled = false;
                                    }
                                } else {
                                    if (isValidDate($("#start_dateTime").val())
                                        && !(startHours < 0 || startHours > 23 || startHours === "" || isNaN(startHours))
                                        && !(startMins === "" || isNaN(startMins) || (startMins < 0 || startMins > 59))) {
                                        $("#finishBeforeStartWarning").popover("show");
                                        setTimeout(function () {
                                            $("#finishBeforeStartWarning").popover("hide");
                                        }, 6000);                                    }
                                    document.getElementById("createButton").disabled = true;
                                }
                                $("#finishTime").val(newFinishTime);
                            }
                        };
                    </script>
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
                <td/>
                <td>
                    <button id="createButton" class="btn btn-lg btn-primary btn-block" type="submit" value="Create">
                        Create
                    </button>
                </td>
                <td/>
            </tr>
        </table>
    </form>
</div>
<%
    }
%>
<%@ include file="/jsp/footer.jsp" %>
