<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%-- This jsp assumes that the availability of a MultiplayerGame variable named game --%>
<script>
var sMutants = new Set();
var sLines = new Set();

function toggleDefend(){
   document.getElementById("submitTest").disabled = <% if(game.isDeclareCoveredLines()) {%> sLines.size == 0 <% } else {%> true <% } %> && <% if(game.isDeclareKilledMutants() ) {%> sMutants.size == 0 <% } else {%> true <% } %>;
}

toggleDefend();

var theForm = document.getElementById('def');

var parent = document.getElementById('utest-div');
var conn = document.createElement('div');

<%-- Handling Line Coverage --%>
<% if(game.isDeclareCoveredLines()) {%>
function selectLine(lineNumber){
	if( sLines.has(lineNumber)){
		sLines.delete(lineNumber);
	} else {
		sLines.add(lineNumber);
	}
	selected_lines.innerText=Array.from(sLines).join(",");
	document.getElementById("selected_lines").value = selected_lines.innerText;
	toggleDefend();
}

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "selected_lines");
input.setAttribute("name", "selected_lines");
input.setAttribute("value", "");
//append to form element that you want .
theForm.appendChild(input);


conn.appendChild(document.createTextNode("Selected Lines:"));
var selected_lines =  document.createElement('div');
selected_lines.setAttribute("id", "selectedLinesDiv");
selected_lines.innerText=""
conn.appendChild(selected_lines);

<!-- Update Code Mirror to enable line selection -->
var editor = document.querySelector('#sut').nextSibling.CodeMirror;

editor.on("gutterClick", function(cm, n) {
  var info = cm.lineInfo(n);
  cm.setGutterMarker(n, "CodeMirror-linenumbers", info.gutterMarkers ? null : makeMarker());
  selectLine(n+1);
});

function makeMarker() {
  var marker = document.createElement("div");
  marker.style.color = "#822";
  marker.innerHTML = "x";
  return marker;
}

<% }%>

<%-- Handling Killing Mutants --%>
<% if(game.isDeclareKilledMutants() ) {%>

function selectMutant(mutantCheckboxRow, mutantCheckbox){
	var table = document.getElementById("alive-mutants");
	var mutantID = table.rows[mutantCheckboxRow].cells[0].innerText.split(' ')[1];
	// Update the label with that
	if( mutantCheckbox.checked ){
		sMutants.add(mutantID);
	} else {
		sMutants.delete(mutantID);
	}
	selected_mutants.innerText=Array.from(sMutants).join(",");
	document.getElementById("selected_mutants").value = selected_mutants.innerText;
	toggleDefend();
}

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "selected_mutants");
input.setAttribute("name", "selected_mutants");
input.setAttribute("value", "");
//append to form element that you want .
theForm.appendChild(input);

conn.appendChild(document.createTextNode("Selected Mutants:"));
var selected_mutants =  document.createElement('div');
selected_mutants.setAttribute("id", "selectedMutantsDiv");
selected_mutants.innerText=""
conn.appendChild(selected_mutants);

parent.insertBefore(conn, theForm);

<!-- Add a checkbox for each alive mutant. Note that the table might not be there -->

var table = document.getElementById("alive-mutants");

if( table != null ){
	<!-- Note the use of "let" instead of "var" -->
	for (let i = 0, row; row = table.rows[i]; i++) {
		// Take Cell #3
		var cell = row.cells[2];
		var checkbox = document.createElement("input");
		checkbox.setAttribute("type", "checkbox");
		checkbox.setAttribute("id", "checkbox_"+i);
		// Do I need to set id and value ?
		cell.appendChild(checkbox);
		var label = document.createElement("label");
		label.setAttribute("for", "checkbox_"+i);
		label.innerHTML="Select this mutant as target"
		cell.appendChild(label);
		checkbox.onclick = function fun(){
			selectMutant( i, this );
		}
	}
}

<% }%>

</script>