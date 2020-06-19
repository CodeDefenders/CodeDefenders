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

<%--
    Displays the test code in a CodeMirror textarea.
--%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>

<pre><textarea id="test-code" name="test" title="test" cols="80" rows="30">${testEditor.testCode}</textarea></pre>

<script>
(function () {

    let startEditLine = ${testEditor.editableLinesStart};
    let readOnlyLinesStart = Array.from(new Array(startEditLine - 1).keys());

    let getReadOnlyLinesEnd = function(lines) {
        // Test editor permits only last two lines
        return [lines.length - 2, lines.length - 1];
    };

    // If you make changes to the autocompletion, change it for an attacker too.
    testMethods = ["assertArrayEquals", "assertEquals", "assertTrue", "assertFalse", "assertNull",
        "assertNotNull", "assertSame", "assertNotSame", "fail"];

    <% if(testEditor.isMockingEnbaled()) { %>
        mockitoMethods = ["mock", "when", "then", "thenThrow", "doThrow", "doReturn", "doNothing"];
        // Answer object handling is currently not included (Mockito.doAnswer(), OngoingStubbing.then/thenAnswer
        // Calling real methods is currently not included (Mockito.doCallRealMethod / OngoingStubbing.thenCallRealMethod)
        // Behavior verification is currentlty not implemented (Mockito.verify)
        testMethods = testMethods.concat(mockitoMethods);
    <% } %>

    let autocompleteList = [];

    filterOutComments = function(text) {
        let commentRegex = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;
        return text.replace(commentRegex, "");
    };

    let updateAutocompleteList = function () {
        let wordRegex = /[a-zA-Z][a-zA-Z0-9]*/gm;
        let set = new Set(testMethods);

        let testClass = editorTest.getValue().split("\n");
        testClass.slice(startEditLine, testClass.length - 2);
        testClass = testClass.join("\n");
        let texts = [testClass];

        const autocompletedClasses = window.autocompletedClasses;
        if (typeof autocompletedClasses !== 'undefined') {
            Object.getOwnPropertyNames(autocompletedClasses).forEach(function(key) {
                texts.push(autocompletedClasses[key]);
            });
        }

        texts.forEach(function (text) {
            text = filterOutComments(text);
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
        autocompleteList =  Array.from(set);
    };

    let editorTest = CodeMirror.fromTextArea(document.getElementById("test-code"), {
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        matchBrackets: true,
        mode: "text/x-java",
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {
            "Ctrl-Space": "autocompleteTest",
            "Tab": "insertSoftTab"
        },
        keyMap: "${login.user.keyMap.CMName}",
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });


    CodeMirror.commands.autocompleteTest = function (cm) {
        cm.showHint({
            hint: function (editor) {
                let reg = /[a-zA-Z][a-zA-Z0-9]*/;

                let list = autocompleteList;
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

    editorTest.on('beforeChange', function (cm, change) {
        let text = cm.getValue();
        let lines = text.split(/\r|\r\n|\n/);

        let readOnlyLinesEnd = getReadOnlyLinesEnd(lines);
        if (~readOnlyLinesStart.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
            change.cancel();
        }
    });

    editorTest.on('focus', function () {
        updateAutocompleteList();
    });

    editorTest.on('keyHandled', function (cm, name, event) {
        // 9 == Tab, 13 == Enter
        if ([9, 13].includes(event.keyCode)) {
            updateAutocompleteList();
        }
    });

    editorTest.setSize("100%", 500);

})();
</script>
