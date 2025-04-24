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
import DeferredPromise from "./deferred_promise";


/**
 * A simple registry to await objects without having to worry about initialization order.
 */
class ObjectRegistry {
    constructor () {
        this.promises = new Map();
    }

    /**
     * Returns a promise that is resolved when an object with that name is registered.
     * @param name The name of the object.
     * @return {Promise<unknown>}
     */
    await (name) {
        if (this.promises.has(name)) {
            return this.promises.get(name).promise;
        }

        const deferred = new DeferredPromise();
        this.promises.set(name, deferred);
        return deferred.promise;
    }

    /**
     * Registers an object with the given name, resolving any requests that were made to it before.
     * @param name The name of the object.
     * @param object The object to register.
     */
    register (name, object) {
        if (this.promises.has(name)) {
            this.promises.get(name).resolve(object);
        }

        const deferred = new DeferredPromise();
        this.promises.set(name, deferred);
        deferred.resolve(object);
    }
}

const objects = new ObjectRegistry();


export {ObjectRegistry, objects};
export default objects;
