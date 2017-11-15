var highlightLine = function (lines, color, superDiv) {
    if (!superDiv) {
        superDiv = "#cut-div";
    }
    var allLines = [];
    $(superDiv + ' .CodeMirror-linenumber').each(function (i, e) {
        var line = parseInt(e.innerHTML);
        allLines[line] = $(e).parent("div").parent("div")[0];
    });
    for (var l in lines) {
        $(allLines[lines[l][0]]).css('background-color', 'rgba(' + color.r + ',' + color.g + ',' + color.b + ',' + lines[l][1] + ')');
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

var mutantLine = function (lineQuant, superDiv) {
    if (!superDiv) {
        superDiv = "#cut-div";
    }
    var allLines = [];
    $(superDiv + ' .CodeMirror-linenumber').each(function (i, e) {
        var line = parseInt(e.innerHTML);
        allLines[line] = $(e).parent("div").parent("div")[0];
    });

    $(".codedef-line-mutant").remove();

    for (var l in lineQuant) {
        const lineNum = lineQuant[l][0];
        const quant = lineQuant[l][1];
        const id = "line" + lineQuant[l][0];
        $(allLines[lineQuant[l][0]]).before('<div id="' + id + '" style="width: 20px; height: 20px; margin-left: 5px; float: left; margin-right: -25px; position: relative; z-index:2000;" class="codedef-line-mutant"><img src="images/mutant.png" alt="' + lineQuant[l][1] + ' mutants on line ' + lineQuant[l][0] + '" width="20" /></div>');
        const divId = "#" + id;
        var mol = "";

        for (var ml in lineQuant[l][2]) {
            mol = mol + lineQuant[l][2][ml] + ",";
        }

        mol = mol.substr(0, mol.length - 1);

        const mutantsOnLine = mol;

        var content = '<span style="background-color: #f00; color: #fff; padding-left: 25px; position: absolute;" id="mutationPopup"> ' + quant + ' mutants on line ' + lineNum + ' (Mutants: ' + mutantsOnLine + ')';
        // Do we have access to line differences (not if defender)
        if (lineQuant.difference &&
            lineQuant.difference.deltas &&
            lineQuant[l].difference.deltas.length > 0) {
            content += '<a href="multiplayer/play?equivLine=' + lineQuant[l][0] + '" style="color: #FEFCFC"> Mark Line Equivalent </a>';
            content += '</span>';
        }

        lineContent[lineNum] = content;

        $(divId).hover(
            function () {
                clearTimeout(timeoutFunction);
                drawMutants(lineNum, this);
            }, function () {
                timeoutFunction = setTimeout(function () {
                    $('#mutationPopup').fadeOut(50);
                }, 500);
            }
        );
    }
};

var drawMutants = function (lineNum, ele) {
    if (!$.contains(ele, $('#mutationPopup')[0])) {
        $('#mutationPopup').remove();
        var content = lineContent[lineNum];
        $(ele).append(content);
    }
};

killedLineContent = [];

var mutantKilledLine = function (lineQuant, superDiv) {
    if (!superDiv) {
        superDiv = "#cut-div";
    }
    var allLines = [];
    $(superDiv + ' .CodeMirror-linenumber').each(function (i, e) {
        var line = parseInt(e.innerHTML);
        allLines[line] = $(e).parent("div").parent("div")[0];
    });
    for (var l in lineQuant) {
        const lineNum = lineQuant[l][0];
        const quant = lineQuant[l][1];
        const id = "kline" + lineQuant[l][0];
        $(allLines[lineQuant[l][0]]).before('<div id="' + id + '" style="width: 20px; height: 20px; margin-left: 25px; float: left; margin-right: -25px; position: relative; z-index:2000;"><img src="images/mutantKilled.png" alt="' + lineQuant[l][1] + ' mutants on line ' + lineQuant[l][0] + '" width="20" /></div>');
        const divId = "#" + id;
        var mol = "";

        for (var ml in lineQuant[l][2]) {
            mol = mol + lineQuant[l][2][ml] + ",";
        }

        const mutantsOnLine = mol;

        var content = '<span style="background-color: #090; color: #fff; position: absolute;" id="mutationPopup"> ' + quant + ' killed mutants on line ' + lineNum;
        content += '</span>'

        killedLineContent[lineNum] = content;

        $(divId).hover(
            function () {
                clearTimeout(timeoutFunction);
                drawKilledMutants(lineNum, this);
            }, function () {
                timeoutFunction = setTimeout(function () {
                    $('#mutationPopup').fadeOut(50);
                }, 500);
            }
        );
    }
};

var drawKilledMutants = function (lineNum, ele) {
    if (!$.contains(ele, $('#mutationPopup')[0])) {
        $('#mutationPopup').remove();
        var content = killedLineContent[lineNum];
        $(ele).append(content);
    }
};