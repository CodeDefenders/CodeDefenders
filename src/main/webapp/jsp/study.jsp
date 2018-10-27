<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<% String pageTitle = "Study 2016"; %>

<%@ include file="/jsp/header_logout.jsp" %>
<div class="container" style="margin: 0 auto; text-align: center;">
    <h1>Code Defenders - Study 2016</h1>
    <h3>Contents</h3>
    <ul style="margin: 0 auto; width: auto; text-align: center; list-style: none; margin: 0px; padding: 0px;">
        <li><a href="study#intro">Introduction</a></li>
        <li><a href="study#survey">Survey Results</a></li>
        <li><a href="study#classes">Selected Classes</a></li>
        <li><a href="study#output">Generated Tests and Mutants</a></li>
        <li><a href="study#scoring">Multiplayer Scoring System</a></li>
    </ul>
    <h3 id="intro">Introduction</h3>
    <p>This page provides additional information on Code Defenders experiments ran in 2016. </p>
    <h3 id="survey">Survey Results</h3>
    <p>Below are the results for both surveys in CSV format.</p>
    <ul style="margin: 0 auto; width: auto; text-align: center; list-style: none; margin: 0px; padding: 0px;">
        <li><a href="data/duel-responses.csv">Suitability Survey Results</a></li>
        <li><a href="data/bg-responses.csv">Crowdsourcing Survey Results</a></li>
    </ul>


    <h3 id="classes">Class Selection</h3>
    <p>There were 20 classes selected </p>
    <table style="border: 1px solid #000;margin: 0 auto;">
        <tr><td>Game ID</td><td>Class Name</td><td>Source Project</td><td>NCSS*</td><td>Major Mutants</td></tr>
        <tr><td>1001</td><td>ByteArrayHashMap</td><td><a href="https://sourceforge.net/projects/azureus/">sf-vuze</a></td><td>179</td><td>17</td></tr><tr>
        <td>1002</td><td>ByteVector</td><td><a href="http://jiprof.sourceforge.net/">sf-jiprof</a></td><td>128</td><td>31</td></tr><tr>
        <td>1003</td><td>ChunkedLongArray</td><td><a href="https://sourceforge.net/projects/summa/">sf-summa</a></td><td>102</td><td>23</td></tr><tr>
        <td>1004</td><td>FTPFile</td><td><a href="https://commons.apache.org/proper/commons-net/">ac-net</a></td><td>158</td><td>11</td></tr><tr>
        <td>1005</td><td>FontInfo</td><td><a href="http://squirrel-sql.sourceforge.net/">sf-squirrel-sql</a></td><td>104</td><td>6</td></tr><tr>
        <td>1006</td><td>HierarchyPropertyParser</td><td><a href="https://sourceforge.net/projects/weka/">sf-weka</a></td><td>261</td><td>28</td></tr><tr>
        <td>1007</td><td>HSLColor</td><td><a href="https://sourceforge.net/projects/azureus/">sf-vuze</a></td><td>160</td><td>65</td></tr><tr>
        <td>1008</td><td>ImprovedStreamTokenizer</td><td><a href="http://caloriecount.sourceforge.net/">sf-caloriecount</a></td><td>128</td><td>11</td></tr><tr>
        <td>1009</td><td>ImprovedTokenizer</td><td><a href="http://caloriecount.sourceforge.net/">sf-caloriecount</a></td><td>163</td><td>7</td></tr><tr>
        <td>1010</td><td>Inflection</td><td><a href="http://schemaspy.sourceforge.net/">sf-schemaspy</a></td><td>112</td><td>10</td></tr><tr>
        <td>1011</td><td>IntHashMap</td><td><a href="https://sourceforge.net/projects/azureus/">sf-vuze</a></td><td>113</td><td>14</td></tr><tr>
        <td>1012</td><td>ParameterParser</td><td><a href="https://commons.apache.org/proper/commons-fileupload/">ac-fileupload</a></td><td>108</td><td>17</td></tr><tr>
        <td>1013</td><td>Range</td><td><a href="https://commons.apache.org/proper/commons-lang/">ac-lang3</a></td><td>128</td><td>15</td></tr><tr>
        <td>1014</td><td>RationalNumber</td><td><a href="https://commons.apache.org/proper/commons-imaging/">ac-imaging</a></td><td>108</td><td>28</td></tr><tr>
        <td>1015</td><td>SubjectParser</td><td><a href="http://newzgrabber.sourceforge.net/">sf-newzgrabber</a></td><td>117</td><td>13</td></tr><tr>
        <td>1016</td><td>TimeStamp</td><td><a href="https://commons.apache.org/proper/commons-net/">ac-net</a></td><td>103</td><td>20</td></tr><tr>
        <td>1017</td><td>VCardBean</td><td><a href="https://sourceforge.net/projects/heal/">sf-heal</a></td><td>188</td><td>18</td></tr><tr>
        <td>1018</td><td>WeakHashtable</td><td><a href="https://commons.apache.org/proper/commons-logging/">ac-logging</a></td><td>168</td><td>9</td></tr><tr>
        <td>1019</td><td>XmlElement</td><td><a href="https://sourceforge.net/projects/inspirento/">sf-inspirento</a></td><td>196</td><td>16</td></tr><tr>
        <td>1020</td><td>XMLParser</td><td><a href="https://sourceforge.net/projects/fim1/">sf-fim1</a></td><td>162</td><td>76</td></tr>
    </table>
    <span style="font-size: 0.9em; font-style: italic;">* NCSS - None Comment Source Statements</span>
    <p>To see the refactored versions of these classes, <a href="data/minimised_classes_2016.zip">download the zip</a></p>
    <h3 id="output">Generated Tests and Mutants</h3>
    <p>To download the tests suites generated from these classes, <a href="data/generated_tests_2016.zip">click here</a></p>
    <p>To download the mutants generated from these classes, <a href="data/generated_mutants_2016.zip">click here</a></p>
    <h3 id="scoring">Multiplayer Scoring System Example</h3>
    <table style="border: 1px solid #000;margin: 0 auto;">
        <tr>
            <td colspan="2">Alice (attacker)</td>
            <td colspan="2">Andy
                (attacker)
            </td>
            <td colspan="2">Dan (defender)</td>
            <td colspan="2">David
                (defender)
            </td>
            <td>Explanation</td>
        </tr>
        <tr style="border-bottom: 4px double #000;">
            <td>
                Action
            </td>
            <td> Points</td>
            <td> Action</td>
            <td> Points</td>
            <td> Action</td>
            <td> Points</td>
            <td> Action</td>
            <td> Points
            </td><td></td>
        </tr>
        <tr>
            <td>
                <span style="font-style:italic">m<sub>1</sub></span>
            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Alice submits  <span style="font-style:italic">m<sub>1</sub></span>.</td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td>
            </td>
            <td> 1</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> No tests exist,  <span style="font-style:italic">m<sub>1</sub></span> gains a point.
            </td>
        </tr>
        <tr>
            <td>
            </td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>2</sub></span></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Andy submits  <span style="font-style:italic">m<sub>2</sub></span>.</td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td></td>
            <td></td>
            <td></td>
            <td> 1</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> No tests exist,  <span style="font-style:italic">m<sub>2</sub></span> gains a point.</td>
        </tr>
        <tr>
            <td>

            </td>
            <td></td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">t<sub>1</sub></span></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Dan submits  <span style="font-style:italic">t<sub>1</sub></span></td>
        </tr>
        <tr  style="border-bottom: 1px dashed #000">
            <td></td>
            <td>2</td>
            <td> <span style="font-style:italic; color: red;">m<sub>2</sub></span></td>
            <td> 1</td>
            <td></td>
            <td> 2</td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>1</sub></span> survives  <span style="font-style:italic">t<sub>1</sub></span>, gaining a
                point.  <span style="font-style:italic">m<sub>2</sub></span> killed
                by  <span style="font-style:italic">t<sub>1</sub></span>
            </td>
        </tr>
        <tr>
            <td>


            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">t<sub>2</sub></span></td>
            <td></td>
            <td> David submits  <span style="font-style:italic">t<sub>2</sub></span></td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td> <span style="font-style:italic;  color: red;">m<sub>1</sub></span></td>
            <td> 2</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> 3</td>
            <td>  <span style="font-style:italic">m<sub>1</sub></span> killed by  <span style="font-style:italic">t<sub>2</sub></span></td>
        </tr>
        <tr>
            <td>


                <span style="font-style:italic">m<sub>3</sub></span>
            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Alice submits  <span style="font-style:italic">m<sub>3</sub></span></td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td> <span style="font-style:italic; color: red;">m<sub>3</sub></span></td>
            <td> 2</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> 4</td>
            <td>  <span style="font-style:italic">m<sub>3</sub></span> survives  <span style="font-style:italic">t<sub>1</sub></span>, but is killed
                by
                <span style="font-style:italic">t<sub>2</sub></span>
            </td>
        </tr>
        <tr>
            <td>


            </td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>4</sub></span></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Andy submits  <span style="font-style:italic">m<sub>4</sub></span></td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td>
            </td>
            <td></td>
            <td></td>
            <td> 4</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>4</sub></span> survives all current tests</td>
        </tr>
        <tr>
            <td>


                <span style="font-style:italic">m<sub>5</sub></span>
            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Alice submits  <span style="font-style:italic">m<sub>5</sub></span></td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td>
            </td>
            <td> 5</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>5</sub></span> survives all current tests</td>
        </tr>
        <tr>
            <td>

            </td>
            <td></td>
            <td></td>
            <td></td>
            <td> eq(<span style="font-style:italic">m<sub>5</sub></span>)</td>
            <td></td>
            <td></td>
            <td></td>
            <td> Dan marks  <span style="font-style:italic">m<sub>5</sub></span> as equivalent</td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td> <span style="font-style:italic;  color: red;">m<sub>5</sub></span></td>
            <td> 2</td>
            <td></td>
            <td></td>
            <td></td>
            <td> 3</td>
            <td></td>
            <td></td>
            <td> Alice accepts  <span style="font-style:italic">m<sub>5</sub></span> as
                equivalent, losing its points. Dan gains a point.
            </td>
        </tr>
        <tr>
            <td>

            </td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>6</sub></span></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Andy submits  <span style="font-style:italic">m<sub>6</sub></span></td>
        </tr>
        <tr style="border-bottom: 1px dashed #000">
            <td>
            </td>
            <td></td>
            <td></td>
            <td> 7</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td>  <span style="font-style:italic">m<sub>6</sub></span> survives all current tests</td>
        </tr>
        <tr>
            <td>

            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> eq(<span style="font-style:italic">m<sub>6</sub></span>)</td>
            <td></td>
            <td> David marks  <span style="font-style:italic">m<sub>6</sub></span> as equivalent</td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td> <span style="font-style:italic;  color: red;">m<sub>6</sub></span></td>
            <td> 7</td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td> Andy submits a killing test,
                proving  <span style="font-style:italic">m<sub>6</sub></span>
                is not equivalent
            </td>
        </tr>
        <tr style="border-top: 4px double #000">
            <td>

            </td>
            <td> 2</td>
            <td>  </td>
            <td> 7</td>
            <td>  </td>
            <td> 3</td>
            <td>  </td>
            <td> 4</td>
            <td> Game ends, Attackers win <span style="font-weight: bold;">9</span> : 7
            </td>
        </tr>
    </table>
    <span style="font-size: 0.9em; font-style: italic;">All tests execute all mutated lines
    in this example. Points are cumulative for respective player</span>

</div>

<%@ include file="footer_logout.jsp" %>