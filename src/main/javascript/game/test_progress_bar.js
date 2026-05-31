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


class TestProgressBar extends ProgressBar {
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
        this.setProgress(16, i18n.tr('Submitting Test'));
        await this._register();
        await this._subscribe();

        /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
        const reconnect = event => {
            this._pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
            this._pushSocket.reconnect();
            this._subscribe();
        };
        this._pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
    }

    async _subscribe () {
        this._pushSocket.subscribe('registration.TestProgressBarRegistrationEvent', {
            gameId: this._gameId
        });
    }

    async _register () {
        this._pushSocket.register('test.TestSubmittedEvent', this._onTestSubmitted.bind(this));
        this._pushSocket.register('test.TestCompiledEvent', this._onTestCompiled.bind(this));
        this._pushSocket.register('test.TestValidatedEvent', this._onTestValidated.bind(this));
        this._pushSocket.register('test.TestTestedOriginalEvent', this._onTestTestedOriginal.bind(this));
        this._pushSocket.register('test.TestTestedMutantsEvent', this._onTestTestedMutants.bind(this));
    }

    _onTestSubmitted (event) {
        this.setProgress(33, i18n.tr('Validating Test'));
    }

    _onTestValidated (event) {
        if (event.success) {
            this.setProgress(50, i18n.tr('Compiling Test'));
        } else {
            this.setProgress(100, i18n.tr('Test Is Not Valid'));
        }
    }

    _onTestCompiled (event) {
        if (event.success) {
            this.setProgress(66, i18n.tr('Running Test Against Original'));
        } else {
            this.setProgress(100, i18n.tr('Test Did Not Compile'));
        }
    }

    _onTestTestedOriginal (event) {
        if (event.success) {
            this.setProgress(83, i18n.tr('Running Test Against Mutants'));
        } else {
            this.setProgress(100, i18n.tr('Test Failed Against Original'));
        }
    }

    _onTestTestedMutants (event) {
        this.setProgress(100, i18n.tr('Done'));
    }
}


export default TestProgressBar;
