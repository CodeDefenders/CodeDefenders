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
import {objects, ProgressBar, PushSocket} from '../main';


class MutantProgressBar extends ProgressBar {
    constructor(progressElement, gameId) {
        super(progressElement);

        /**
         * Game ID of the current game.
         * @type {number}
         */
        this._gameId = gameId;
    }

    async initAsync () {
        this._pushSocket = await objects.await('pushSocket');

        return this;
    }

    async activate () {
        this.setProgress(16, 'Submitting Mutant');
        await this._register();
        await this._subscribe();

        /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
        const reconnect = () => {
            this._pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
            this._pushSocket.reconnect();
            this._subscribe();
        };
        this._pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
    }

    async _subscribe () {
        this._pushSocket.subscribe('registration.MutantProgressBarRegistrationEvent', {
            gameId: this._gameId
        });
    }

    async _register () {
        this._pushSocket.register('mutant.MutantSubmittedEvent', this._onMutantSubmitted.bind(this));
        this._pushSocket.register('mutant.MutantValidatedEvent', this._onMutantValidated.bind(this));
        this._pushSocket.register('mutant.MutantDuplicateCheckedEvent', this._onDuplicateChecked.bind(this));
        this._pushSocket.register('mutant.MutantCompiledEvent', this._onMutantCompiled.bind(this));
        this._pushSocket.register('mutant.MutantTestedEvent', this._onMutantTested.bind(this));
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
