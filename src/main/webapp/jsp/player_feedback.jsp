<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ page import="org.codedefenders.util.Paths"%>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.model.Feedback" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.model.Feedback.Type" %>

<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>

<link href="${url.forPath("/css/specific/player_feedback.css")}" rel="stylesheet">

<div id="playerFeedback" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-responsive">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Feedback for Game ${playerFeedback.gameId}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">

                <% if (playerFeedback.canGiveFeedback()) { %>

                    <ul class="nav nav-tabs mb-3">
                        <li class="nav-item" id="give-feedback-tab">
                            <button type="button" class="nav-link active" data-bs-toggle="tab"
                                    data-bs-target="#give-feedback" role="tab"
                                    aria-controls="give-feedback">
                                Give Feedback
                            </button>
                        </li>
                        <li class="nav-item" id="view-feedback-tab">
                            <button type="button" class="nav-link" data-bs-toggle="tab"
                                    data-bs-target="#view-feedback" role="tab"
                                    aria-controls="view-feedback">
                                View Feedback
                            </button>
                        </li>
                    </ul>

                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="give-feedback" role="tabpanel" aria-labelledby="give-feedback-tab">
                            <span class="h4">How much do you agree with the following statements?</span>
                            <form id="give-feedback-form" action="${url.forPath(Paths.API_FEEDBACK)}" method="post" autocomplete="off">
                                <input type="hidden" name="formType" value="sendFeedback">
                                <input type="hidden" name="gameId" value="${playerFeedback.gameId}">

                                <div class="rating-grid p-3">
                                    <%
                                        Map<Feedback.Type, Integer> ratings = playerFeedback.getOwnRatings();
                                        for (Type type : playerFeedback.getAvailableFeedbackTypes()) {
                                            int oldValue = ratings.getOrDefault(type, 0);
                                    %>
                                        <div><%=type.description()%></div>
                                        <fieldset class="rating rating-interactive">
                                            <input type="radio" id="star5_<%=type.name()%>" name="rating<%=type.name()%>" value=5
                                                <%=oldValue == 5 ? "checked" : ""%>>
                                            <label class="full" for="star5_<%=type.name()%>" title="very much"></label>
                                            <input type="radio" id="star4_<%=type.name()%>" name="rating<%=type.name()%>" value=4
                                                <%=oldValue == 4 ? "checked" : ""%>>
                                            <label class="full" for="star4_<%=type.name()%>" title="a lot"></label>
                                            <input type="radio" id="star3_<%=type.name()%>" name="rating<%=type.name()%>" value=3
                                                <%=oldValue == 3 ? "checked" : ""%>>
                                            <label class="full" for="star3_<%=type.name()%>" title="somewhat"></label>
                                            <input type="radio" id="star2_<%=type.name()%>" name="rating<%=type.name()%>" value=2
                                                <%=oldValue == 2 ? "checked" : ""%>>
                                            <label class="full" for="star2_<%=type.name()%>" title="a bit"></label>
                                            <input type="radio" id="star1_<%=type.name()%>" name="rating<%=type.name()%>" value=1
                                                <%=oldValue == 1 ? "checked" : ""%>>
                                            <label class="full" for="star1_<%=type.name()%>" title="not at all"></label>
                                        </fieldset>
                                    <%
                                        }
                                    %>
                                </div>

                                <p style="max-width: 800px;">
                                    In providing feedback you help us improve gameplay mechanics,
                                    hone match making and select classes that are engaging and fun.
                                    You can change your feedback even after the game finishes.
                                    Thank you for your time.
                                </p>

                                <div class="d-flex justify-content-end gap-2">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                                    <button type="submit" id="give-feedback-button" class="btn btn-primary"
                                            <%=playerFeedback.hasOwnRatings() ? "" : "disabled"%>>
                                        Save Feedback
                                    </button>
                                </div>

                            </form>
                        </div>

                        <div class="tab-pane fade" id="view-feedback" role="tabpanel" aria-labelledby="view-feedback-tab">

                <% } %>

                            <div class="table-responsive">
                                <table class="table table-bordered">
                                    <thead>
                                        <tr>
                                            <th>${playerFeedback.canSeeFeedback() ? "Player" : ""}</th>
                                            <% for (Type f : Type.TYPES) { %>
                                                <th class="text-center" title="<%=f.description()%>"><%=f.displayName()%></th>
                                            <% } %>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <%
                                            if (playerFeedback.canSeeFeedback()) {
                                                for (Map.Entry<Player, Map<Feedback.Type, Integer>> entry : playerFeedback.getAllRatings().entrySet()) {
                                                    Player player = entry.getKey();
                                                    Map<Feedback.Type, Integer> playerRatings = entry.getValue();
                                                    String bgClass = player.getRole() == Role.ATTACKER ? "bg-attacker" : "bg-defender";
                                        %>
                                            <tr class="<%=bgClass%> text-white">
                                                <td><%=player.getUser().getUsername()%></td>
                                                <%
                                                    for (Type type : Type.TYPES) {
                                                        Integer ratingValue = playerRatings.get(type);
                                                %>
                                                    <td class="text-center">
                                                        <% if (ratingValue != null) { %>
                                                            <div class="d-flex justify-content-center">
                                                                <fieldset class="rating rating-static">
                                                                    <% for (int i = Feedback.MAX_RATING; i > 0; i--) { %>
                                                                        <label class="full" title="<%=i%>"
                                                                               style="color: <%=i <= ratingValue ? "#FFD700" : "#bdbdbd"%>">
                                                                        </label>
                                                                    <% } %>
                                                                </fieldset>
                                                            </div>
                                                        <% } %>
                                                    </td>
                                                <%
                                                    }
                                                %>
                                            </tr>
                                        <%
                                                }
                                            }
                                        %>
                                        <tr>
                                            <td>Average</td>
                                            <%
                                                Map<Feedback.Type, Double> averageRatings = playerFeedback.getAverageRatings();
                                                for (Type type : Type.TYPES) {
                                                    Double rating = averageRatings.get(type);
                                            %>
                                                <td>
                                                    <div class="d-flex justify-content-center gap-2">
                                                        <% if (rating != null) { %>
                                                            <span><%=String.format("%.1f", rating)%></span>
                                                            <fieldset class="rating">
                                                                <% for (int i = Feedback.MAX_RATING; i > 0; i--) { %>
                                                                    <label class="full" title="<%=i%>"
                                                                           style="color: <%=i <= Math.round(rating)  ? "#FFD700" : "#bdbdbd"%>"></label>
                                                                <% } %>
                                                            </fieldset>
                                                        <% } %>
                                                    </div>
                                                </td>
                                            <%
                                                }
                                            %>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <%
                                /* If there is no feedback yet. */
                                if (playerFeedback.getAllRatings().values().stream()
                                        .allMatch(Map::isEmpty)) {
                            %>
                                <p>No player has provided feedback for this game yet.</p>
                            <%
                                }
                            %>

                            <div class="d-flex justify-content-end gap-2">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                            </div>

                <% if (playerFeedback.canGiveFeedback()) { %>
                        </div>
                    </div>
                <% } %>

            </div>
       </div>
    </div>
</div>

<script type="module">
    import $ from '${url.forPath("/js/jquery.mjs")}';


    $(document).ready(() => {
        $('#give-feedback-form').on('change', '.rating input', function () {
            $('#give-feedback-button').removeAttr('disabled');
        });
    });
</script>
