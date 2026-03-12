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

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<div class="d-flex mb-3">
    <h2 class="w-50 text-center fg-attacker">${i18n.tr('Attackers')}</h2>
    <h2 class="w-50 text-center fg-defender">${i18n.tr('Defenders')}</h2>
</div>

<h4 class="text-center">${i18n.tr('Get one point for:')}</h4>

<div class="row g-3 mb-3">
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Creating a mutant that survives one test')}</li>
                    <li>${i18n.tr('Every additional test that executes the mutant and does not kill it')}</li>
                </ul>
            </div>
        </div>
    </div>
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Each mutant your test kills')}</li>
                    <li>${i18n.tr('Each point that mutant had accumulated')}</li>
                    <li>${i18n.tr('Each previously untouched mutant your test kills')}</li>
                </ul>
            </div>
        </div>
    </div>
</div>

<h4 class="text-center">${i18n.tr('Equivalence Duels:')}</h4>

<h5 class="text-center">${i18n.tr('Mutant is proven to <b>not</b> be equivalent:')}</h5>

<div class="row g-3 mb-3">
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Keep the mutant\\\'s points and get an extra point')}</li>
                </ul>
            </div>
        </div>
    </div>
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Your score does not change')}</li>
                </ul>
            </div>
        </div>
    </div>
</div>

<h5 class="text-center">${i18n.tr('Equivalence is accepted or no killing test is provided in time:')}</h5>

<div class="row g-3">
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Lose the mutant\\\'s points')}</li>
                </ul>
            </div>
        </div>
    </div>
    <div class="col-6">
        <div class="card">
            <div class="card-body">
                <ul class="m-0 ps-3">
                    <li>${i18n.tr('Gain one additional point')}</li>
                </ul>
            </div>
        </div>
    </div>
</div>
