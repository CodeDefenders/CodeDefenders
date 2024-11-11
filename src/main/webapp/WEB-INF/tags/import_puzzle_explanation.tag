<%@ tag pageEncoding="UTF-8" %>

<p>
    The file tree below shows the file structure for a puzzle.
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
        Contains a single <code>.java</code> file to be used as the CUT (class under test).
    </li>
    <li>
        <code>cut/deps</code>:
        Contains any dependencies of the CUT as plain <code>.java</code> files.
        This folder is allowed to have arbitrary sub-folders.
    </li>
    <li>
        <code>mutants</code>:
        Contains the mutants, each in its separate sub-folder.
        All mutants should have the same name as the CUT.
    </li>
    <li>
        <code>tests</code>:
        Contains the tests, each in its separate sub-folder.
        Tests can have any name, except the name of the CUT.
    </li>
    <li>
        <code>puzzle.properties</code>:
        The configuration file of the puzzle.
    </li>
</ul>
