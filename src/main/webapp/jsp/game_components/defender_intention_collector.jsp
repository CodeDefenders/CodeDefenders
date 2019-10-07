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
var sLine = null; // Primary Target

var codeOriginalDisplay= document.querySelector('#code').parentNode.style.display;

// Include a DIV for the alternative text
var theForm = document.getElementById('def');
var parent = document.getElementById('utest-div');
var container = document.createElement('div');
container.setAttribute("style", "text-align: left;");
container.innerHTML='<h4 class="panel panel-default" style="margin-top: 50px; padding: 5px; max-width: 500px">' +
    'Click on the line number you are targeting in the Class Under Test with your test to enable the test editor</h4>'
// Hide the div
container.style.display = "none";
// Put the container before the form
parent.insertBefore(container, theForm);

function toggleDefend() {
	var input = document.getElementById('selected_lines');
	var submitTestButton = document.getElementById('submitTest');
	if( sLine == null ) {
		// When no lines are selected hide code mirror and display the alternative text instead
		// Disable the button
		submitTestButton.disabled = true;
		// Standard text
		submitTestButton.innerText = "Defend !";

		document.querySelector('#code').parentNode.style.display = "none";
		container.style.display = "block";
		// Update the value of the hidden field
		input.setAttribute("value", "");
	} else {
		// Enable the button
		submitTestButton.disabled = false;
		// Update the text inside the Defend button to show the selected line as well
		submitTestButton.innerText = "Defend Line " + sLine + " !";

		document.querySelector('#code').parentNode.style.display = codeOriginalDisplay;
		container.style.display = "none";
		// Update the value of the hidden field
		input.setAttribute("value", sLine);
	}
}

function selectLine(lineNumber){
	if( sLine == lineNumber ){
		sLine = null;
	} else {
		sLine = lineNumber;
	}
	// Update UI
	toggleDefend();
}

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "selected_lines");
input.setAttribute("name", "selected_lines");
input.setAttribute("value", "");
//append to form element that you want .
theForm.appendChild(input);

<!-- Update Left Code Mirror to enable line selection on gutter -->
var editor = document.querySelector('#sut').nextSibling.CodeMirror;
/* function isEmpty(obj) {
    for (var n in obj) if (obj.hasOwnProperty(n) && obj[n]) return false;
    return true;
  } */

editor.on("gutterClick", function(cm, n) {
  if( sLine != null ){
		if( sLine != n + 1) {
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

  selectLine(n+1);
});

function makeMarker() {
  var marker = document.createElement("div");
   marker.style.color = "#002cae";
  marker.innerHTML = "<span class=\"glyphicon glyphicon-triangle-right marker\" aria-hidden=\"true\"> </span>";
  return marker;
}

// Trigger the logic that updates the UI at last
toggleDefend();
addIntentionClass();

// If we there's lines to "pre-select" we do it now
<%if (session.getAttribute("selected_lines") != null) {%>
console.log("setting value for selected_lines "+<%=session.getAttribute("selected_lines")%> );
input.setAttribute("value", "<%=session.getAttribute("selected_lines")%>");
selectedLine = parseInt(<%=session.getAttribute("selected_lines")%>);
selectLine(selectedLine); // +1
editor.setGutterMarker(selectedLine-1, "CodeMirror-linenumbers", makeMarker());
<%}%>

function addIntentionClass() {
    $('#cut-div .CodeMirror-linenumber').each(function() {
         $(this).addClass("linenumber-intention");
    });
}

// add styling class to each line number every second, since a previously picked number looses its class after a new selection and can't be updated manually
window.setInterval(function(){
    addIntentionClass();
}, 1000);

// prepend note for line selection above CUT
var lineChooseNote = "<span class='panel panel-default' style='padding: 5px; margin-left: 10px; color: #00289c'>" +
    "<i class='glyphicon glyphicon-arrow-down' style='margin: 5px 3px 20px 0'></i>" +
    "Indicate here which line you are defending</span>";
$('#cut-div .CodeMirror.cm-s-default').prepend(lineChooseNote);

</script>
