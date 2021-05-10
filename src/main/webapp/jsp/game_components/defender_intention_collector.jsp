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
<script>
(function () {
    var sLine = null; // Primary Target

    // Include a DIV for the alternative text
    var theForm = document.getElementById('def');

    // prepend note for line selection above CUT
    var lineChooseNote = '<div id="line-choose-note" class="mb-1 ps-3" style="color: #00289c"><i class="fa fa-arrow-down"></i> Indicate which line you are defending to enable test editor</div>';

    $(lineChooseNote).insertAfter('#cut-div .game-component-header');

    var input = document.createElement("input");
    input.setAttribute("type", "hidden");
    input.setAttribute("id", "selected_lines");
    input.setAttribute("name", "selected_lines");
    input.setAttribute("value", "");
    //append to form element that you want .
    theForm.appendChild(input);

    // Update Left Code Mirror to enable line selection on gutter
    var editor = document.querySelector('#sut').nextSibling.CodeMirror;

    toggleIntentionClass();
    // Trigger the logic that updates the UI at last
    toggleDefend();

    // If we there's lines to "pre-select" we do it now
    <% if (session.getAttribute("selected_lines") != null) { %>
        console.log("setting value for selected_lines "+<%=session.getAttribute("selected_lines")%> );
        input.setAttribute("value", "<%=session.getAttribute("selected_lines")%>");
        selectedLine = parseInt(<%=session.getAttribute("selected_lines")%>);
        selectLine(selectedLine); // +1
        toggleLineChooseNote();
        toggleIntentionClass();
        editor.setGutterMarker(selectedLine-1, "CodeMirror-linenumbers", makeMarker());
    <% } %>

    editor.on("gutterClick", function (cm, n) {
        if (isLineSelected()) {
            if (sLine != n + 1) {
                // DeSelect the previously selected line if any
                cm.setGutterMarker(sLine - 1, "CodeMirror-linenumbers", null);
            }
        }
        // Toogle the new one
        var info = cm.lineInfo(n);
        var markers = info.gutterMarkers || (info.gutterMarkers = {});
        var value = markers["CodeMirror-linenumbers"];
        if (value != null) {
            cm.setGutterMarker(n, "CodeMirror-linenumbers", null);
        } else {
            cm.setGutterMarker(n, "CodeMirror-linenumbers", makeMarker());
        }

        selectLine(n + 1);
        toggleLineChooseNote();

        // wait for the linenumbers to render before adding/removing a styling class
        setTimeout(function () {
            toggleIntentionClass();
        });
    });

    function makeMarker() {
        var marker = document.createElement("div");
        marker.style.color = "#002cae";
        marker.innerHTML = '<i class="fa fa-arrow-right marker ps-1"></i>';
        return marker;
    }

    function selectLine(lineNumber){
        if (sLine == lineNumber) {
            sLine = null;
        } else {
            sLine = lineNumber;
        }
        // Update UI
        toggleDefend();
    }

    function toggleDefend() {
        var input = document.getElementById('selected_lines');
        var submitTestButton = document.getElementById('submitTest');
        if (sLine == null) {
            // When no lines are selected hide code mirror and display the alternative text instead
            // Disable the button
            submitTestButton.disabled = true;
            // Standard text
            submitTestButton.innerText = "Defend";

            $('#def pre').addClass('readonly-pre');

            // Update the value of the hidden field
            input.setAttribute("value", "");
        } else {
            // Enable the button
            submitTestButton.disabled = false;
            // Update the text inside the Defend button to show the selected line as well
            submitTestButton.innerText = "Defend Line " + sLine;

            $('#def pre').removeClass('readonly-pre');

            // Update the value of the hidden field
            input.setAttribute("value", sLine);
        }
    }

    function toggleIntentionClass() {
        $('#cut-div .CodeMirror-gutter-elt').each(function() {
            if (!isLineSelected()) {
                $(this).addClass('linenumber-intention');
            } else {
                $(this).removeClass('linenumber-intention');
            }
        });
    }

    function toggleLineChooseNote() {
        if (!isLineSelected()) {
            $('#line-choose-note').show();
        } else {
            $('#line-choose-note').hide();
        }
    }

    function isLineSelected() {
        return sLine != null;
    }
})();
</script>
