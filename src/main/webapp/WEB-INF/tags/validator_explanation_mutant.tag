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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<h3>${i18n.tr('Mutation rules')}</h3>
<b>${i18n.tr('Relaxed')}</b> <br>
<ul>
    <li>${i18n.tr('No calls to {0},{1}', '<i>System.*</i>', '<i>Random.*</i>')}</li>
    <li>${i18n.tr('No mutants with only changes to comments or formatting')}</li>
    <li>${i18n.tr('No renaming of methods or fields, no additional methods or fields')}</li>
</ul>
<b>${i18n.tr('Moderate')}</b> <br>
<ul>
    <li>${i18n.tr('No changes to comments')}</li>
    <li>${i18n.tr('No additional logical operators ({0}, {1})', '<i>&&</i>', '<i>||</i>')}</li>
    <li>${i18n.tr('No ternary operators')}</li>
    <li>${i18n.tr('No new control structures ({0}, {1}, {2}, ...)', '<i>switch</i>', '<i>if</i>', '<i>for</i>')}</li>
</ul>
<b>${i18n.tr('Strict')}</b> <br>
<ul class="mb-0">
    <li>${i18n.tr('No reflection')}</li>
    <li>${i18n.tr('No bitwise operators (bitshifts and logical)')}</li>
    <li>${i18n.tr('No signature changes')}</li>
</ul>
