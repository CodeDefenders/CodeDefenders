<% String pageTitle = "Help"; %>

<%@ include file="/jsp/header_base.jsp" %>

<div style="padding: 0px 16px 0px 16px">
	<h1>Help</h1>
	<p style="margin-left: 16px">Code Defenders pits two teams against each other on a Java class. Attackers must create mutants in the code, whilst Defenders write unit tests to catch (kill) these changes to the code.</p>
	<div style="padding: 0px 16px 0px 16px" class="help-main-div">
		<h2>Defenders</h2>
		<p>At the top of the page, there are two panels. On the left is the original Class Under Test (CUT), and on the right there is a panel to write a new test, along with a &#34;Defend!&#34; button which submits the test.</p>
		<p><img style="margin-top:16px" src="images/help/defend_01.png" class="img-responsive"/></p>
		<p>Below these panels are two more panels, which show existing mutants and tests. In the mutants panel, mutants can be claimed as being equivalent (which is covered at the end of this page). Existing mutants can also be seen by the icon present in the margin of the CUT panel.</p>
		<p><img style="margin-top:16px" src="images/help/defend_02.png" class="img-responsive"/></p>
		<p>We&#39;re going to submit a test to kill an alive mutant. Tests don&#39;t necessarily have to target alive mutants, and can simply be made to kill possible future mutants.</p>
		<p>Tests are limited by the following rules:</p>
		<ul>
			<li>They cannot contain loops.</li>
			<li>They cannot make calls to System.*</li>
			<li>They cannot contain new methods or conditionals.</li>
			<li>They can only make two assertions each.</li>
		</ul>
		<p><img style="margin-top:16px" src="images/help/defend_03.png" class="img-responsive"/></p>
		<p>If an error is made in your test and it does not compile, a full compiler error is shown at the top of the screen. In this case, I did not add the brackets at the end of a function call.</p>
		<p><img style="margin-top:16px" src="images/help/defend_04.png" class="img-responsive"/></p>
		<p>After re-submitting the test, we see that it compiled successfully and killed the mutant.</p>
		<p>
			<img style="margin-top:16px" src="images/help/defend_05.png" class="img-responsive"/>
			<img style="margin-top:16px" src="images/help/defend_06.png" class="img-responsive"/>
		</p>
		<p>The actual changes made in killed mutants can be viewed by clicking the &#34;View Diff&#34; button on the killed tab of the mutants panel.</p>
		<p>
			<img style="margin-top:16px" src="images/help/defend_07.png" class="img-responsive"/>
			<!--<img style="margin-top:16px" src="images/help/defend_08.png" class="img-responsive"/>-->
		</p>

		<h2>Attackers</h2>
		<p>The attack page only has two panels, existing mutants, and a panel containing the CUT, which can be modified to create mutants. Green lines are covered by existing tests, with darker green showing more coverage.</p>
		<p><img style="margin-top:16px" src="images/help/attack_01.png" class="img-responsive"/></p>
		<p>Similarly to tests, mutants are limited by rules, which come in three strictness levels:</p>
		<div class = "panel" style="margin: 30px"><%@ include file="/jsp/validator_explanation.jsp" %></div>
		<p>Here I have created a mutant by changing the > comparator to <. Return values, variables, etc. can also be changed.</p>
		<p>
			<img style="margin-top:16px" src="images/help/attack_02_originalsnippet.png" class="img-responsive"/>
			<img style="margin-top:16px" src="images/help/attack_02_mutated.png" class="img-responsive"/>
		</p>
		<p>After submitting our mutant by pressing the &#34;Attack!&#34; button, we see that it survived two existing tests. Similarly to submitting a test, an error will be displayed if the mutated class did not compile.</p>
		<p><img style="margin-top:16px" src="images/help/attack_03.png" class="img-responsive"/></p>

		<h2>Equivalents</h2>

		<p>It is possible to create a mutant which is identical in functionality to the CUT, so no test can pass on the CUT and fail on the mutated class.</p>
		<p>For example, the following functions are identical in behaviour, they are equivalent:</p>
		<p>
			<img style="margin-top:16px" src="images/help/equiv_01_original.png" class="img-responsive"/>
			<img style="margin-top:16px" src="images/help/equiv_01_modified.png" class="img-responsive"/>
		</p>
		<p>If a Defender believes that an Attacker&#39;s mutant is equivalent, they can click the &#34;Claim Equivalent&#34; button on the mutant.</p>
		<p><img style="margin-top:16px" src="images/help/equiv_03.png" class="img-responsive"/></p>
		<p>After this, the Attacker will see that their mutant was marked as equivalent. If the mutant is equivalent, they should accept it as equivalent.</p>
		<p>However, if the mutant isn&#39;t equivalent, the Attacker can prove that it isn&#39;t by writing a test which kills it.</p>
		<p><img style="margin-top:16px" src="images/help/equiv_04.png" class="img-responsive"/></p>

		<h2>Scoring System</h2>
		<h3>Mutants</h3>
		<p>For every test a mutant passes where the test executes the mutated line, the mutant gains a point.</p>
		<p>Mutants also gain one point for being created and surviving.</p>
		<h3>Tests</h3>
		<p>For every mutant a test kills, the test gains points equal to the score of the mutant.</p>
		<p>A test gains a point for killing a previously untouched mutant.</p>
		<p>A test is also given an additional point for killing a newly created mutant.</p>
		<h3>Equivalences</h3>
		<p>If an Attacker proves a mutant is not equivalent, they keep the mutant&#39;s points and gain an additional point.</p>
		<p>If the Attacker accepts the mutant is equivalent, or the game expires, they lose the points of that mutant, and the Defender who claimed that it is equivalent gains a point.</p>

		<h2>Code Editor</h2>
		<p>The code editor has functionality to search the code, and to move to a line, implemented using CodeMirror.</p>
		<p>These are the keyboard shortcuts for particular functions:</p>
		<ul>
			<li>Start Searching - Ctrl+F / Cmd+F</li>
			<li>Find Next - Ctrl+G / Cmd+G</li>
			<li>Find Previous - Shift+Ctrl+G / Shift+Cmd+G</li>
			<li>Replace - Shift+Ctrl+F / Cmd+Option+F</li>
			<li>Replace All - Shift+Ctrl+R / Shift+Cmd+Option+F</li>
			<li>Persistent Search (dialog doesn't close, Enter to find next, Shift+Enter to find prev.) - Alt+F</li>
			<li>Jump to Line - Alt+G</li>
		</ul>
	</div>
</div>

<%@ include file="/jsp/footer.jsp" %>