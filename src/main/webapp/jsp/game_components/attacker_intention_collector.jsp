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

var intentionDropDown = document.createElement("SELECT");
intentionDropDown.setAttribute("id", "intention-table");
intentionDropDown.setAttribute("style", "height: 25px; width: 20%; margin-top: 10px");
intentionDropDown.setAttribute("onchange", "updateAttackForm(this.value)");
intentionDropDown.innerHTML = '<option selected disabled hidden>My mutant is</option>' +
    '<option value="KILLABLE" name="attacker-intention" onclick="updateAttackForm(\'KILLABLE\')">Killable</option>' +
    '<option value="EQUIVALENT" name="attacker-intention" onclick="updateAttackForm(\'EQUIVALENT\')">Equivalent</option>' +
    '<option value="DONTKNOW" name="attacker-intention" onclick="updateAttackForm(\'DONTKNOW\')">I don\'t know</option>' +
    '</select>';

container.appendChild(theTable)

</script>