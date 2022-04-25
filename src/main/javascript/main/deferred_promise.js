class DeferredPromise {
    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.reject = reject
            this.resolve = resolve
        });
    }
}


export default DeferredPromise;
