<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<div class="progress mx-3 mt-3" id="progress" style="height: 40px; font-size: 30px;" hidden>
    <div class="progress-bar" role="progressbar"
         style="font-size: 15px; line-height: 40px; width: 0;
                <%-- Disable animations because animations don't have time to finish before the page reloads. --%>
                transition: none; -o-transition: none; -webkit-transition: none;"
         aria-valuemin="0"
         aria-valuemax="100"
         aria-valuenow="0">
    </div>
</div>
