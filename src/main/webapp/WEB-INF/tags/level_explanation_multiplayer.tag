<%--
  ~ Copyright (C) 2023 Code Defenders contributors
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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<b>Easy</b>
<ul>
    <li>
        Teams are allowed to view the other team's submissions.
        <ul>
            <li>Attackers are allowed to view the defender team's tests.</li>
            <li>Defenders are allowed to view the attacker team's mutants.</li>
        </ul>
    </li>
</ul>

<b>Hard</b>
<ul class="mb-0">
    <li>Teams are <b>not</b> allowed to view the other team's submissions.</li>
    <li>But: Defenders can still view mutants after they are killed or accepted as equivalent.</li>
</ul>
