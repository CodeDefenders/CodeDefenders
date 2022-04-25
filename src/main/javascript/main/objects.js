import DeferredPromise from "./deferred_promise";


class ObjectRegistry {
    constructor () {
        this.promises = new Map();
    }

    await (name) {
        if (this.promises.has(name)) {
            return this.promises.get(name).promise;
        }

        const deferred = new DeferredPromise();
        this.promises.set(name, deferred);
        return deferred.promise;
    }

    register (name, component) {
        if (this.promises.has(name)) {
            this.promises.get(name).resolve(component);
        }

        const deferred = new DeferredPromise();
        this.promises.set(name, deferred);
        deferred.resolve(component);
    }
}

const objects = new ObjectRegistry();


export {ObjectRegistry, objects};
export default objects;
