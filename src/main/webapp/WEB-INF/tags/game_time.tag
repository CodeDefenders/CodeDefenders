<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="gameId" required="true" type="java.lang.Integer" %>
<%@ attribute name="selectionManagerUrl" required="true" type="java.lang.String" %>
<%@ attribute name="duration" required="true" type="java.lang.Integer" %>
<%@ attribute name="maxDuration" required="true" type="java.lang.Integer" %>
<%@ attribute name="startTime" required="true" type="java.lang.Long" %>
<%@ attribute name="canSetDuration" required="true" type="java.lang.Boolean" %>

<form id="durationModalForm" action="${selectionManagerUrl}" method="post" class="needs-validation">
    <input type="hidden" name="formType" value="durationChange">
    <input type="hidden" name="gameId" value="${gameId}">
    <input type="hidden" name="newDuration" id="duration-total">

    <%-- Button with remaining duration. --%>

    <div data-bs-toggle="tooltip"
    <c:choose>
        <c:when test="${canSetDuration}">
            title="Change the game duration."
        </c:when>
        <c:otherwise>
            title="View the game duration."
        </c:otherwise>
    </c:choose>
    >
        <button type="button"
                class="btn btn-sm btn-outline-${canSetDuration ? 'danger' : 'secondary'}"
                form="durationModalForm"
                data-bs-toggle="modal"
                data-bs-target="#duration-change-modal">
            <i class="fa fa-hourglass-start"></i>
            <span class="time-left"
                  data-type="remaining"
                  data-duration="${duration}"
                  data-start="${startTime}">
            Game Duration
        </span>
        </button>
    </div>

    <t:modal title="Game Duration" id="duration-change-modal"
             closeButtonText="${canSetDuration ? 'Cancel' : 'Close'}">
        <jsp:attribute name="content">

            <%-- Progress Bar --%>
            <div class="progress mb-3" style="height: 1em;">
                <div class="progress-bar progress-bar-animated progress-bar-striped time-left"
                     data-type="progress"
                     data-duration="${duration}"
                     data-start="${startTime}"
                     style="width: 0; animation-duration: 2s; transition-duration: 10s;"
                     role="progressbar">
                </div>
            </div>

            <%-- Duration Info --%>
            <div class="row text-center">
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>Total Duration</small>
                    <span class="time-left"
                          data-type="total"
                          data-duration="${duration}">
                        &hellip;
                    </span>
                </div>
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>Remaining Duration</small>
                    <span class="time-left"
                          data-type="remaining"
                          data-duration="${duration}"
                          data-start="${startTime}">
                        &hellip;
                    </span>
                </div>
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>This game ${startTime == -1 ? 'would' : 'will'} end at</small>
                    <span class="time-left"
                          data-type="end"
                          data-duration="${duration}"
                          data-start="${startTime}">
                        &hellip;
                    </span>
                </div>
            </div>

            <%-- Duration Controls --%>
            <c:if test="${canSetDuration}">
                <div class="mt-3">
                    <label class="form-label">Set the games new remaining duration:</label>
                    <div class="input-group input-group-sm has-validation">
                        <input type="number" name="days" class="form-control" id="days-input" min="0">
                        <label for="days-input" class="input-group-text">days</label>
                        <input type="number" name="hours" class="form-control" id="hours-input" min="0">
                        <label for="hours-input" class="input-group-text">hours</label>
                        <input type="number" name="minutes" class="form-control" id="minutes-input" min="0">
                        <label for="minutes-input" class="input-group-text">minutes</label>
                        <div class="invalid-feedback">
                            Please input a valid duration.
                            Maximum remaining duration:
                            <span class="time-left"
                                  data-type="remaining"
                                  data-duration="${maxDuration}"
                                  data-start="${startTime}">
                                &hellip;
                            </span>
                        </div>
                    </div>
                </div>
            </c:if>
        </jsp:attribute>

        <jsp:attribute name="footer">
            <c:if test="${canSetDuration}">
                <button type="submit" form="durationModalForm" class="btn btn-primary" id="durationChange">
                    Change Game Duration
                </button>
            </c:if>
        </jsp:attribute>
    </t:modal>

    <%-- Validate input and update hidden field containing duration as minutes. --%>
    <c:if test="${canSetDuration}">
        <script>
            const MAX_DURATION_MINUTES = Number(${maxDuration});
            const units = ['days', 'hours', 'minutes'];
            const inputs = {};
            units.forEach(unit => inputs[unit] = document.getElementById(unit + '-input'));
            const totalInput = document.getElementById('duration-total');

            const setValidity = function (customValidity) {
                units.forEach(u => inputs[u].setCustomValidity(customValidity));
            };

            const validateAndSetDuration = function () {
                const hasValue = units.some(u => inputs[u].value.length > 0);
                if (!hasValue) {
                    setValidity('missing-value');
                    return;
                }

                const days = Number(inputs.days.value);
                const hours = Number(inputs.hours.value);
                const minutes = Number(inputs.minutes.value);
                const elapsedMinutes = <%-- No dynamic calculation needed if the game isn't started yet. --%>
                    <c:choose>
                        <c:when test="${startTime == -1}">0</c:when>
                        <c:otherwise>Math.round((Date.now() / 1e3 - ${startTime}) / 60)</c:otherwise>
                    </c:choose>;
                const total = ((days * 24) + hours) * 60 + minutes + elapsedMinutes;

                totalInput.value = total;

                if (total < 0 || total > MAX_DURATION_MINUTES) {
                    setValidity('invalid-value');
                    return;
                }

                setValidity('');
            };

            units.forEach(u => inputs[u].addEventListener('input', validateAndSetDuration));
            validateAndSetDuration();
        </script>
    </c:if>
</form>

<script type="module">
    import {GameTimeManager} from './js/codedefenders_game.mjs';
    const gameTimeManager = new GameTimeManager(".time-left", 10);
</script>
