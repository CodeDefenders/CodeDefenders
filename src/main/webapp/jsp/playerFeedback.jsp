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
<%@page import="java.util.List"%>
<%@page import="org.codedefenders.util.Paths"%>
<%@page import="org.codedefenders.servlets.util.ServletUtils"%>
<%@page import="org.codedefenders.game.multiplayer.MultiplayerGame"%>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.FeedbackDAO" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.model.Feedback" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.database.GameDAO" %>
<%@ page import="org.codedefenders.model.Player" %>
<%
{
    int gameId = (Integer) request.getAttribute("gameId");
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    int userId = ServletUtils.userId(request); // required for playerFeedback, too
    Role role = game.getRole(userId); // required for header_game, too
%>
<div id="playerFeedback" class="modal fade" role="dialog" style="z-index: 10000; position: absolute;">

    <style>
        fieldset, label {
            margin: 0;
            padding: 0;
        }

        /****** Style Star Rating Widget *****/
        .rating {
            border: none;
            float: left;
        }

        .rating > input {
            display: none;
        }

        .rating > label:before {
            font-size: 1.25em;
            display: inline-block;
            content: "\e006";
            font-family: 'Glyphicons Halflings';
            font-style: normal;
            font-weight: normal;
        }

        .rating > label {
            font-size: 20px;
            color: #ddd;
            float: right;
        }

        /***** CSS Magic to Highlight Stars on Hover *****/
        .rating > input:checked ~ label, /* show gold star when clicked */
        .rating:not(:checked) > label:hover, /* hover current star */
        .rating:not(:checked) > label:hover ~ label {
            color: #FFD700;
        }

        /* hover previous stars in list */
        .rating > input:checked + label:hover, /* hover current star when changing rating */
        .rating > input:checked ~ label:hover,
        .rating > label:hover ~ input:checked ~ label, /* lighten current selection */
        .rating > input:checked ~ label:hover ~ label {
            color: #FFED85;
        }
    </style>

    <div class="modal-dialog modal-lg">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3 class="modal-title">Feedback for Game <%=gameId%>
                </h3>
            </div>

            <%  int currentUserId = ((Integer) session.getAttribute("uid"));

                boolean canSeePlayerFeedback = (currentUserId == game.getCreatorId()) || AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SHOW_PLAYER_FEEDBACK).getBoolValue();
                boolean canGiveFeedback = role.equals(Role.DEFENDER) || role.equals(Role.ATTACKER);
                if (canGiveFeedback) {%>
            <ul class="nav nav-tabs">
                <li class="active" id="provideFeedbackLink">
                    <a onClick="switchModal()">
                        Give Feedback
                    </a>
                </li>
                <li id="viewFeedbackLink">
                    <a onClick="switchModal()">
                        View Feedback
                    </a>
                </li>
            </ul>

            <div class="modal-body" id="provide_feedback_modal">
                <h4><b>How much do you agree with the following statements:</b></h4>
                <br>

                <form id="sendFeedback" action="<%=request.getContextPath() + Paths.API_FEEDBACK%>" method="post">
                    <input type="hidden" name="formType" value="sendFeedback">
                    <input type="hidden" name="gameId" value="<%=gameId%>">
                    <table class="table-hover table-striped table-responsive ">
                        <tbody>

                        <%
                            List<Integer> oldValues = FeedbackDAO.getFeedbackValues(gameId, userId);
                            for (Feedback.Type f : Feedback.types) {
                                int oldValue = oldValues.isEmpty() ? -1 : oldValues.get(f.ordinal());
                                if ((role.equals(Role.DEFENDER) &&
                                        (f.equals(Feedback.Type.CUT_MUTATION_DIFFICULTY) ||
                                                f.equals(Feedback.Type.DEFENDER_FAIRNESS) ||
                                                f.equals(Feedback.Type.DEFENDER_COMPETENCE)))
                                        ||
                                        (role.equals(Role.ATTACKER) &&
                                                (f.equals(Feedback.Type.CUT_TEST_DIFFICULTY) ||
                                                        f.equals(Feedback.Type.ATTACKER_FAIRNESS) ||
                                                        f.equals(Feedback.Type.ATTACKER_COMPETENCE))))
                                    continue;
                        %>

                        <tr>
                            <td><%=f.description()%>
                            </td>
                            <td>
                                <fieldset class="rating">
                                    <input type="radio" id="star5_<%=f.name()%>" name="rating<%=f.name()%>" value=5
                                        <%=oldValue == 5 ? "checked" : ""%>>
                                    <label class="full" for="star5_<%=f.name()%>" title="very much"></label>
                                    <input type="radio" id="star4_<%=f.name()%>" name="rating<%=f.name()%>" value=4
                                        <%=oldValue == 4 ? "checked" : ""%>>
                                    <label class="full" for="star4_<%=f.name()%>" title="a lot"></label>
                                    <input type="radio" id="star3_<%=f.name()%>" name="rating<%=f.name()%>" value=3
                                        <%=oldValue == 3 ? "checked" : ""%>>
                                    <label class="full" for="star3_<%=f.name()%>" title="somewhat"></label>
                                    <input type="radio" id="star2_<%=f.name()%>" name="rating<%=f.name()%>" value=2
                                        <%=oldValue == 2 ? "checked" : ""%>>
                                    <label class="full" for="star2_<%=f.name()%>" title="a bit"></label>
                                    <input type="radio" id="star1_<%=f.name()%>" name="rating<%=f.name()%>" value=1
                                        <%=oldValue == 1 ? "checked" : ""%>>
                                    <label class="full" for="star1_<%=f.name()%>" title="not at all"></label>
                                </fieldset>
                            </td>
                        </tr>
                        <%}%>
                        </tbody>

                    </table>

                    <br>
                    <p>In providing feedback you help us improve gameplay mechanics, <br>
                        hone match making and select classes that are engaging and fun.</p>
                    <p>You can change your feedback even after the game finishes.</p>
                    <p>Thank you for your time.</p>
                    <br>

                    <button class="btn btn-primary" type="submit"> Save Feedback</button>
                </form>
            </div>
            <%}%>

            <div class="modal-body" id="view_feedback_modal"
                 style="<%=canGiveFeedback ? "display: none;" : ""%>">

                <% if (FeedbackDAO.getNBFeedbacksForGame(gameId) > 0) {%>
                <div class="table-responsive">
                <table class="table-striped table-hover table-bordered table-responsive table-sm">
                    <thead>
                    <tr>
                        <th><%=canSeePlayerFeedback ? "Player" : ""%></th>
                        <% for (Feedback.Type f : Feedback.Type.values()) {%>
                        <th style="width: 12.5%" title="<%=f.description()%>"><%=f.displayName()%>
                        </th>
                        <%}%>
                    </tr>
                    </thead>
                    <tbody>

                    <%
                        if (canSeePlayerFeedback) {
                            for (Player player : GameDAO.getAllPlayersForGame(gameId)) {
                                User user = player.getUser();
                                int playerUserId = user.getId();

                                if (FeedbackDAO.hasNotRated(gameId, playerUserId))
                                    continue;

                                String rowColor = player.getRole() == Role.ATTACKER ? "#9a002914" : "#0029a01a";
                    %>
                    <tr style="background-color:<%=rowColor%>">
                        <td><%=user.getUsername()%>
                        </td>
                        <%
                            List<Integer> ratingValues = FeedbackDAO.getFeedbackValues(gameId, playerUserId);
                            for (Feedback.Type f : Feedback.Type.values()) {
                                int ratingValue = ratingValues == null ? -1 : ratingValues.get(f.ordinal());
                                if (ratingValue < 1) {
                        %>
                        <td></td>

                        <%} else {%>

                        <td>
                            <fieldset class="rating">
                                <%for (int i = Feedback.MAX_RATING; i > 0; i--) {%>
                                <label class="full" title="<%=i%>"
                                       style="font-size:9px; color:<%=i <= ratingValue  ? "#FFD700" : "#bdbdbd"%>"></label>
                                <%}%>
                            </fieldset>
                        </td>

                        <%
                                }
                            }
                        %>
                    </tr>

                    <%
                            }
                        }
                    %>
                    <tr></tr>
                    <tr>
                        <td>Average</td>
                        <%
                            List<Double> avgRatings = FeedbackDAO.getAverageGameRatings(gameId);
                            for (Feedback.Type f : Feedback.types) {
                                double ratingValue = avgRatings == null ? -1 : avgRatings.get(f.ordinal());
                                if (ratingValue < 1) {
                        %>
                        <td></td>

                        <%} else {%>

                        <td>
                            <p style="text-align: left;"><%=String.format("%.1f", ratingValue)%></p>
                            <fieldset class="rating">
                                <%for (int i = Feedback.MAX_RATING; i > 0; i--) {%>
                                <label class="full" title="<%=i%>"
                                       style="font-size:9px; color:<%=i <= Math.round(ratingValue)  ? "#FFD700" : "#bdbdbd"%>"></label>
                                <%}%>
                            </fieldset>
                        </td>

                        <%
                                }
                            }
                        %>
                    </tr>
                    </tbody>

                </table>
                </div>
                <% } else {
                %>
                <h4>No player has provided feedback for this game yet.</h4>
                <%
                    }%>
            </div>
        </div>
    </div>
</div>

<script>
    function switchModal() {
        var provideFeedbackModalStyle = document.getElementById('provide_feedback_modal').style.display;
        document.getElementById('view_feedback_modal').style.display = provideFeedbackModalStyle;
        document.getElementById('provide_feedback_modal').style.display = provideFeedbackModalStyle == 'none' ? 'block' : 'none';
        document.getElementById('view_feedback_modal').style.width = "90%";
        document.getElementById('view_feedback_modal').style.margin = "0 auto";
        document.getElementById('provideFeedbackLink').classList.toggle('active');
        document.getElementById('viewFeedbackLink').classList.toggle('active');
    }
</script>
<%
}
%>
