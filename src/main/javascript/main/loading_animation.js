class LoadingAnimation {
    static hideAnimation (elem) {
        while (!elem.classList.contains('loading')) {
            elem = elem.parentElement;
        }
        elem.classList.remove('loading');
    }
}


export default LoadingAnimation;
