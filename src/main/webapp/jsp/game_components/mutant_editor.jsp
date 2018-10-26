<%--
    Displays the mutant code in a CodeMirror textarea.

    @param String testCode
        The test code to display.
--%>

<% { %>

<%
    final String mutantCode = (String) request.getAttribute("mutantCode");
    final String mutantName = (String) request.getAttribute("mutantName");
    final Map<String, String> dependencies = (Map<String, String>) request.getAttribute("dependencies");
%>
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

        var texts = [editorMutant.getValue()];
        if (typeof autocompletedClasses !== 'undefined') {
            Object.getOwnPropertyNames(autocompletedClasses).forEach(function(key) {
                texts.push(autocompletedClasses[key]);
            });
        }

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

</script>

<%
    if (dependencies.isEmpty()) { // no dependencies -> no tabs
%>
<pre style="margin-top: 10px;"><textarea id="code" name="mutant" title="mutant" cols="80"
                                         rows="50"><%=mutantCode%></textarea></pre>

<script>
    var editorMutant = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java",
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {"Ctrl-Space": "autocomplete"},
        keyMap: "default",
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });

    editorMutant.setSize("100%", 500);
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
                var editorMutant = CodeMirror.fromTextArea(document.getElementById("code"), {
                    lineNumbers: true,
                    indentUnit: 4,
                    smartIndent: true,
                    indentWithTabs: true,
                    matchBrackets: true,
                    mode: "text/x-java",
                    autoCloseBrackets: true,
                    styleActiveLine: true,
                    extraKeys: {"Ctrl-Space": "autocomplete"},
                    keyMap: "default",
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
                });

                editorMutant.setSize("100%", 500);

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
        <%-- Without the hideAfterRendering class attribute the editor is only rendered
             when the editor is displayed and actively clicked on.--%>
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
<%
}
%>
