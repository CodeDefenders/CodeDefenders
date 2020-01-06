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
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.model.Feedback" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.model.Feedback.Type" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>

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
                <h3 class="modal-title">Feedback for Game ${playerFeedback.gameId}
                </h3>
            </div>

            <% if (playerFeedback.canGiveFeedback()) {%>
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
                    <input type="hidden" name="gameId" value="${playerFeedback.gameId}">
                    <table class="table-hover table-striped table-responsive ">
                        <tbody>

                        <%
                            List<Integer> ratings = playerFeedback.getOwnRatings();
                            for (Type f : Feedback.types) {
                                if (!playerFeedback.isRatingForRole(f)) {
                                    continue;
                                }
                                int oldValue = ratings.isEmpty() ? -1 : ratings.get(f.ordinal());
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
                        <% } %>
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
                 style="${playerFeedback.canGiveFeedback() ? "display: none;" : ""}">

                <% if (playerFeedback.getAllRatings().size() > 0) { %>
                <div class="table-responsive">
                <table class="table-striped table-hover table-bordered table-responsive table-sm">
                    <thead>
                    <tr>
                        <th>${playerFeedback.canSeeFeedback() ? "Player" : ""}</th>
                        <% for (Type f : Type.values()) {%>
                            <th style="width: 12.5%" title="<%=f.description()%>"><%=f.displayName()%>
                            </th>
                        <%}%>
                    </tr>
                    </thead>
                    <tbody>

                    <%
                        if (playerFeedback.canSeeFeedback()) {
                            for (Map.Entry<Player, List<Integer>> entry : playerFeedback.getAllRatings().entrySet()) {
                                Player player = entry.getKey();
                                List<Integer> ratings = entry.getValue();

                                if (ratings.isEmpty()) {
                                    continue;
                                }

                                String rowColor = player.getRole() == Role.ATTACKER ? "#9a002914" : "#0029a01a";
                    %>
                    <tr style="background-color:<%=rowColor%>">
                        <td><%=player.getUser().getUsername()%></td>
                        <%
                            for (Type f : Type.values()) {
                                int ratingValue = ratings.get(f.ordinal());
                                if (ratingValue < 1) {
                        %>
                        <td></td>

                        <%} else {%>

                        <td>
                            <fieldset class="rating">
                                <% for (int i = Feedback.MAX_RATING; i > 0; i--) { %>
                                <label class="full" title="<%=i%>"
                                       style="font-size:9px; color:<%=i <= ratingValue  ? "#FFD700" : "#bdbdbd"%>"></label>
                                <% } %>
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
                            List<Double> averageRatings = playerFeedback.getAverageRatings();
                            for (Type f : Feedback.types) {
                                double ratingValue = averageRatings.isEmpty() ? -1 : averageRatings.get(f.ordinal());
                                if (ratingValue < 1) {
                        %>
                        <td></td>

                        <% } else { %>

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
