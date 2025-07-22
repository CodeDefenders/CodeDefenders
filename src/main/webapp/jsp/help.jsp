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
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>

<fmt:setBundle basename="org.codedefenders.i18n.Messages" var="bundle"/>

<c:set var="title" value="Help"/>

<p:main_page title="${title}">
    <div class="container" id="help-main-div">
        <h1>${i18n.tr("Help")}</h1>
        <p>${i18n.tr("Code Defenders pits two teams against each other on a Java class. Attackers must create mutants in the code, whilst Defenders write unit tests to catch (kill) these changes to the code.")}</p>
        <h2>${i18n.tr("Defenders")}</h2>
        <p>${i18n.tr("At the top of the page, there are two panels. On the left is the original Class Under Test (CUT), and on the right there is a panel to write a new test, along with a \"Defend!\" button which submits the test.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("images/help/defend_01.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("Below these panels are two more panels, which show existing mutants and tests. In the mutants panel, mutants can be claimed as being equivalent (which is covered at the end of this page). Existing mutants can also be seen by the icon present in the margin of the CUT panel.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("images/help/defend_02.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("We're going to submit a test to kill an alive mutant. Tests don't necessarily have to target alive mutants, and can simply be made to kill possible future mutants.")}</p>
        <div class="card p-4">
            <t:validator_explanation_test/>
        </div>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/defend_03.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("If an error is made in your test and it does not compile, a full compiler error is shown at the top of the screen. In this case, I did not add the brackets at the end of a function call.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/defend_04.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("After re-submitting the test, we see that it compiled successfully and killed the mutant.")}</p>
        <p>
            <img style="margin-top:16px" src="${url.forPath("/images/help/defend_05.png")}" class="img-responsive"/>
            <img style="margin-top:16px" src="${url.forPath("/images/help/defend_06.png")}" class="img-responsive"/>
        </p>
        <p>${i18n.tr("The actual changes made in killed mutants can be viewed by clicking the \"View Diff\" button on the killed tab of the mutants panel.")}</p>
        <p>
            <img style="margin-top:16px" src="${url.forPath("/images/help/defend_07.png")}" class="img-responsive"/>
            <!--<img style="margin-top:16px" src="${url.forPath("/images/help/defend_08.png")}" class="img-responsive"/>-->
        </p>

        <h2>${i18n.tr("Attackers")}</h2>
        <p>${i18n.tr("The attack page only has two panels, existing mutants, and a panel containing the CUT, which can be modified to create mutants. Green lines are covered by existing tests, with darker green showing more coverage.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/attack_01.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("Similarly to tests, mutants are limited by rules, which come in three strictness levels:")}</p>
        <div class="card p-4">
            <t:validator_explanation_mutant/>
        </div>
        <p>${i18n.tr("Here I have created a mutant by changing the > comparator to <. Return values, variables, etc. can also be changed.")}</p>
        <p>
            <img style="margin-top:16px" src="${url.forPath("/images/help/attack_02_originalsnippet.png")}" class="img-responsive"/>
            <img style="margin-top:16px" src="${url.forPath("/images/help/attack_02_mutated.png")}" class="img-responsive"/>
        </p>
        <p>${i18n.tr("After submitting our mutant by pressing the \"Attack!\" button, we see that it survived two existing tests. Similarly to submitting a test, an error will be displayed if the mutated class did not compile.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/attack_03.png")}" class="img-responsive"/></p>

        <h2>${i18n.tr("Equivalents")}</h2>

        <p>${i18n.tr("It is possible to create a mutant which is identical in functionality to the CUT, so no test can pass on the CUT and fail on the mutated class.")}</p>
        <p>${i18n.tr("For example, the following functions are identical in behaviour, they are equivalent:")}</p>
        <p>
            <img style="margin-top:16px" src="${url.forPath("/images/help/equiv_01_original.png")}" class="img-responsive"/>
            <img style="margin-top:16px" src="${url.forPath("/images/help/equiv_01_modified.png")}" class="img-responsive"/>
        </p>
        <p>${i18n.tr("If a Defender believes that an Attacker\'s mutant is equivalent, they can click the \"Claim Equivalent\" button on the mutant.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/equiv_03.png")}" class="img-responsive"/></p>
        <p>${i18n.tr("After this, the Attacker will see that their mutant was marked as equivalent. If the mutant is equivalent, they should accept it as equivalent.")}</p>
        <p>${i18n.tr("However, if the mutant isn't equivalent, the Attacker can prove that it isn't by writing a test which kills it.")}</p>
        <p><img style="margin-top:16px" src="${url.forPath("/images/help/equiv_04.png")}" class="img-responsive"/></p>

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
        <div style="width: 500px;">
            <table class="table table-striped table-condensed">
                <thead>
                <tr>
                    <th>${i18n.tr("Action")}</th>
                    <th>${i18n.tr("Key")}</th>
                    <th>${i18n.tr("Key (Mac)")}</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>${i18n.tr("Autocomplete")}</td>
                    <td>${i18n.tr("Ctrl + Space")}</td>
                    <td>${i18n.tr("Cmd + Space")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Search")}</td>
                    <td>${i18n.tr("Ctrl + F")}</td>
                    <td>${i18n.tr("Cmd + F")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Find Next")}</td>
                    <td>${i18n.tr("Ctrl + G")}</td>
                    <td>${i18n.tr("Cmd + G")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Find Previous")}</td>
                    <td>${i18n.tr("Ctrl + Shift + G")}</td>
                    <td>${i18n.tr("Cmd + Shift + G")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Search and Replace")}</td>
                    <td>${i18n.tr("Ctrl + Shift + F")}</td>
                    <td>${i18n.tr("Cmd + Shift + F")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Search and Replace All")}</td>
                    <td>${i18n.tr("Ctrl + Shift + R")}</td>
                    <td>${i18n.tr("Cmd + Shift + R")}</td>
                </tr>
                <tr>
                    <td>${i18n.tr("Jump to Line")}</td>
                    <td>${i18n.tr("Alt + R")}</td>
                    <td>${i18n.tr("Alt + R")}</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</p:main_page>
