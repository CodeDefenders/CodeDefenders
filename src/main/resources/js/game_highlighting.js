var highlightLine = function (lines, color, superDiv){
    if (!superDiv){
        superDiv = "#cut-div";
    }
    var allLines = [];
    $(superDiv + ' .CodeMirror-linenumber').each(function(i, e){
        var line = parseInt(e.innerHTML);
        allLines[line] = $(e).parent("div").parent("div")[0];
    });
    for (var l in lines){
        $(allLines[lines[l]]).css('background-color', color);
    }
};

var COVERED_COLOR = "#99FF99";
var UNCOVERED_COLOR = "#FF9999";