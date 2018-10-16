<%@ page import="org.apache.commons.lang.StringEscapeUtils" %><%--
    Displays the mutant code in a CodeMirror textarea.

    @param String testCode
        The test code to display.
--%>

<% { %>

<%
    String mutantCode = (String) request.getAttribute("mutantCode");
%>

<pre style="margin-top: 10px;"><textarea id="code" name="mutant" title="mutant" cols="80" rows="50"><%= StringEscapeUtils.escapeHtml(mutantCode)%></textarea></pre>

<script>
    autocompletelist = [];

    filterOutComments = function (text) {
        var blockCommentRegex = /\/\*(.|\s)*?\*\//gm;
        var lineCommentRegex = /\/\/.*(\r\n|\r|\n)/g;
        return text.replace(blockCommentRegex, "").replace(lineCommentRegex, "")
    };

    updateAutocompleteList = function () {
        var wordRegex = /[a-zA-Z][a-zA-Z0-9]*/gm;
        var set = new Set();

        var texts = [editorSUT.getValue()];

        texts.forEach(function (text) {
            text = filterOutComments(text);
            var m;
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
                var reg = /[a-zA-Z][a-zA-Z0-9]*/;

                var list = !autocompleteList ? [] : autocompleteList;
                var cursor = editor.getCursor();
                var currentLine = editor.getLine(cursor.line);
                var start = cursor.ch;
                var end = start;
                while (end < currentLine.length && reg.test(currentLine.charAt(end))) ++end;
                while (start && reg.test(currentLine.charAt(start - 1))) --start;
                var curWord = start != end && currentLine.slice(start, end);
                var regex = new RegExp('^' + curWord, 'i');
                var result = {
                    list: (!curWord ? list : list.filter(function (item) {
                        return item.match(regex);
                    })).sort(),
                    from: CodeMirror.Pos(cursor.line, start),
                    to: CodeMirror.Pos(cursor.line, end)
                };

                return result;
            }
        });
    };

    var editorSUT = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java",
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {"Ctrl-Space": "autocomplete"},
        keyMap: "default"
    });

    editorSUT.setSize("100%", 500);

    editorSUT.on('focus', function () {
        updateAutocompleteList();
    });
    editorSUT.on('keyHandled', function (cm, name, event) {
        // 9 == Tab, 13 == Enter
        if ([9, 13].includes(event.keyCode)) {
            updateAutocompleteList();
        }
    });
</script>

<% } %>
