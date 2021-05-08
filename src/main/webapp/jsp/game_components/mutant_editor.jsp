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
<%@ page import="java.util.Map" %>

<%--
    Displays the mutant code in a CodeMirror textarea.
--%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>

<div class="card codemirror-card">

    <%-- no dependencies -> no tabs --%>
    <% if (!mutantEditor.hasDependencies()) { %>

        <div class="card-body p-0">
            <pre class="m-0"><textarea id="mutant-code" name="mutant" title="mutant" cols="80" rows="50">${mutantEditor.mutantCode}</textarea></pre>
        </div>

    <%-- dependencies exist -> tab system --%>
    <% } else { %>

        <div class="card-header">
            <ul class="nav nav-pills nav-fill gap-1" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link px-2 py-1 active" data-bs-toggle="tab"
                            id="${mutantEditor.className}-tab"
                            data-bs-target="#${mutantEditor.className}"
                            aria-controls="${mutantEditor.className}"
                            type="button" role="tab" aria-selected="true">
                        ${mutantEditor.className}
                    </button>
                </li>
                <% for (String depName : mutantEditor.getDependencies().keySet()) { %>
                <li class="nav-item" role="presentation">
                    <button class="nav-link px-2 py-1" data-bs-toggle="tab"
                            id="<%=depName%>-tab"
                            data-bs-target="#<%=depName%>"
                            aria-controls="<%=depName%>"
                            type="button" role="tab" aria-selected="true">
                        <%=depName%>
                    </button>
                </li>
                <% } %>
            </ul>
        </div>

        <div class="card-body p-0">
            <div class="tab-content">
                <div class="tab-pane active"
                     id="${mutantEditor.className}"
                     aria-labelledby="${mutantEditor.className}-tab"
                     role="tabpanel">
                    <pre class="m-0" readonly><textarea id="mutant-code" name="mutant" title="mutant" cols="80" rows="30">${mutantEditor.mutantCode}</textarea></pre>
                </div>
                <% for (Map.Entry<String, String> dependency : mutantEditor.getDependencies().entrySet()) {
                    String depName = dependency.getKey();
                    String depCode = dependency.getValue(); %>
                    <div class="tab-pane"
                         id="<%=depName%>"
                         aria-labelledby="<%=depName%>-tab"
                         role="tabpanel">
                             <pre class="m-0" readonly><textarea id="text-<%=depName%>"
                                                                 name="text-<%=depName%>"
                                                                 title="text-<%=depName%>"
                                                                 cols="80" rows="30"><%=depCode%></textarea></pre>
                    </div>
                <% } %>
            </div>
        </div>

    <% } %>

</div>


<script>
(function () {

    /* ==================== Scaffolding ==================== */

    let mutantStartEditLine = ${mutantEditor.editableLinesStart};
    let mutantReadOnlyLinesStart = Array.from(new Array(mutantStartEditLine - 1).keys());

    let mutantEndEditLine = ${mutantEditor.hasEditableLinesEnd() ? mutantEditor.editableLinesEnd : "null"};

    let mutantGetMutantReadOnlyLinesEnd = function (lines) {
        if (mutantEndEditLine == null) {
            return [];
        }
        let mutantReadOnlyLines = [];

        // You don't want the end line to be editable even when a user removes above lines.
        // So the mutantEndEditLine isn't a static hard limit, but an indicator that
        // totalLines - mutantEndEditLine many lines from the bottom are read only.
        for (let i = 1; i <= mutantGetReadOnlyBottomNumber(lines); i++) {
            mutantReadOnlyLines.push(lines.length - i)
        }
        return mutantReadOnlyLines;
    };

    let numberOfMutantReadOnlyLinesFromBottom = null;
    let mutantGetReadOnlyBottomNumber = function(lines) {
        if (mutantEndEditLine != null && numberOfMutantReadOnlyLinesFromBottom == null) {
            numberOfMutantReadOnlyLinesFromBottom = lines.length - mutantEndEditLine;
        }
        return numberOfMutantReadOnlyLinesFromBottom;
    };

    const mutantFilterOutComments = function (text) {
        let commentRegex = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;
        return text.replace(commentRegex, "");
    };

    let mutantUpdateAutocompleteList = function () {
        let wordRegex = /[a-zA-Z][a-zA-Z0-9]*/gm;
        let set = new Set();

        let texts = [editorMutant.getValue()];

        const autocompletedClasses = window.autocompletedClasses;
        if (typeof autocompletedClasses !== 'undefined') {
            Object.getOwnPropertyNames(autocompletedClasses).forEach(function (key) {
                texts.push(autocompletedClasses[key]);
            });
        }

        texts.forEach(function (text) {
            text = mutantFilterOutComments(text);
            let m;
            while ((m = wordRegex.exec(text)) !== null) {
                if (m.index === wordRegex.lastIndex) {
                    wordRegex.lastIndex++;
                }
                m.forEach(function (match) {
                    set.add(match)
                });
            }
        });

        autocompleteList = Array.from(set);
    };

    CodeMirror.commands.autocompleteMutant = function (cm) {
        cm.showHint({
            hint: function (editor) {
                let reg = /[a-zA-Z][a-zA-Z0-9]*/;

                let list = !autocompleteList ? [] : autocompleteList;
                let cursor = editor.getCursor();
                let currentLine = editor.getLine(cursor.line);
                let start = cursor.ch;
                let end = start;
                while (end < currentLine.length && reg.test(currentLine.charAt(end))) ++end;
                while (start && reg.test(currentLine.charAt(start - 1))) --start;
                let curWord = start !== end && currentLine.slice(start, end);
                let regex = new RegExp('^' + curWord, 'i');

                return {
                    list: (!curWord ? list : list.filter(function (item) {
                        return item.match(regex);
                    })).sort(),
                    from: CodeMirror.Pos(cursor.line, start),
                    to: CodeMirror.Pos(cursor.line, end)
                };
            }
        });
    };


    /* ==================== Initialize main mutant editor ==================== */

    let editorMutant = CodeMirror.fromTextArea(document.getElementById("mutant-code"), {
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        matchBrackets: true,
        mode: "text/x-java",
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {
            "Ctrl-Space": "autocompleteMutant",
            "Tab": "insertSoftTab"
        },
        keyMap: "${login.user.keyMap.CMName}",
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons'],
        autoRefresh: true
    });

    editorMutant.on('beforeChange', function (cm, change) {
        let text = cm.getValue();
        let lines = text.split(/\r|\r\n|\n/);

        let mutantReadOnlyLinesEnd = mutantGetMutantReadOnlyLinesEnd(lines);
        if (~mutantReadOnlyLinesStart.indexOf(change.from.line) || ~mutantReadOnlyLinesEnd.indexOf(change.to.line)) {
            change.cancel();
        }
    });

    // All the lines which are not between mutantStartEditLine and mutantEndEditLine() must be greyed out
    // https://stackoverflow.com/questions/1720320/how-to-dynamically-create-css-class-in-javascript-and-apply
    // https://stackoverflow.com/questions/5081690/how-to-gray-out-a-html-element
    // Define the Grayout style
    if (mutantEndEditLine != null) {
        var style = document.createElement('style');
        style.innerHTML = '.grayout { opacity: 0.5; filter: alpha(opacity = 50);}';
        document.getElementsByTagName('head')[0].appendChild(style);
        // Apply the gray out style to the non-editable lines
        var count = editorMutant.lineCount();
        for (i = 0; i < count; i++) {
            if ((i + 1) < mutantStartEditLine || (i + 1) > mutantEndEditLine) {
                editorMutant.addLineClass(i, "text", "grayout");
            }
        }
    }

    editorMutant.on('focus', function () {
        mutantUpdateAutocompleteList();
    });
    editorMutant.on('keyHandled', function (cm, name, event) {
        // 9 == Tab, 13 == Enter
        if ([9, 13].includes(event.keyCode)) {
            mutantUpdateAutocompleteList();
        }
    });

    /* If global autocompletedClasses exists, get it, otherwise, create it. */
    const autocompletedClasses = window.autocompletedClasses = window.autocompletedClasses || {};
    autocompletedClasses['${mutantEditor.className}'] = editorMutant.getTextArea().value;


    /* ==================== Initialize dependency viewers ==================== */

    <%-- dependencies exist -> tab system --%>
    <% if (mutantEditor.hasDependencies()) { %>

        <% for (Map.Entry<String, String> dependency : mutantEditor.getDependencies().entrySet()) {
            String depName = dependency.getKey(); %>

                let editor<%=depName%> = CodeMirror.fromTextArea(document.getElementById("text-<%=depName%>"), {
                    lineNumbers: true,
                    matchBrackets: true,
                    mode: "text/x-java",
                    readOnly: true,
                    autoRefresh: true
                });

                autocompletedClasses['<%=depName%>'] =  editor<%=depName%>.getTextArea().value;
        <% } %>

    <% } %>

})();
</script>
