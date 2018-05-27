<%@ page import="org.codedefenders.model.NotificationType" %>
<%
	codeDivName = "cut-div";
%>
<div class="ws-12">
	<div class="col-md-6" id="cut-div">
		<h2>Class Under Test</h2>
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30"><%=mg.getCUT().getAsString()%></textarea></pre>
		<%@include file="/jsp/multiplayer/game_key.jsp"%>
	</div> <!-- col-md6 left -->
	<div class="col-md-6" id="utest-div" style="float: right; min-width: 480px">
		<h2> Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
					<% if (!mg.getState().equals(GameState.ACTIVE)) { %> disabled <% } %>>
				Defend!
			</button>
		</h2>
		<form id="def" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
					testCode = previousTestCode;
				} else
					testCode = mg.getCUT().getTestTemplate();
			%>
			<pre><textarea id="code" name="test" cols="80" rows="30"><%= testCode %></textarea></pre>
			<input type="hidden" name="formType" value="createTest">
			<input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
		</form>
	</div> <!-- col-md6 right top -->
</div> <!-- 	row-fluid 1 -->

</div>
<div class="crow fly up">
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>
	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
</div>
<div>
	<script>
        junitMethods = ["assertArrayEquals", "assertEquals", "assertTrue", "assertFalse", "assertNull",
            "assertNotNull", "assertSame", "assertNotSame", "fail"];
        autocompletelist = [];

        updateAutocompleteList = function () {
            var regex = /[a-zA-Z][a-zA-Z0-9]*/gm;
            var set = new Set(junitMethods);

            var testClass = editorTest.getValue();
            testClass.slice(8, testClass.length - 2);

            var texts = [testClass, editorSUT.getValue()];

            texts.forEach(function (text) {
                var m;
                while ((m = regex.exec(text)) !== null) {
                    if (m.index === regex.lastIndex) {
                        regex.lastIndex++;
                    }
                    m.forEach(function (match) {
                        set.add(match)
                    });
                }
            });
            autocompletelist = Array.from(set);
        };

        CodeMirror.commands.autocomplete = function (cm) {
            cm.showHint({
                hint: function (editor) {
                    var reg = /[a-zA-Z][a-zA-Z0-9]*/;

                    var list = autocompletelist;
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

		var editorTest = CodeMirror.fromTextArea(document.getElementById("code"), {
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
		editorTest.on('beforeChange',function(cm,change) {
			var text = cm.getValue();
			var lines = text.split(/\r|\r\n|\n/);
			var readOnlyLines = [0,1,2,3,4,5,6,7];
			var readOnlyLinesEnd = [lines.length-1,lines.length-2];
			if ( ~readOnlyLines.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
				change.cancel();
			}
		});
        editorTest.on('keyHandled', function (cm, name, event) {
            // 9 == Tab, 13 == Enter
            if ([9, 13].includes(event.keyCode)) {
                updateAutocompleteList();
            }
        });
		editorTest.setSize("100%", 500);
		var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
			lineNumbers: true,
			matchBrackets: true,
			mode: "text/x-java",
			readOnly: true
		});
		editorSUT.setSize("100%", 500);

		/* Submitted tests */
		var x = document.getElementsByClassName("utest");
		var i;
		for (i = 0; i < x.length; i++) {
			CodeMirror.fromTextArea(x[i], {
				lineNumbers: true,
				matchBrackets: true,
				mode: "text/x-java",
				readOnly: true
			});
		}
		/* Mutants diffs */
		$('.modal').on('shown.bs.modal', function() {
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
		});

		$('#finishedModal').modal('show');
	</script>
	<script type="text/javascript">
	
	function typeOf(obj) {
		  return {}.toString.call(obj).split(' ')[1].slice(0, -1).toLowerCase();
		}
	
    var updateProgressBar = function(url) {
        var progressBarDiv = document.getElementById("progress-bar");
        $.get(url, function (r) {
              $(r).each(function (index) {
         					switch( r[index] ){
           						case 'COMPILE_TEST': // After test is compiled
           							progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 50%; font-size: 15px; line-height: 40px;" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100">Running Test Against Original</div>';
           							break;
           						case "TEST_ORIGINAL": // After testing original
                                   	progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 75%; font-size: 15px; line-height: 40px;" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100">Running Test Against first Mutant</div>';
                                	break;
                                // Not sure will ever get this one... since the test_mutant target execution might be written after testing mutants.
                                case "TEST_MUTANT":
                                   progressBar.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running Test Against more Mutants</div>';
                                	break;
           					}
                        });
                    }
              );
    };

    function progressBar() {
        // Create the Div to host events if that's not there
        if (document.getElementById("progress-bar") == null) {
            // Load the progress bar
            var progressBar = document.createElement('div');
            progressBar.setAttribute('class', 'progress');
            progressBar.setAttribute('id', 'progress-bar');
            progressBar.setAttribute('style', 'height: 40px; font-size: 30px');
            //
            progressBar.innerHTML = '<div class="progress-bar bg-danger" role="progressbar" style="width: 25%; font-size: 15px; line-height: 40px;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100">Validating and Compiling the Test</div>';
            var form = document.getElementById('logout');
            // Insert progress bar right under logout... this will conflicts with the other push-events
            form.parentNode.insertBefore(progressBar, form.nextSibling);
        }
        // Do a first request right away, such that compilation of this test is hopefully not yet started. This one will set the session...
        var updateURL = "<%= request.getContextPath()%>/notifications?type=<%=NotificationType.PROGRESSBAR%>&gameId=" + <%=gameId%> +"&isDefender=1";
        updateProgressBar(updateURL);

        // Register the requests to start in 1 sec
        var interval = 1000;
        setInterval(function () {
            updateProgressBar(updateURL);
        }, interval);
    }
              
</script>

<% request.getSession().removeAttribute("lastTest"); %>

