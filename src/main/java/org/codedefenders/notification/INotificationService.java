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
package org.codedefenders.notification;

/**
 * Interface for the Notification Service.
 *
 * <p>Modeled following the Guava Event Bus generic interface for message passing and registration.
 *
 * @author gambi
 */
// TODO Introduce a bit more logic here for filtering, tagging, authentication, etc
// TODO Introduce some logic for authentication authorization some interactions must be specific for client request
//  so maybe some ticketing service or similar. Most likely this can be done using some filter or request listener
public interface INotificationService {
    void register(Object eventHandler);

    void unregister(Object eventHandler);

    void post(Object message);
}
