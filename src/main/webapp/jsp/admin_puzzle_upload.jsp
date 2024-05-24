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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ page import="org.codedefenders.util.Paths" %>

<p:main_page title="Puzzle Upload">
    <div class="container">
        <t:admin_navigation activePage="adminPuzzles"/>

        <h3>Upload Puzzles</h3>

        <p>Puzzle information is uploaded in a single zip file. For details on the convention, expand the description.</p>

        <form id="uploadPuzzles" name="uploadPuzzles" action="${url.forPath(Paths.ADMIN_PUZZLE_UPLOAD)}"
              class="form-width mb-3" method="post" enctype="multipart/form-data" autocomplete="off">
            <input type="hidden" name="formType" value="uploadPuzzles">

            <div class="row g-3 mb-3">
                <div class="col flex-grow-1">
                    <input class="form-control" type="file" id="fileUploadPuzzles" name="fileUploadPuzzles" accept=".zip">
                </div>
                <div class="col-auto">
                    <button class="btn btn-primary" type="submit" id="upload"
                            onclick="this.form.submit(); this.disabled = true; this.innerText='Uploading...';">
                        Upload
                    </button>
                </div>
            </div>

            <details>
                <summary>
                    Puzzle Upload Format
                </summary>
                <div class="mt-3">
                    <h4>Classes Under Test (CUTs)</h4>
                    <ul>
                        <li>Directory <code>cuts/</code></li>
                        <li>File convention: <code>cuts/&#60cut_alias&#62/&#60filename&#62</code></li>
                    </ul>

                    <h4>Mutants</h4>
                    <ul>
                        <li>Directory <code>mutants/</code></li>
                        <li>Requires <code>&#60cut_alias&#62</code> to refer to a CUT in this zip file.</li>
                        <li>File convention: <code>mutants/&#60cut_alias&#62/&#60position&#62/&#60filename&#62</code></li>
                    </ul>

                    <h4>Tests</h4>
                    <ul>
                        <li>Directory <code>tests/</code></li>
                        <li>Requires <code>&#60cut_alias&#62</code> to refer to a CUT in this zip file.</li>
                        <li>File convention: <code>tests/&#60cut_alias&#62/&#60position&#62/&#60filename&#62</code></li>
                    </ul>

                    <h4>Puzzle Chapters</h4>
                    <ul>
                        <li>Directory <code>puzzleChapters/</code></li>
                        <li>File convention: <code>puzzleChapters/&#60filename&#62.properties</code></li>
                        <li>File name is ignored</li>
                        <li>Mandatory properties:
                            <ul>
                                <li><code>chapterId</code> (unique integer value)</li>
                            </ul>
                        </li>
                        <li>Optional properties:
                            <ul>
                                <li><code>title</code></li>
                                <li><code>position</code> (integer value used to sort puzzle chapters)</li>
                                <li><code>description</code></li>
                            </ul>
                        </li>
                    </ul>

                    <h4>Puzzles</h4>
                    <ul class="mb-0">
                        <li>Directory <code>puzzles/</code></li>
                        <li>Requires <code>&#60cut_alias&#62</code> to refer to an existing CUT.</li>
                        <li>File convention: <code>puzzles/&#60cut_alias&#62/&#60puzzle_alias_ext&#62.properties</code></li>
                        <li><code>&#60puzzle_alias_ext&#62</code> is used for the puzzle alias, which is constructed as follows:
                            <code>&#60cut_alias&#62_puzzle_&#60puzzle_alias_ext&#62</code></li>
                        <li>Mandatory properties:
                            <ul>
                                <li><code>activeRole</code> ('DEFENDER' or 'ATTACKER')</li>
                                <li><code>gameLevel</code> ('EASY' or 'HARD')</li>
                                <li><code>chapterId</code> (has to be of an existing chapter)</li>
                            </ul>
                        </li>
                        <li>Optional properties:
                            <ul>
                                <li><code>mutants</code> (comma separated list of positions of mutants for the existing CUT)
                                </li>
                                <li><code>tests</code> (comma separated list of positions of tests for the existing CUT)</li>
                                <li><code>title</code></li>
                                <li><code>description</code></li>
                                <li><code>editableLinesStart</code> (first line the user can edit)</li>
                                <li><code>editableLinesEnd</code> (last line the user can edit)</li>
                                <li><code>position</code> (position in the puzzle chapter)</li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </details>

        </form>

    </div>
</p:main_page>
