<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.codedefenders.validation.CodeValidator" %>
<%@ page import="static org.codedefenders.validation.CodeValidator.DEFAULT_NB_ASSERTIONS" %>
<% String pageTitle = "Create Battleground"; %>
<%@ include file="/jsp/header_main.jsp" %>
<div id="creategame" class="container">
    <form id="create" action="<%=request.getContextPath() %>/multiplayer/games" method="post"
          class="form-creategame-mp">
        <input type="hidden" name="formType" value="createGame">
        <table class="tableform">
            <tr>
                <td width="25%">Java Class</td>
                <td>
                    <select name="class" class="form-control selectpicker" data-size="large">
                        <% for (GameClass c : DatabaseAccess.getAllClasses()) { %>
                        <option value="<%=c.getId()%>"><%=c.getAlias()%>
                        </option>
                        <%}%>
                    </select>
                </td>
                <td width="17%">
                    <a href="games/upload" class="text-center new-account">Upload Class</a>
                </td>
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
                <td>Level</td>
                <td>
                    <input type="checkbox" id="level" name="level" class="form-control" data-size="large"
                           data-toggle="toggle" data-on="Easy" data-off="Hard" data-onstyle="info"
                           data-offstyle="warning">
                </td>
            </tr>
            <% /*
				Integer.parseInt(request.getParameter("defenderLimit")),
				Integer.parseInt(request.getParameter("attackerLimit")),
                Integer.parseInt(request.getParameter("minDefenders")),
                Integer.parseInt(request.getParameter("minAttackers")),
                Long.parseLong(request.getParameter("finishTime")),
                MultiplayerGame.State.CREATED.name());

                */
            %>
            <tr>
                <td>Defenders</td>
                <td class="crow fly">
                    <label style="font-weight: normal;" for="minDefenders">Min</label>
                    <input type="number" name="minDefenders" id="minDefenders" value="2"/>
                    <label style="font-weight: normal;" for="defenderLimit">Max</label>
                    <input type="number" value="4" id="defenderLimit" name="defenderLimit"/>
                </td>
            </tr>
            <tr>
                <td>Attackers</td>
                <td class="crow fly">
                    <label style="font-weight: normal;" for="minAttackers">Min</label>
                    <input type="number" value="2" name="minAttackers" id="minAttackers"/>
                    <label style="font-weight: normal;" for="attackerLimit">Max</label>
                    <input type="number" value="4" name="attackerLimit" id="attackerLimit"/>
                </td>
            </tr>
            <tr>
                    <%
					DateTime startDate = DateTime.now();
					DateTime finishDate = startDate.plusDays(3);
					DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
				%>
                <td>Start Time</td>
                <td>
                    <div class="crow">
                        <input type="hidden" id="startTime" name="startTime" value="<%=startDate.getMillis()%>"/>
                        <input class="ws-5" name="start_dateTime" id="start_dateTime"
                               value="<%=fmt.print(startDate)%>"/>
                        <div class="ws-7 nest">
                            <input class="ws-1" type="text" name="start_hours" id="start_hours"
                                   value="<%=String.format("%1$02d",startDate.getHourOfDay())%>"
                                   style="text-align: center;"/>
                            <span>:</span>
                            <input class="ws-1" type="text" name="start_minutes" id="start_minutes"
                                   value="<%=String.format("%1$02d",startDate.getMinuteOfHour())%>"
                                   style="text-align: center;"/>
                        </div>
                    </div>
                    <script>
                        $(document).ready(function () {
                            $("#startTime").val(new Date().getTime());
                            $("#start_dateTime").val($.datepicker.formatDate('yyyy/MM/dd', new Date(timeStamp)));
                            updateStartTimestamp();
                        });
                        var voidFunct = function () {
                        };
                        var updateStartTimestamp = function () {
                            var timestamp = new Date($("#start_dateTime").val()).valueOf();
                            timestamp += parseInt($("#start_hours").val()) * 60 * 60 * 1000;
                            timestamp += parseInt($("#start_minutes").val()) * 60 * 1000;
                            var now = new Date().getTime();
                            if (timestamp < now) {
                                //invalid timestamp, set it to now
                                timestamp = now;
                            }
                            $("#startTime").val(timestamp);
                        }
                        $("#start_hours").on("change", function () {
                            var hours = $("#start_hours").val();
                            if (hours < 0 || hours > 59) {
                                $("#start_hours").val(0);
                            }
                            updateStartTimestamp();
                        })
                        $("#start_minutes").on("change", function () {
                            var mins = $("#start_minutes").val();
                            if (mins < 0 || mins > 59) {
                                $("#start_minutes").val(0);
                            }
                            updateStartTimestamp();
                        })
                        var dataPicker = $("#start_dateTime").datepicker({
                            onSelect: function (selectedDate, dp) {
                                $(".ui-datepicker a").attr("href", "javascript:voidFunct();");
                                //var date = $.datepicker.formatDate("@", dp);
                                //alert(date);
                                updateStartTimestamp();
                            }
                        });
                    </script>
                </td>
            <tr>
                <td>Finish Time</td>
                <td>
                    <div class="crow">
                        <input type="hidden" id="finishTime" name="finishTime" value="<%=finishDate.getMillis()%>"/>
                        <input class="ws-5" name="finish_dateTime" id="finish_dateTime"
                               value="<%=fmt.print(finishDate)%>"/>
                        <div class="ws-7 nest">
                            <input class="ws-1" type="text" name="finish_hours" id="finish_hours"
                                   value="<%=String.format("%1$02d",finishDate.getHourOfDay())%>"
                                   style="text-align: center;"/>
                            <span>:</span>
                            <input class="ws-1" type="text" name="finish_minutes" id="finish_minutes"
                                   value="<%=String.format("%1$02d",finishDate.getMinuteOfHour())%>"
                                   style="text-align: center;"/>
                        </div>
                    </div>
                    <script>
                        $(document).ready(function () {
                            $("#finishTime").val(new Date().getTime());
                            $("#finish_dateTime").val($.datepicker.formatDate('yyyy/MM/dd', new Date(timeStamp)));
                            updateFinishTimestamp();
                        });
                        var updateFinishTimestamp = function () {
                            var timestamp = new Date($("#finish_dateTime").val()).valueOf();
                            timestamp += parseInt($("#finish_hours").val()) * 60 * 60 * 1000;
                            timestamp += parseInt($("#finish_minutes").val()) * 60 * 1000;
                            var now = new Date().getTime();
                            if (timestamp < now) {
                                //invalid timestamp, set it to now+5 days
                                timestamp = now + (3 * 24 * 60 * 60 * 1000);
                            }
                            $("#finishTime").val(timestamp);
                        }
                        $("#finish_hours").on("change", function () {
                            var hours = $("#finish_hours").val();
                            if (hours < 0 || hours > 59) {
                                $("#finish_hours").val(0);
                            }
                            updateFinishTimestamp();
                        })
                        $("#finish_minutes").on("change", function () {
                            var mins = $("#finish_minutes").val();
                            if (mins < 0 || mins > 59) {
                                $("#finish_minutes").val(0);
                            }
                            updateFinishTimestamp();
                        })
                        var dataPicker = $("#finish_dateTime").datepicker({
                            onSelect: function (selectedDate, dp) {
                                $(".ui-datepicker a").attr("href", "javascript:voidFunct();");
                                //var date = $.datepicker.formatDate("@", dp);
                                //alert(date);
                                updateFinishTimestamp();
                            },
                        });
                    </script>
                </td>
            </tr>
            <tr>
                <td title="Maximum number of assertions per test. Increase this for difficult to test classes.">
                    Max. Assertions per Test
                </td>
                <td>
                    <input class="form-control" type="number" value="<%=DEFAULT_NB_ASSERTIONS%>" name="maxAssertionsPerTest"
                           id="maxAssertionsPerTest" min = 1 required/>
                </td>
            </tr>
            <tr>
                <td title="Click the question sign for more information on the levels">
                    Mutant validator
                </td>
                <td>
                    <select id = "mutantValidatorLevel" name="mutantValidatorLevel" class="form-control selectpicker" data-size="medium">
                        <%for (CodeValidator.CodeValidatorLevel cvl : CodeValidator.CodeValidatorLevel.values()){%>
                        <option value=<%=cvl.name()%> <%=cvl.equals(CodeValidator.CodeValidatorLevel.MODERATE) ? "selected" : ""%>>
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
                <td title="Attackers can mark uncovered lines as equivalent">
                    Mark uncovered lines as equivalent
                </td>
                <td>
                    <input type="checkbox" id="markUncovered" name="markUncovered"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="">
                </td>
            </tr>
            <tr>
                <td title="Players can chat with their team and with all players in the game">
                    Chat
                </td>
                <td>
                    <input type="checkbox" id="chatEnabled" name="chatEnabled"
                           class="form-control" data-size="large" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="" checked>
                </td>
            </tr>
            <input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">
            <tr>
                <td/>
                <td>
                    <button class="btn btn-lg btn-primary btn-block" type="submit" value="Create">Create</button>
                </td>
                <td/>
            </tr>
        </table>
    </form>
</div>
<%@ include file="/jsp/footer.jsp" %>
