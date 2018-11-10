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
var sLine = null; // Primary Target


function toggleDefend(){
	document.getElementById("submitTest").disabled = <% if(game.isDeclareCoveredLines()) {%> sLine == null  <% } else {%> true <% } %> && <% if(game.isDeclareKilledMutants() ) {%> sMutants.size == 0 <% } else {%> true <% } %>;
}

toggleDefend();

var theForm = document.getElementById('def');

var parent = document.getElementById('utest-div');
var container = document.createElement('div');
container.setAttribute("style", "width: 100%");
// Put the container before the form
parent.insertBefore(container, theForm);

var theTable = document.createElement("TABLE");
theTable.setAttribute("id", "intention-table");
container.appendChild(theTable)

<%-- Handling Line Coverage --%>
<% if(game.isDeclareCoveredLines()) {%>

function selectLine(lineNumber){
	if( sLine == lineNumber ){
		sLine = null;
		selected_lines.innerText="<no line selected>";
	} else {
		sLine = lineNumber;
		selected_lines.innerText=sLine;
	}
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

// update the UI by adding a row to the "intention-table"
// https://stackoverflow.com/questions/18333427/how-to-insert-row-in-html-table-body-in-javascript
// Insert a row in the table at the last row
var newRow = theTable.insertRow();
// Insert a cell in the row
var newCell  = newRow.insertCell();
// Write the HTML inside the cell
newCell.innerHTML='<strong>Selected Lines:</strong>';
newCell.setAttribute("style", "width: 25%;  height: 40px;")

//Insert another cell in the row
var newCell  = newRow.insertCell();
var selected_lines =  document.createElement('div');
selected_lines.setAttribute("id", "selectedLinesDiv");
selected_lines.innerText="<no line selected>";
newCell.appendChild(selected_lines);

<!-- Update Code Mirror to enable line selection -->
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
  // 
  selectLine(n+1);
});

function makeMarker() {
  var marker = document.createElement("div");
  marker.style.color = "#822";
  marker.innerHTML = "x";
  return marker;
}

// Resize original code mirror
var testCode = document.querySelector('#code').nextSibling;
var newHeight = testCode.style.height.replace("px","");
newHeight=newHeight-40;
testCode.style.height=newHeight+"px";
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

var newRow = theTable.insertRow();
//Insert a cell in the row
var newCell  = newRow.insertCell();
//Write the HTML inside the cell
newCell.innerHTML='<strong>Selected Mutants:</strong>';
newCell.setAttribute("style", "width: 25%;  height: 40px;")
//Insert another cell in the row
var newCell  = newRow.insertCell();
var selected_mutants =  document.createElement('div');
selected_mutants.setAttribute("id", "selectedMutantsDiv");
selected_mutants.innerText=""
newCell.appendChild(selected_mutants);

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
		var label = document.createTextNode("Target this mutant");
		cell.appendChild(label);
		checkbox.onclick = function fun(){
			selectMutant( i, this );
		}
	}
}

// Resize the original code mirror
var testCode = document.querySelector('#code').nextSibling;
var newHeight = testCode.style.height.replace("px","");
newHeight=newHeight-40;
testCode.style.height=newHeight+"px";


<% }%>

</script>