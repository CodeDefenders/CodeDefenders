import {objects} from '../main';


/* There is honestly not that much reason for this to be a class, other than consistency. */
class AttackerIntentionCollection {

    constructor () {
        /**
         * The dropdown containing the attacker intentions.
         * @type {HTMLElement}
         */
        this.dropdown = document.getElementById('attacker-intention-dropdown');

        /**
         * The "Attack" button normally used to submit mutants.
         * @type {HTMLElement}
         */
        this.attackButton = document.getElementById('submitMutant');

        /**
         * The form to submit for attacking.
         * @type {HTMLFormElement}
         */
        this.attackForm = document.forms['atk'];

        /**
         * The (hidden) input to submit the intention with.
         * @type {HTMLInputElement}
         */
        this.intentionInput = this.attackForm.querySelector('input[name="attacker_intention"]');

        this._init();
    }

    /** @private */
    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        this.dropdown.addEventListener('click', function (event) {
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
    _submitForm (intention) {
        this.intentionInput.value = intention;
        this.attackForm.submit();
        this.attackButton.disabled = true;
        objects.await('mutantProgressBar')
                .then(mutantProgressBar => mutantProgressBar.activate());
    }
}


export default AttackerIntentionCollection;
