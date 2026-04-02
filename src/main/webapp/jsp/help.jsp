<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<c:set var="title" value="${i18n.tr('Help')}"/>

<p:main_page title="${title}">
    <div class="container" id="help-main-div">
        <h1>${title}</h1>

        <p>${i18n.tr("Code Defenders pits two teams against each other on a Java class. Attackers must create mutants in the code, whilst Defenders write unit tests to catch (kill) these changes to the code.")}</p>

        <h2>${i18n.tr("Main page")}</h2>
        <p>${i18n.tr("Here you can join open games. You can also create your own games!")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/games_overview.png", i18n)}"
                 alt="${i18n.tr("Games overview page showing open and available games.")}"
                 class="img-responsive border p-2"/>
        </p>

        <h2>${i18n.tr("Defenders")}</h2>
        <p>${i18n.tr("At the top of the page, there are two panels. On the left is the original Class Under Test (CUT), and on the right there is a panel to write a new test, along with a \"Defend\" button which submits the test.")}<br>${i18n.tr("On the CUT panel, we can see the coverage of existing tests (in green) and the location of existing mutants (the symbols at the side).")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("images/help/defender_overview.png", i18n)}"
                 alt="${i18n.tr("Defender view with the Class Under Test on the left and the test editor on the right.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("We're going to submit a test to kill a living mutant. Tests don't necessarily have to target alive mutants, and can simply be made to kill possible future mutants.")}</p>
        <p>${i18n.tr("As we can see above, there is a living mutant in the <code>getCurrentFloor()</code> method, and there is no test yet to cover it.")}</p>

        <t:validator_explanation_test/>

        <p style="margin-top:16px">
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/defender_broken.png", i18n)}"
                 alt="${i18n.tr("Defender test submission showing a failing or broken test example.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("If an error is made in your test and it does not compile, a full compiler error is shown at the top of the screen. In this case, I did not add the brackets at the end of a method call.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/defender_error.png", i18n)}"
                 alt="${i18n.tr("Compiler error displayed after submitting a test that does not compile.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("After re-submitting the test, we see that it compiled successfully and killed the mutant.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/defender_fix.png", i18n)}"
                 alt="${i18n.tr("Successful test submission showing the mutant was killed.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("Below these panels are two more panels, which show existing mutants and tests.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/defender_bottom.png", i18n)}"
                 alt="${i18n.tr("Lower section of the defender page showing the mutants and tests panels.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("The actual changes made in killed mutants can be viewed by clicking the \"View Diff\" button on the killed tab of the mutants panel.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/defender_diff.png", i18n)}"
                 alt="${i18n.tr("Mutant diff view displayed after clicking the \"View Diff\" button.")}"
                 class="img-responsive border p-2"/>
            <!--<img style="margin-bottom:16px" src="${url.forPathLocalized("/images/help/defend_08.png", i18n)}" class="img-responsive border p-2"/>-->
        </p>

        <h2>${i18n.tr("Attackers")}</h2>
        <p>${i18n.tr("The attack page only has two panels, existing mutants, and a panel containing the CUT, which can be modified to create mutants. Green lines are covered by existing tests, with darker green showing more coverage.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/attacker_overview.png", i18n)}"
                 alt="${i18n.tr("Attacker view showing existing mutants and the editable Class Under Test.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("Similarly to tests, mutants are limited by rules, which come in three strictness levels:")}</p>
        <div style="margin-bottom:16px">
            <b>${i18n.tr("Relaxed")}</b> <br>
            <ul>
                <li>${i18n.tr("No calls to System.* or Random.*")}</li>
                <li>${i18n.tr("No mutants with only changes to comments or formatting")}</li>
                <li>${i18n.tr("No renaming of methods or fields, and no additional methods or fields")}</li>
            </ul>

            <b>${i18n.tr("Moderate")}</b> <br>
            <ul>
                <li>${i18n.tr("No changes to comments")}</li>
                <li>${i18n.tr("No additional logical operators (&&, ||)")}</li>
                <li>${i18n.tr("No ternary operators")}</li>
                <li>${i18n.tr("No new control structures (switch, if, for, ...)")}</li>
            </ul>

            <b>${i18n.tr("Strict")}</b> <br>
            <ul class="mb-0">
                <li>${i18n.tr("No reflection")}</li>
                <li>${i18n.tr("No bitwise operators (bitshifts and logical)")}</li>
                <li>${i18n.tr("No signature changes")}</li>
            </ul>
        </div>

        <p>
            ${i18n.tr("Here I have created a mutant by changing the > comparator to <. Return values, variables, etc. can also be changed.")}
        </p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/attacker_snippet_original.png", i18n)}"
                 alt="${i18n.tr("Original code snippet before creating a mutant.")}"
                 class="img-responsive border p-2"/>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/attacker_snippet_mutated.png", i18n)}"
                 alt="${i18n.tr("Mutated code snippet after changing the comparator.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("After submitting our mutant by pressing the \"Attack!\" button, we see that it survived two existing tests. Similarly to submitting a test, an error will be displayed if the mutated class did not compile.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/attacker_success.png", i18n)}"
                 alt="${i18n.tr("Attack result showing that the mutant survived existing tests.")}"
                 class="img-responsive border p-2"/>
        </p>

        <h2>${i18n.tr("Equivalents")}</h2>
        <p>${i18n.tr("It is possible to create a mutant which is identical in functionality to the CUT, so no test can pass on the CUT and fail on the mutated class.")}</p>
        <p>${i18n.tr("For example, the following functions are identical in behaviour, they are equivalent:")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/equivalence_original.png", i18n)}"
                 alt="${i18n.tr("Original function used in an equivalence example.")}"
                 class="img-responsive border p-2"/>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/equivalence_mutated.png", i18n)}"
                 alt="${i18n.tr("Mutated function that is equivalent to the original example.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr('If a Defender believes that an Attacker\'s mutant is equivalent, they can click the "Claim Equivalent" button on the mutant.')}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/equivalence_claim.png", i18n)}"
                 alt="${i18n.tr("Mutant panel showing the \"Claim Equivalent\" action.")}"
                 class="img-responsive border p-2"/>
        </p>

        <p>${i18n.tr("After this, the Attacker will see that their mutant was marked as equivalent. If the mutant is equivalent, they should accept it as equivalent.")}</p>
        <p>${i18n.tr("However, if the mutant isn't equivalent, the Attacker can prove that it isn't by writing a test which kills it.")}</p>
        <p>
            <img style="margin-bottom:16px"
                 src="${url.forPathLocalized("/images/help/equivalence_duel.png", i18n)}"
                 alt="${i18n.tr("Attacker view showing an equivalent marking and the option to respond.")}"
                 class="img-responsive border p-2"/>
        </p>

        <h2>${i18n.tr("Scoring System")}</h2>

        <h3>${i18n.tr("Mutants")}</h3>
        <p>${i18n.tr("For every test a mutant passes where the test executes the mutated line, the mutant gains a point.")}</p>
        <p>${i18n.tr("Mutants also gain one point for being created and surviving.")}</p>

        <h3>${i18n.tr("Tests")}</h3>
        <p>${i18n.tr("For every mutant a test kills, the test gains points equal to the score of the mutant.")}</p>
        <p>${i18n.tr("A test gains a point for killing a previously untouched mutant.")}</p>
        <p>${i18n.tr("A test is also given an additional point for killing a newly created mutant.")}</p>

        <h3>${i18n.tr("Equivalences")}</h3>
        <p>${i18n.tr("If an Attacker proves a mutant is not equivalent, they keep the mutant\'s points and gain an additional point.")}</p>
        <p>${i18n.tr("If the Attacker accepts the mutant is equivalent, or the game expires, they lose the points of that mutant, and the Defender who claimed that it is equivalent gains a point.")}</p>

        <h2>${i18n.tr("Code Editor Keyboard Shortcuts")}</h2>
        <p>${i18n.tr('Keyboard shortcuts can be configured by clicking on the "editor mode" button at the top of the page.')}</p>
    </div>
</p:main_page>
