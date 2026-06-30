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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="mutantAccordion" type="org.codedefenders.beans.game.MutantAccordionBean"--%>

<link href="${url.forPath("/css/specific/mutant_accordion.css")}" rel="stylesheet">

<div id="mutants-div">

    <div class="game-component-header">
        <h3>${i18n.tr('Existing Mutants')}</h3>
        <div id="ma-filter">
            <input type="radio" class="btn-check" name="filter" id="all" value="ALL" checked autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="all">
                <span class="align-middle">${i18n.tr('All')}</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="alive" value="ALIVE" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="alive">
                <span class="mutantCUTImage mutantImageAlive align-middle"></span>
                <span class="align-middle">${i18n.tr('Alive')}</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="killed" value="KILLED" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="killed">
                <span class="mutantCUTImage mutantImageKilled align-middle"></span>
                <span class="align-middle">${i18n.tr('Killed')}</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="marked" value="FLAGGED" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="marked">
                <span class="mutantCUTImage mutantImageFlagged align-middle"></span>
                <span class="align-middle">${i18n.tr('Claimed Equivalent')}</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="equivalent" value="EQUIVALENT" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="equivalent">
                <span class="mutantCUTImage mutantImageEquiv align-middle"></span>
                <span class="align-middle">${i18n.tr('Equivalent')}</span>
            </label>
        </div>
    </div>

    <div class="accordion loading loading-border-card loading-bg-gray" id="mutants-accordion"></div>

    <script type="module">
        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
        import {MutantAccordion} from '${url.forPath("/js/codedefenders_game.mjs")}';

        const categories = JSON.parse('${mutantAccordion.jsonFromCategories()}');
        const mutants = new Map(JSON.parse('${mutantAccordion.jsonMutants()}'));
        const gameId = ${mutantAccordion.gameId};

        const mutantAccordion = new MutantAccordion(categories, mutants, gameId);

        objects.register('mutantAccordion', mutantAccordion);
    </script>
</div>
