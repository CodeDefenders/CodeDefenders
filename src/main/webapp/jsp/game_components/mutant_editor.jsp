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
    Displays the mutant code in a CodeMirror textarea.

    @param String mutantCode
        The mutant code to display, but not {@code null}.
    @param String mutantName
        The class name of the mutant, but not {@code null}.
    @param Map<String, String> dependencies
        A mapping between a class name and the content of the CUT dependencies.
        Can be empty, but must not be {@code null}.
    @param Integer startEditLine
        Start of editable lines. If smaller than one or {@code null}, the code can
        be modified from the start.
    @param Integer endEditLine
        End of editable lines in the orginial mutant.
        If smaller than one or {@code null}, the code can be modified until the end.
    @param errorLines the list of lines on which the compiler reported an error.
        Those have to be highlighted
--%>

<%@ page import="java.util.Map" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    final String mutantCode = (String) request.getAttribute("mutantCode");
    final String mutantName = (String) request.getAttribute("mutantName");
    final Map<String, String> dependencies = (Map<String, String>) request.getAttribute("dependencies");

    Integer startEditLine = (Integer) request.getAttribute("startEditLine");
    if (startEditLine == null || startEditLine < 1) {
        startEditLine = 1;
    }
    Integer endEditLine = (Integer) request.getAttribute("endEditLine");
    if (endEditLine != null && (endEditLine < 1 || endEditLine < startEditLine)) {
        endEditLine = null;
    }
%>

<script>
    let startEditLine = <%=startEditLine%> ;
    let readOnlyLinesStart = Array.from(new Array(startEditLine - 1).keys());

    let endEditLine = <%=endEditLine%>;

    let getReadOnlyLinesEnd = function(lines) {
        if (endEditLine == null) {
            return [];
        }
        let readOnlyLines = [];

        // You don't want the end line to be editable even when a user removes above lines.
        // So the endEditLine isn't a static hard limit, but an indicator that
        // totalLines - endEditLine many lines from the bottom are read only.
        for (let i = 1; i <= getReadOnlyBottomNumber(lines); i++) {
            readOnlyLines.push(lines.length - i)
        }
        return readOnlyLines;
    };

    let numberOfReadOnlyLinesFromBottom = null;
    let getReadOnlyBottomNumber = function(lines) {
        if (endEditLine != null && numberOfReadOnlyLinesFromBottom == null) {
            numberOfReadOnlyLinesFromBottom = lines.length - endEditLine;
        }
        return numberOfReadOnlyLinesFromBottom;

    };

    filterOutComments = function (text) {
        let commentRegex = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;
        return text.replace(commentRegex, "");
    };

    let updateAutocompleteList = function () {
        let wordRegex = /[a-zA-Z][a-zA-Z0-9]*/gm;
        let set = new Set();

        let texts = [editorMutant.getValue()];
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

        autocompleteList = Array.from(set);
    };
    CodeMirror.commands.autocomplete = function (cm) {
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
                let curWord = start != end && currentLine.slice(start, end);
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

</script>

<%
    if (dependencies.isEmpty()) { // no dependencies -> no tabs
%>
<pre style="margin-top: 10px;"><textarea id="code" name="mutant" title="mutant" cols="80"
                                         rows="50"><%=mutantCode%></textarea></pre>

<script>
    let editorMutant = CodeMirror.fromTextArea(document.getElementById("code"), {
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
        keyMap: "${login.user.keyMap.CMName}",
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });

    editorMutant.setSize("100%", 500);

    editorMutant.on('beforeChange', function (cm, change) {
        let text = cm.getValue();
        let lines = text.split(/\r|\r\n|\n/);

        let readOnlyLinesEnd = getReadOnlyLinesEnd(lines);
        if (~readOnlyLinesStart.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
            change.cancel();
        }
    });

	// All the lines which are not between startEditLine and endEditLine() must be greyed out
	// https://stackoverflow.com/questions/1720320/how-to-dynamically-create-css-class-in-javascript-and-apply
	// https://stackoverflow.com/questions/5081690/how-to-gray-out-a-html-element
	// Define the Grayout style
    if(endEditLine != null) {
        var style = document.createElement('style');
        style.type = 'text/css';
        style.innerHTML = '.grayout { opacity: 0.5; filter: alpha(opacity = 50);}';
        document.getElementsByTagName('head')[0].appendChild(style);
        // Apply the gray out style to the non-editable lines
        var count = editorMutant.lineCount();
        for (i = 0; i < count; i++) {
            if ((i + 1) < startEditLine || (i + 1) > endEditLine) {
                editorMutant.addLineClass(i, "text", "grayout");
            }
        }
    }
</script>

<%
    } else { // dependencies exist -> tab system
%>
<div>
    <ul class="nav nav-tabs" style="margin-top: 15px">
        <li role="presentation" class="active"><a href="#<%=mutantName%>" aria-controls="<%=mutantName%>" role="tab" data-toggle="tab"><%=mutantName%></a></li>
        <%
            for (String depName : dependencies.keySet()) {
        %>
        <li role="presentation"><a href="#<%=depName%>" aria-controls="<%=depName%>" role="tab" data-toggle="tab"><%=depName%></a></li>
        <%
            }
        %>
    </ul>

    <div class="tab-content">
        <div role="tabpanel" class="tab-pane active" id="<%=mutantName%>" data-toggle="tab">
            <pre><textarea id="code" name="mutant" title="mutant" cols="80"
                                                     rows="50"><%=mutantCode%></textarea></pre>
            <script>
                let editorMutant = CodeMirror.fromTextArea(document.getElementById("code"), {
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
                    keyMap: "${login.user.keyMap.CMName}",
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
                });

                editorMutant.setSize("100%", 500);

                editorMutant.on('beforeChange', function (cm, change) {
                    let text = cm.getValue();
                    let lines = text.split(/\r|\r\n|\n/);

                    let readOnlyLinesEnd = getReadOnlyLinesEnd(lines);
                    if (~readOnlyLinesStart.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
                        change.cancel();
                    }
                });

                autocompletedClasses = {
                    '<%=mutantName%>': editorMutant.getTextArea().value
                }
            </script>
        </div>
        <%
            for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
                String depName = dependency.getKey();
                String depCode = dependency.getValue();
        %>
        <div role="tabpanel" class="tab-pane active hideAfterRendering" id="<%=depName%>" data-toggle="tab">
            <pre class="readonly-pre"><textarea class="readonly-textarea" id="text-<%=depName%>"
                                                name="text-<%=depName%>"
                                                title="text-<%=depName%>" cols="80"
                                                rows="30"><%=depCode%></textarea></pre>
            <script>
                let editor<%=depName%> = CodeMirror.fromTextArea(document.getElementById("text-<%=depName%>"), {
                    lineNumbers: true,
                    matchBrackets: true,
                    mode: "text/x-java",
                    readOnly: true
                });
                editor<%=depName%>.setSize("100%", 500); // next to the test editor the cm editor would be too big
                editor<%=depName%>.refresh();

                Object.assign(autocompletedClasses, {
                    '<%=depName%>': editor<%=depName%>.getTextArea().value
                });
            </script>
        </div>
        <%
            }
        %>
    </div>
    <script>
        // Please don't blame me.

        // Without the hideAfterRendering class attribute the editor is only rendered
        // when the editor is displayed and actively clicked on
        $('.hideAfterRendering').each(function () {
            $(this).removeClass('active')
        });
    </script>
</div>

<%
    }
%>
<script>
    editorMutant.on('focus', function () {
        updateAutocompleteList();
    });
    editorMutant.on('keyHandled', function (cm, name, event) {
        // 9 == Tab, 13 == Enter
        if ([9, 13].includes(event.keyCode)) {
            updateAutocompleteList();
        }
    });
</script>
