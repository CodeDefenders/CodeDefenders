<script>

var sMutants = new Set();
var sLines = new Set();

function toggleDefend(){
document.getElementById("submitTest").disabled = sMutants.size == 0 && sLines.size == 0 ;
}
toggleDefend();

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


var theForm = document.getElementById('def');

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "selected_lines");
input.setAttribute("name", "selected_lines");
input.setAttribute("value", "");

//append to form element that you want .
theForm.appendChild(input);

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "selected_mutants");
input.setAttribute("name", "selected_mutants");
input.setAttribute("value", "");

//append to form element that you want .
theForm.appendChild(input);

<!-- Update UI -->
var parent = document.getElementById('utest-div');

var conn = document.createElement('div');

conn.appendChild(document.createTextNode("Selected Lines:"));
var selected_lines =  document.createElement('div');
selected_lines.setAttribute("id", "selectedLinesDiv");
selected_lines.innerText=""
conn.appendChild(selected_lines);


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

<!-- Update Code Mirror to select lines as target -->
<!-- TODO Maybe use the breakpoint stuff to visualize them -->

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
</script>