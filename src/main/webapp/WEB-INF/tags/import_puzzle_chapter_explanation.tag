<%@ tag pageEncoding="UTF-8" %>

<p>
    The file-tree below shows the file structure for a chapter:
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
    A chapter file can contain any number of puzzles.
    Each puzzle is stored inside a sub-folder of the root.
    The alphabetical order of the folder names determines the order of the puzzles.
    A puzzle can also be stored in a nested <code>.zip</code> file instead of a sub-folder.
</p>

<p>
    A chapter also contains a single <code>chapter.properties</code> file with the chapter title and description:
</p>

<pre class="mb-0 p-3 bg-light"># Title of the chapter.
title=Puzzle 1

# Description of the chapter.
description=Puzzles of easy difficulty</pre>
