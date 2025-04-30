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
package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.test.TestCompiledEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestTestedOriginalEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for test progressbar events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link TestSubmittedEvent}</li>
 * <li>{@link TestValidatedEvent}</li>
 * <li>{@link TestCompiledEvent}</li>
 * <li>{@link TestTestedOriginalEvent}</li>
 * <li>{@link TestTestedMutantsEvent}</li>
 * </ul>
 */
public class TestProgressBarRegistrationEvent extends RegistrationEvent {
    @Expose private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
