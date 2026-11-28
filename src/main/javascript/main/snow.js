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

class Snow {
    static init(selector) {
        const parent = document.querySelector(selector);
        if (parent) return new Snow(parent);
    }

    constructor(parent) {
        this.parent = parent;
        this.canvas = document.createElement("canvas");
        this.canvas.className = "christmas-scene__snow";
        this.canvas.setAttribute("aria-hidden", "true");
        parent.prepend(this.canvas);

        this.context = this.canvas.getContext("2d");
        this.width = 0;
        this.height = 0;
        this.flakes = [];
        this.frame = 0;
        this.lastTime = 0;
        this.visible = true;

        this.animate = this.animate.bind(this);
        this.resize = this.resize.bind(this);
        this.updateAnimation = this.updateAnimation.bind(this);

        new IntersectionObserver(([entry]) => {
            this.visible = entry.isIntersecting;
            this.updateAnimation();
        }).observe(parent);

        window.addEventListener("resize", this.resize);
        window.addEventListener("load", this.resize, { once: true });
        document.addEventListener("visibilitychange", this.updateAnimation);
        this.resize();
    }

    draw(delta = 0) {
        const { context, width, height } = this;
        if (!context) return;
        context.clearRect(0, 0, width, height);
        for (const flake of this.flakes) {
            flake.phase += delta * .7;
            flake.x += Math.sin(flake.phase) * delta * 8;
            flake.y += flake.speed * delta;
            if (flake.y > height + 4) {
                flake.y = -4;
                flake.x = Math.random() * width;
            }
            if (flake.x < -4) flake.x = width + 4;
            if (flake.x > width + 4) flake.x = -4;
            context.beginPath();
            context.arc(flake.x, flake.y, flake.radius, 0, Math.PI * 2);
            context.fillStyle = `rgba(242, 249, 255, ${flake.opacity})`;
            context.fill();
        }
    }

    animate(time) {
        this.draw(this.lastTime ? Math.min((time - this.lastTime) / 1000, .05) : 0);
        this.lastTime = time;
        this.frame = requestAnimationFrame(this.animate);
    }

    updateAnimation() {
        cancelAnimationFrame(this.frame);
        this.lastTime = 0;
        if (this.context && this.visible && !document.hidden) {
            this.frame = requestAnimationFrame(this.animate);
        }
    }

    resize() {
        const root = document.documentElement;
        const rootBounds = root.getBoundingClientRect();
        this.width = Math.min(root.clientWidth, rootBounds.width);
        this.height = this.parent.clientHeight;
        const left = rootBounds.left - this.parent.getBoundingClientRect().left;
        this.parent.style.setProperty("--christmas-scene-width", `${this.width}px`);
        this.parent.style.setProperty("--christmas-scene-left", `${left}px`);
        if (!this.context) return;

        // Cap canvas memory for tall mobile calendars and high-DPI screens.
        const scale = Math.min(window.devicePixelRatio || 1, 2,
                Math.sqrt(4_000_000 / Math.max(1, this.width * this.height)));
        this.canvas.width = Math.max(1, Math.round(this.width * scale));
        this.canvas.height = Math.max(1, Math.round(this.height * scale));
        this.context.setTransform(scale, 0, 0, scale, 0, 0);

        this.flakes = Array.from(
                {length: Math.min(160, Math.max(18, Math.round(this.width * this.height / 14000)))},
                () => ({
                    x: Math.random() * this.width,
                    y: Math.random() * this.height,
                    radius: .8 + Math.random() * 1.7,
                    speed: 14 + Math.random() * 24,
                    opacity: .25 + Math.random() * .6,
                    phase: Math.random() * Math.PI * 2
                }));
        this.draw();
        this.updateAnimation();
    }
}

export default Snow;
