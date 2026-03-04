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
<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<p>
    ${i18n.tr('The file-tree below shows the file structure for a chapter:')}
</p>

<pre class="mb-3 p-3 bg-light" style="line-height: 1.15;">chapter.zip
│
├─ puzzle01
│   ├─ cut ...
│   ├─ mutants ...
│   ├─ tests ...
│   └─ puzzle.properties
│
├─ puzzle02.zip
│   ├─ cut ...
│   ├─ mutants ...
│   ├─ tests ...
│   └─ puzzle.properties
│
├─ ...
│
└─ chapter.properties</pre>

<p>
    ${i18n.tr('A chapter file can contain any number of puzzles.')}
    ${i18n.tr('Each puzzle is stored inside a sub-folder of the root.')}
    ${i18n.tr('The alphabetical order of the folder names determines the order of the puzzles.')}
    ${i18n.tr('A puzzle can also be stored in a nested .zip file instead of a sub-folder.')}
</p>

<p>
    ${i18n.tr('A chapter also contains a single chapter.properties file with the chapter title and description:')}
</p>

<pre class="mb-0 p-3 bg-light">${i18n.tr('# Title of the chapter.\ntitle=Puzzle 1\n\n# Description of the chapter.\ndescription=Puzzles of easy difficulty')}</pre>
