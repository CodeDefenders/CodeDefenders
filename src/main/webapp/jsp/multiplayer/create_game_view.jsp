<% String pageTitle = "Create Battleground"; %>
<%@ include file="/jsp/header.jsp"%>
<div id="creategame" class="container">
	<form id="create" action="multiplayer/games" method="post" class="form-creategame">
		<h2>Create Game</h2>
		<input type="hidden" name="formType" value="createGame">
		<table class="tableform">
			<tr>
				<td>Java Class</td>
				<td>
					<select name="class" class="form-control selectpicker" data-size="large" >
						<% for (GameClass c : DatabaseAccess.getAllClasses()) { %>
						<option value="<%=c.getId()%>"><%=c.getName()%></option>
						<%}%>
					</select>
				</td>
				<td>
					<a href="games/upload" class="text-center new-account">Upload Class</a>
				</td>
			</tr>
			<tr>
				<td>Line Coverage Goal</td><td><input type="text" value="0.8" name="line_cov" /></td>
			</tr>
			<tr>
				<td>Mutation Goal</td><td><input type="text" value="0.5" name="mutant_cov"></td>
			</tr>
			<tr>
				<td>Level</td> <td><input type="checkbox" id="level" name="level" class="form-control" data-size="large" data-toggle="toggle" data-on="Easy" data-off="Hard" data-onstyle="info" data-offstyle="warning">
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
				<td>Defenders</td> <td class="crow fly"><input type="number" name="minDefenders" value="2" />-<input type="number" value="4" name="defenderLimit" /></td>
			</tr>
			<tr>
				<td>Attackers</td> <td class="crow fly"><input type="number" value="2" name="minAttackers" />-<input type="number" value="4" name="attackerLimit" /></td>
			</tr>
			<tr>
				<td>Finish Time</td> <td>
				<input type="hidden" id="finishTime" name="finishTime" /><input name="dateTime" id="dateTime" value="Select Date"/>
				<div class="crow fly">
					<input class="ws-5" type="text" name="hours" id="hours" value="00" />
					<span class="wd-2">:</span>
					<input class="wd-5" type="text" name="minutes" id="minutes" value="00" />
				</div>
					<script>
					voidFunct = function(){};
					updateTimestamp = function(){
						var timestamp = new Date($("#dateTime").val()).valueOf();
						timestamp += parseInt($("#hours").val()) * 60 * 60 * 1000;
						timestamp += parseInt($("#minutes").val()) * 60 * 1000;
						var now = new Date().getTime();
						if (timestamp < now){
							//invalid timestamp, set it to now+5 days
							timestamp = now + (5*24*60*60*1000);
						}
						$("#finishTime").val(timestamp);
					}
					$("#hours").on( "change", function(){
						var hours = $("#hours").val();
						if (hours < 0 || hours > 59){
							$("#hours").val(0);
						}
						updateTimestamp();
					})
					$("#minutes").on( "change", function(){
						var mins = $("#minutes").val();
						if (mins < 0 || mins > 59){
							$("#minutes").val(0);
						}
						updateTimestamp();
					})
					var dataPicker = $("#dateTime").datepicker({
						onSelect: function (selectedDate, dp) {
							$(".ui-datepicker a").attr("href", "javascript:voidFunct();");
							//var date = $.datepicker.formatDate("@", dp);
							//alert(date);
							updateTimestamp();
						}
					});
				</script>
				</td>
			</tr>
		</table>
		<button class="btn btn-lg btn-primary btn-block" type="submit" value="Create">Create</button>
	</form>
</div>
<%@ include file="/jsp/footer.jsp" %>