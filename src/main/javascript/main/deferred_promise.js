/**
 * Stores a promise with its resolve/reject methods so it can be resolved/rejected later.
 */
class DeferredPromise {
    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.reject = reject
            this.resolve = resolve
        });
    }
}


export default DeferredPromise;
