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
<div class="panel-body" style="padding: 10px;">
	<p>This value refers to the number of tests which cover, but do not
		kill, mutants and controls if and when equivalence duels are
		automatically triggered.
		<ul>
			<li><b>0</b> disables the automatic triggering of equivalence
				mutants.
			<li><b>X (X>0)</b> triggers an equivalence duels for every mutant
				which survives X tests.
		</ul>
	</p>
</div>