/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
import {objects} from '../main';


/* There is honestly not that much reason for this to be a class, other than consistency. */
class AttackerIntentionCollection {

    constructor () {
        /**
         * The dropdown containing the attacker intentions.
         * @type {HTMLElement}
         */
        this._dropdown = document.getElementById('attacker-intention-dropdown');

        /**
         * The "Attack" button normally used to submit mutants.
         * @type {HTMLElement}
         */
        this._attackButton = document.getElementById('submitMutant');

        /**
         * The form to submit for attacking.
         * @type {HTMLFormElement}
         */
        this._attackForm = document.forms['atk'];

        /**
         * The (hidden) input to submit the intention with.
         * @type {HTMLInputElement}
         */
        this._intentionInput = this._attackForm.querySelector('input[name="attacker_intention"]');

        this._init();
    }

    /** @private */
    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        this._dropdown.addEventListener('click', function (event) {
            if (event.target instanceof HTMLLIElement) {
                const intention = event.target.firstElementChild.dataset.intention;
                self._submitForm(intention);
            } else if (event.target instanceof HTMLAnchorElement) {
                const intention = event.target.dataset.intention;
                self._submitForm(intention);
            }
            // Don't do anything if something besides the list entries is clicked.
        });
    }

    /** @private */
    async _submitForm (intention) {
        this._intentionInput.value = intention;
        this._attackForm.submit();
        this._attackButton.disabled = true;

        const mutantProgressBar = await objects.await('mutantProgressBar');
        mutantProgressBar.activate();
    }
}


export default AttackerIntentionCollection;
