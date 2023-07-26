<%--

    Copyright (C) 2016-2023 Code Defenders contributors

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
<p>
    This value refers to the number of tests which cover, but do not
    kill, mutants and controls if and when equivalence duels are
    automatically triggered.
</p>
<ul class="mb-0">
    <li><b>0</b> disables the automatic triggering of equivalence
        mutants.
    <li><b>N > 0</b> triggers an equivalence duels for every mutant
        which survives N tests.
</ul>
