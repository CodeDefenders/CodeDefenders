<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="gameId" required="true" %>
<%@ attribute name="selectionManagerUrl" required="true" %>
<%@ attribute name="duration" required="true" %>
<%@ attribute name="maxDuration" required="true" %>
<%@ attribute name="startTime" required="true" %>
<%@ attribute name="canSetDuration" required="true" %>

<form id="adminDurationChange" action="${selectionManagerUrl}" method="post" class="needs-validation">
    <input type="hidden" name="formType" value="durationChange">
    <input type="hidden" name="gameId" value="${gameId}">
    <input type="hidden" name="newDuration" id="duration-total">

    <%-- Button with remaining duration. --%>
    <button type="button" class="btn btn-sm btn-outline-danger" form="adminDurationChange"
            data-bs-toggle="modal" data-bs-target="#duration-change-modal">
        <i class="fa fa-hourglass-start"></i>
        <span class="time-left"
              data-type="remaining"
              data-duration="${duration}"
              data-start="${startTime}">
            Game Duration
        </span>
    </button>

    <t:modal title="Game Duration" id="duration-change-modal" closeButtonText="Cancel">
        <jsp:attribute name="content">

            <%-- Progress Bar --%>
            <div class="progress mb-2" style="height: 1em;">
                <div class="progress-bar time-left"
                     data-type="progress"
                     data-duration="${duration}"
                     data-start="${startTime}"
                     style="width: 0;">
                </div>
            </div>

            <%-- Duration Info --%>
            <div class="row">
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>Total Duration</small>
                    <span class="time-left"
                          data-type="total"
                          data-duration="${duration}">
                    </span>
                </div>
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>Remaining Duration</small>
                    <span class="time-left"
                          data-type="remaining"
                          data-duration="${duration}"
                          data-start="${startTime}">
                    </span>
                </div>
                <div class="col-4 d-flex flex-column align-items-center">
                    <small>End Time</small>
                    <span class="time-left"
                          data-type="end"
                          data-duration="${duration}"
                          data-start="${startTime}">
                    </span>
                </div>
            </div>

            <%-- Duration Controls --%>
            <c:if test="${canSetDuration}">
                <div class="mt-3">
                    <label class="form-label">Set the remaining duration:</label>
                    <div class="input-group input-group-sm has-validation">
                        <input type="number" name="days" class="form-control" id="days-input" min="0">
                        <span class="input-group-text">days</span>
                        <input type="number" name="hours" class="form-control" id="hours-input" min="0">
                        <span class="input-group-text">hours</span>
                        <input type="number" name="minutes" class="form-control" id="minutes-input" min="0">
                        <span class="input-group-text">minutes</span>
                        <div class="invalid-feedback">
                            Please input a valid duration.
                            Maximum duration:
                            <span class="time-left"
                                  data-type="total"
                                  data-duration="${maxDuration}">
                            </span>
                        </div>
                    </div>
                </div>
            </c:if>
        </jsp:attribute>

        <jsp:attribute name="footer">
            <c:if test="${canSetDuration}">
                <button type="submit" form="adminDurationChange" class="btn btn-primary" id="durationChange">
                    Change Game Duration
                </button>
            </c:if>
        </jsp:attribute>
    </t:modal>

    <%-- Validate input and update hidden field containing duration as minutes. --%>
    <c:if test="${canSetDuration}">
        <script>
            const MAX_DURATION_MINUTES = Number(${maxDuration});

            const daysInput = document.getElementById('days-input');
            const hoursInput = document.getElementById('hours-input');
            const minutesInput = document.getElementById('minutes-input');
            const totalInput = document.getElementById('duration-total');

            const setValidity = function (customValidity) {
                daysInput.setCustomValidity(customValidity);
                hoursInput.setCustomValidity(customValidity);
                minutesInput.setCustomValidity(customValidity);
            };

            const validateAndSetDuration = function () {
                if (daysInput.value.length === 0
                        && hoursInput.value.length === 0
                        && minutesInput.value.length === 0) {
                    setValidity('missing-value')
                    return;
                }

                const days = Number(daysInput.value);
                const hours = Number(hoursInput.value);
                const minutes = Number(minutesInput.value);
                const total = ((days * 24) + hours) * 60 + minutes;

                totalInput.value = total;

                if (total < 0 || total > MAX_DURATION_MINUTES) {
                    setValidity('invalid-value')
                    return
                }

                setValidity('');
            };

            daysInput.addEventListener('input', validateAndSetDuration);
            hoursInput.addEventListener('input', validateAndSetDuration);
            minutesInput.addEventListener('input', validateAndSetDuration);

            validateAndSetDuration();
        </script>
    </c:if>
</form>
