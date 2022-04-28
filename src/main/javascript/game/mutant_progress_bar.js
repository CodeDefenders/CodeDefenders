import {objects, ProgressBar, PushSocket} from '../main';


class MutantProgressBar extends ProgressBar {
    constructor(progressElement, gameId) {
        super(progressElement);

        /**
         * Game ID of the current game.
         * @type {number}
         */
        this.gameId = gameId;
    }

    async initAsync () {
        this.pushSocket = await objects.await('pushSocket');
    }

    async activate () {
        this.setProgress(16, 'Submitting Mutant');
        await this._register();
        await this._subscribe();

        /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
        const reconnect = () => {
            this.pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
            this.pushSocket.reconnect();
            this._subscribe();
        };
        this.pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
    }

    async _subscribe () {
        this.pushSocket.subscribe('registration.MutantProgressBarRegistrationEvent', {
            gameId: this.gameId
        });
    }

    async _register () {
        this.pushSocket.register('mutant.MutantSubmittedEvent', this._onMutantSubmitted.bind(this));
        this.pushSocket.register('mutant.MutantValidatedEvent', this._onMutantValidated.bind(this));
        this.pushSocket.register('mutant.MutantDuplicateCheckedEvent', this._onDuplicateChecked.bind(this));
        this.pushSocket.register('mutant.MutantCompiledEvent', this._onMutantCompiled.bind(this));
        this.pushSocket.register('mutant.MutantTestedEvent', this._onMutantTested.bind(this));
    }

    _onMutantSubmitted (event) {
        this.setProgress(33, 'Validating Mutant');
    }

    _onMutantValidated (event) {
        if (event.success) {
            this.setProgress(50, 'Checking For Duplicate Mutants');
        } else {
            this.setProgress(100, 'Mutant Is Not Valid');
        }
    }

    _onDuplicateChecked (event) {
        if (event.success) {
            this.setProgress(66, 'Compiling Mutant');
        } else {
            this.setProgress(100, 'Found Duplicate Mutant');
        }
    }

    _onMutantCompiled (event) {
        if (event.success) {
            this.setProgress(83, 'Running Tests Against Mutant');
        } else {
            this.setProgress(100, 'Mutant Did Not Compile');
        }
    }

    _onMutantTested (event) {
        this.setProgress(100, 'Done');
        // if (event.survived) {
        //     setProgress(100, 'Mutant Survived');
        // } else {
        //     setProgress(100, 'Mutant Killed');
        // }
    }
}


export default MutantProgressBar;
