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
    ${i18n.tr('The file tree below shows the file structure for a puzzle.')}
</p>

<pre class="mb-3 p-3 bg-light" style="line-height: 1.15;">puzzle.zip
│
├─ cut
│   ├─ deps
│   │   ├─ SomeDependency.java
│   │   └─ OtherDependency.java
│   │
│   └─ Example.java
│
├─ mutants
│   ├─ 01
│   │   └─ Example.java
│   └─ 02
│       └─ Example.java
│
├─ tests
│   └─ 01
│       └─ TestExample.java
│
└─ puzzle.properties</pre>

<ul>
    <li>
        <code>cut</code>:
        ${i18n.tr('Contains a single .java file to be used as the CUT (class under test).')}
    </li>
    <li>
        <code>cut/deps</code>:
        ${i18n.tr('Contains any dependencies of the CUT as plain .java files.')}
        ${i18n.tr('This folder is allowed to have arbitrary sub-folders.')}
    </li>
    <li>
        <code>mutants</code>:
        ${i18n.tr('Contains the mutants, each in its separate sub-folder.')}
        ${i18n.tr('All mutants should have the same name as the CUT.')}
    </li>
    <li>
        <code>tests</code>:
        ${i18n.tr('Contains the tests, each in its separate sub-folder.')}
        ${i18n.tr('Tests can have any name, except the name of the CUT.')}
    </li>
    <li>
        <code>puzzle.properties</code>:
        ${i18n.tr('The configuration file of the puzzle.')}
    </li>
</ul>
