<h3 style="margin-bottom: 0;">Create a mutant here</h3>

<form id="reset" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>"
      method="post">
    <input type="hidden" name="formType" value="reset">
    <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset"
            style="margin-top: -40px; margin-right: 80px">
        Reset
    </button>
</form>
<form id="atk" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>"
      method="post">
    <!-- TODO disableAttack(mg)/game.getActiveRole().equals(Role.ATTACKER))(dg) -->

    <% if (game.getState().equals(ACTIVE)) {%>
    <!-- TODO progressBar -->
    <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk"
            onClick="this.form.submit(); this.disabled=true; this.value='Attacking...';"
            style="margin-top: -50px">
        Attack!
    </button>
    <%}%>
    <input type="hidden" name="formType" value="createMutant">
    <input type="hidden" name="gameID" value="<%= game.getId() %>"/>
    <%
        String mutantCode;
        String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        if (previousMutantCode != null) {
            mutantCode = previousMutantCode;
        } else
            mutantCode = game.getCUT().getAsString();
    %>
    <pre style=" margin-top: 10px; "><textarea id="code" name="mutant" cols="80" rows="50"
                                               style="min-width: 512px;"><%= mutantCode %></textarea></pre>

    <%@include file="/jsp/multiplayer/game_key.jsp" %>

</form>

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

    var x = document.getElementsByClassName("utest");
    for (var i = 0; i < x.length; i++) {
        CodeMirror.fromTextArea(x[i], {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true
        });
    }
    /* Mutants diffs */
    $('.modal').on('shown.bs.modal', function () {
        try {
            var codeMirrorContainer = $(this).find(".CodeMirror")[0];
            if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                codeMirrorContainer.CodeMirror.refresh();
            } else {
                var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
                    lineNumbers: false,
                    mode: "diff",
                    readOnly: true /* onCursorActivity: null */
                });
                editorDiff.setSize("100%", 500);
            }
        } catch (e) {
        }
    });

    $('#finishedModal').modal('show');
</script>