/* adapted from https://www.w3schools.com/howto/howto_js_draggable.asp */
class Draggable extends EventTarget {
    constructor(draggableElement, handleElement) {
        super();

        this.draggableElement = draggableElement;
        this.handleElement = handleElement ?? draggableElement;

        this.lastX = 0;
        this.lastY = 0;

        this._onMouseDown = this.onMouseDown.bind(this);
        this._onMouseUp = this.onMouseUp.bind(this);
        this._onMouseMove = this.onMouseMove.bind(this);
        this.handleElement.addEventListener('mousedown', this._onMouseDown);
    }

    onMouseDown (event) {
        event.preventDefault();
        this.lastX = event.clientX;
        this.lastY = event.clientY;
        document.addEventListener('mouseup', this._onMouseUp);
        document.addEventListener('mousemove', this._onMouseMove);
        this.dispatchEvent(new CustomEvent('start'));
    }

    onMouseMove (event) {
        event.preventDefault();
        const deltaX = event.clientX - this.lastX;
        const deltaY = event.clientY - this.lastY;
        this.lastX = event.clientX;
        this.lastY = event.clientY;
        this.draggableElement.style.top = `${this.draggableElement.offsetTop + deltaY}px`;
        this.draggableElement.style.left = `${this.draggableElement.offsetLeft + deltaX}px`;
        this.draggableElement.style.bottom = null;
        this.draggableElement.style.right = null;
        this.dispatchEvent(new CustomEvent('move'));
    }

    onMouseUp (event) {
        document.removeEventListener('mouseup', this._onMouseUp);
        document.removeEventListener('mousemove', this._onMouseMove);
        this.dispatchEvent(new CustomEvent('stop'));
    }
}


export default Draggable;
