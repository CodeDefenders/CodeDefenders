<%--
  ~ Copyright (C) 2021 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<h3>Mutation rules</h3>
<b>Relaxed</b> <br>
<ul>
    <li>No calls to <i>System.*</i>,<i>Random.*</i></li>
    <li>No mutants with only changes to comments or formatting</li>
</ul>
<b>Moderate</b> <br>
<ul>
    <li>No changes to comments</li>
    <li>No additional logical operators (<i>&&</i>, <i>||</i>)</li>
    <li>No ternary operators</li>
    <li>No new control structures (<i>switch</i>, <i>if</i>, <i>for</i>, ...)</li>
</ul>
<b>Strict</b> <br>
<ul class="mb-0">
    <li>No reflection</li>
    <li>No bitwise operators (bitshifts and logical)</li>
    <li>No signature changes</li>
</ul>
