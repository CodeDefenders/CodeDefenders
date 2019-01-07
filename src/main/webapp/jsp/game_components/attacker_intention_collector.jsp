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

<script>

function updateAttackForm(value){
	var attackButton = document.getElementById('submitMutant');
	attackButton.disabled = false;
	document.getElementById("attacker_intention").value = value;
}

var theForm = document.getElementById('atk');

var input = document.createElement("input");
input.setAttribute("type", "hidden");
input.setAttribute("id", "attacker_intention");
input.setAttribute("name", "attacker_intention");
input.setAttribute("value", "");
//append to form element that you want .
theForm.appendChild(input);


var attackButton = document.getElementById('submitMutant');
attackButton.setAttribute("style","margin-top: -0px");
attackButton.disabled = true;

var resetButton = document.getElementById('btnReset');
resetButton.setAttribute("style","margin-top: -0px; margin-right: 0px");

var container = document.createElement('div');
container.setAttribute("style", "width: 100%");

// Put the container before the form
//parent.insertAfter(container, theForm);
theForm.insertBefore(container, attackButton.nextSibling);

var theTable = document.createElement("TABLE");
theTable.setAttribute("id", "intention-table");
theTable.setAttribute("style", "height: 40px");

//Insert cells
var newRow = theTable.insertRow();
var newCell  = newRow.insertCell();
//Write the HTML inside the cell
newCell.innerHTML='<strong>Is your mutant:</strong>';

var newCell  = newRow.insertCell();
//Write the HTML inside the cell
newCell.innerHTML='<input type="radio" name="attacker-intention" value="killable" onmousedown="updateAttackForm(\'KILLABLE\')"/>&nbsp;Killable';


var newCell  = newRow.insertCell();
//Write the HTML inside the cell
newCell.innerHTML='<input type="radio" name="attacker-intention" value="equivalent" onmousedown="updateAttackForm(\'EQUIVALENT\')"/>&nbsp;Equivalent';


var newCell  = newRow.insertCell();
//Write the HTML inside the cell
newCell.innerHTML='<input type="radio" name="attacker-intention" value="dontknow" onmousedown="updateAttackForm(\'DONTKNOW\')"/>&nbsp;I don\'t know';

container.appendChild(theTable)

</script>