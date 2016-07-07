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
        $(allLines[lines[l][0]]).css('background-color', 'rgba('+color.r+','+color.g+','+color.b+','+lines[l][1]+')');
        //$(allLines[lines[l][0]]).css('opacity', lines[l][1]);
    }
};

var COVERED_COLOR = [];
COVERED_COLOR.r = 153;
COVERED_COLOR.g = 255;
COVERED_COLOR.b = 153;
var UNCOVERED_COLOR = [];
UNCOVERED_COLOR.r = 255;
UNCOVERED_COLOR.g = 153;
UNCOVERED_COLOR.b = 153;

lineContent = [];

timeoutFunction = null;

var mutantLine = function (lineQuant, superDiv, defender){
    if (!superDiv){
        superDiv = "#cut-div";
    }
    var allLines = [];
    $(superDiv + ' .CodeMirror-linenumber').each(function(i, e){
        var line = parseInt(e.innerHTML);
        allLines[line] = $(e).parent("div").parent("div")[0];
    });
    for (var l in lineQuant){
        const lineNum = lineQuant[l][0];
        const quant = lineQuant[l][1];
        const id = "line" + lineQuant[l][0];
        $(allLines[lineQuant[l][0]]).before('<div id="'+id+'" style="width: 20px; height: 20px; margin-left: 5px; float: left; margin-right: -25px; position: relative; z-index:100000;"><span>' + lineQuant[l][1] + ' x </span><img src="images/mutant.png" alt="' + lineQuant[l][1] + ' mutants on line ' + lineQuant[l][0] + '" width="20" /></div>');
        const divId = "#" + id;
        var mol = "";

        for (var ml in lineQuant[l][2]){
            mol = mol + lineQuant[l][2][ml] + ",";
        }

        const mutantsOnLine = mol;

        var content = '<span style="background-color: #f00; color: #fff;" id="mutationPopup"> ' + quant + ' mutants on line ' + lineNum + ' (Mutants: ' + mutantsOnLine + ')';
        if (defender) {
            content += '<a href="multiplayer/play?equivLine=' + lineQuant[l][0] + '"> Mark Line Equivalent </a>';
        }
        content += '</span>'

        lineContent[lineNum] = content;

        $(divId).hover(
            function() {
                clearTimeout(timeoutFunction);
                drawMutants(lineNum, this);
            }, function() {
                timeoutFunction = setTimeout(function(){$('#mutationPopup').fadeOut(500);}, 5000);
            }
        );
    }
};

var drawMutants = function(lineNum, ele){
    if (!$.contains(ele, $('#mutationPopup')[0])) {
        $('#mutationPopup').remove();
        var content = lineContent[lineNum];
        $(ele).append(content);
    }
}