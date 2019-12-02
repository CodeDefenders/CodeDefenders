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
    <b>Relaxed</b> <br>
    <ul>
        <li>No calls to <i>System.*</i>,<i>Random.*</i></li>
    </ul>
    <b>Moderate</b> <br>
    <ul>
        <li>No comments</li>
        <li>No additional logical operators (<i>&&</i>, <i>||</i>)</li>
        <li>No ternary operators</li>
        <li>No new control structures (<i>switch</i>, <i>if</i>, <i>for</i>, ...)</li>
    </ul>
    <b>Strict</b> <br>
    <ul>
        <li>No reflection</li>
        <li>No bitwise operators (bitshifts and logical)</li>
        <li>No signature changes</li>
    </ul>

</div>
