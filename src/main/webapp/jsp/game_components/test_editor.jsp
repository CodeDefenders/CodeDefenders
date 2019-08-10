<%@ page import="org.codedefenders.model.KeyMap" %><%--

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

    @param String testCode
        The test code to display.
    @param Boolean mockingEnabled
        Enable autocompletions for Mockito methods.
    @param Integer startEditLine
        Start of editable lines. If smaller than one or {@code null}, the code can
        be modified from the start.
    @param KeyMap user-keymap (session attribute)
        The user's preferred key map. Can be {@code null}.
--%>

<% { %>

<%
    String testCode = (String) request.getAttribute("testCode");
    Boolean mockingEnabled = (Boolean) request.getAttribute("mockingEnabled");

    Integer startEditLine = (Integer) request.getAttribute("startEditLine");
    if (startEditLine == null || startEditLine < 1) {
        startEditLine = 1;
    }
    KeyMap keymap = ((KeyMap) session.getAttribute("user-keymap"));
%>

<pre><textarea id="code" name="test" title="test" cols="80" rows="30"><%=testCode%></textarea></pre>

<script>
    let startEditLine = <%=startEditLine%> ;
    let readOnlyLinesStart = Array.from(new Array(startEditLine - 1).keys());

    let getReadOnlyLinesEnd = function(lines) {
        // Test editor permits only last two lines
        return [lines.length - 2, lines.length - 1];
    };

    // If you make changes to the autocompletion, change it for an attacker too.
    testMethods = ["assertArrayEquals", "assertEquals", "assertTrue", "assertFalse", "assertNull",
        "assertNotNull", "assertSame", "assertNotSame", "fail"];

    <% if(mockingEnabled) { %>
        mockitoMethods = ["mock", "when", "then", "thenThrow", "doThrow", "doReturn", "doNothing"];
        // Answer object handling is currently not included (Mockito.doAnswer(), OngoingStubbing.then/thenAnswer
        // Calling real methods is currently not included (Mockito.doCallRealMethod / OngoingStubbing.thenCallRealMethod)
        // Behavior verification is currentlty not implemented (Mockito.verify)
        testMethods = testMethods.concat(mockitoMethods);
    <% } %>

    var autocompleteList = [];

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

    CodeMirror.commands.autocomplete = function (cm) {
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

    let editorTest = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        matchBrackets: true,
        mode: "text/x-java",
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {
            "Ctrl-Space": "autocomplete",
            "Tab": "insertSoftTab"
        },
        keyMap: "<%=keymap.getCMName()%>",
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });

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
</script>

<% } %>
