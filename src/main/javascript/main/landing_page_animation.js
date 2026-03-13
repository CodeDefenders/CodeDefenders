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
const animationSpeedFactor = 1 / 2;
const leftCode = document.getElementById("leftCodeEditor");
const rightCode = document.getElementById("rightCodeEditor");
const leftTitle = document.getElementById("leftTitle");
const rightTitle = document.getElementById("rightTitle");
const statusEl = document.getElementById("status");
const splashEl = document.getElementById("splash");
const promptEl = document.getElementById("prompt");
const status = {
    set textContent(value) {
        statusEl.textContent = value;
    },
    set innerHTML(value) {
        statusEl.innerHTML = value;
    },
    async animate(text, duration = 2000) {
        splashEl.querySelector(".inner").innerHTML = text;
        splashEl.setAttribute("aria-hidden", "false");
        await sleep(duration);
        splashEl.setAttribute("aria-hidden", "true");
    },
    async set(text, duration = 2000) {
        this.innerHTML = "";
        await this.animate(text, duration);
        this.innerHTML = text;
    }
};

/**
 * Waits for the given number of milliseconds, scaled by the animation speed factor.
 *
 * @param ms - The number of milliseconds to wait before resolving the promise.
 * @return {Promise<unknown>} A promise that resolves after the specified time has elapsed.
 */
function sleep(ms) {
    return new Promise(res => setTimeout(res, ms / animationSpeedFactor));
}

async function typeInto(node, text, speed = 35) {
    node.classList.add("typewriter");
    node.textContent = "";
    for (let i = 0; i < text.length; i++) {
        node.textContent += text[i];
        await sleep(speed);
    }
    node.classList.remove("typewriter");
}

async function runLandingPageAnimation() {
    const originalCUT =
        `public int max(int a, int b) {
  if (a > b) {
    return a;
  } else {
    return b;
  }
}
`;

    // Initial CUT
    leftTitle.textContent = "Class Under Test (CUT)";
    leftCode.textContent = originalCUT;
    rightTitle.textContent = "Defender: Test Editor";
    status.textContent = "";
    promptEl.classList.add("hidden");

    await sleep(800);

    // Defender writes the test (typewriter) on the right
    await status.set("Defender writes a test");
    await typeInto(
        rightCode,
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5);
  assertEquals(5, result);
}`, 50
    );

    await sleep(1200);

    // Simulate test execution
    await status.set("Executing test...");
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>;
  assertEquals(5, result);
}`;

    await sleep(1200);

    // Execute the CUT line by line
    leftCode.innerHTML =
        `<span class="highlight-exec">public int max(int a, int b) {</span>
  if (a > b) {
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `<span class="highlight-exec">public int max(int a, int b) {</span> <span class="comment">// a = 2, b = 5</span>
  if (a > b) {
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  <span class="highlight-exec">if (a > b) {</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  <span class="highlight-exec">if (a > b) {</span> <span class="comment">// 2 > 5</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  <span class="highlight-exec">if (a > b) {</span> <span class="comment">// false</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  if (a > b) { <span class="comment">// false</span>
    return a;
  } else {
    <span class="highlight-exec">return b;</span>
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  if (a > b) { <span class="comment">// false</span>
    return a;
  } else {
    <span class="highlight-exec">return b;</span> <span class="comment">// return 5</span>
  }
}
`;
    await sleep(600);

    // Return result to test
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>; <span class="comment">// = 5</span>
  assertEquals(5, result);
}`;

    await sleep(600);

    // Evaluate assertion
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5); <span class="comment">// = 5</span>
  <span class="highlight-exec">assertEquals(5, result);</span> <span class="comment">// 5 == 5 ✓</span>
}`;

    await sleep(800);

    // Show green check for passing test
    await status.set(`<span class="ok"><span class="checkmark">✓</span> Test passes!</span>`);

    await sleep(1800);

    // Reset for mutation
    leftCode.innerHTML = originalCUT;
    leftTitle.textContent = "Class Under Test (CUT)";
    rightTitle.textContent = "Defender's test";
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5);
  assertEquals(5, result);
}`;
    status.textContent = "";

    await sleep(800);

    // First mutation in the left CUT
    await status.set("Attacker mutates the CUT");

    await sleep(1200);
    leftTitle.textContent = "Attacker's mutant";
    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a <span class="mutant blink"><</span> b) {
    return a;
  } else {
    return b;
  }
}
`;

    await sleep(1500);

    // Execute test with mutated code
    await status.set("Executing test on mutant...");
    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a <span class="mutant"><</span> b) {
    return a;
  } else {
    return b;
  }
}
`;

    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>;
  assertEquals(5, result);
}`;

    await sleep(1000);

    // Execute mutated function line by line
    leftCode.innerHTML =
        `<span class="highlight-exec">public int max(int a, int b) {</span> <span class="comment">// a = 2, b = 5</span>
  if (a < b) {
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  <span class="highlight-exec">if (a < b) {</span> <span class="comment">// 2 < 5</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  <span class="highlight-exec">if (a < b) {</span> <span class="comment">// true</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  if (a < b) { <span class="comment">// true</span>
    <span class="highlight-exec">return a;</span>
  } else {
    return b;
  }
}
`;
    await sleep(600);

    leftCode.innerHTML =
        `public int max(int a, int b) { <span class="comment">// a = 2, b = 5</span>
  if (a < b) { <span class="comment">// true</span>
    <span class="highlight-exec">return a;</span> <span class="comment">// return 2</span>
  } else {
    return b;
  }
}
`;
    await sleep(600);

    // Return wrong result to test
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>; <span class="comment">// = 2</span>
  assertEquals(5, result);
}`;

    await sleep(600);

    // Evaluate assertion - fails
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5); <span class="comment">// = 2</span>
  <span class="fail highlight-exec">assertEquals(5, result);</span> <span class="comment">// 5 != 2 <span class="fail">✗</span></span>
}`;

    await sleep(800);

    // Test fails, mutant killed
    await status.set(`<span class="fail">Test fails! Mutant killed!</span>The defender gains one point for killing a mutant.`);
    await sleep(600);
    leftCode.classList.add("mutant-dead");
    await sleep(2000);

    // Restore CUT, attacker tries again
    leftCode.classList.remove("mutant-dead");
    leftCode.innerHTML = originalCUT;
    leftTitle.textContent = "Class Under Test (CUT)";
    await status.set("Attacker tries again...");
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5);
  assertEquals(5, result);
}`;

    // Second, subtler mutation of the CUT
    await status.set("Attacker creates a subtler mutant");
    await sleep(1200);
    leftTitle.textContent = "Attacker's mutant";
    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a > b<span class="mutant blink"> - 1</span>) {
    return a;
  } else {
    return b;
  }
}
`;

    await sleep(1500);

    // Execute test with subtle mutant
    await status.set("Executing test on mutant...");
    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a > b - 1) {
    return a;
  } else {
    return b;
  }
}
`;

    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>;
  assertEquals(5, result);
}`;

    await sleep(1000);

    // Execute mutated function line by line
    leftCode.innerHTML =
        `<span class="highlight-exec">public int max(int a, int b) {</span> <span class="comment">// a = 2, b = 5</span>
  if (a > b - 1) {
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(400);

    leftCode.innerHTML =
        `public int max(int a, int b) {
  <span class="highlight-exec">if (a > b - 1) {</span> <span class="comment">// 2 > 5 - 1</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(400);

    leftCode.innerHTML =
        `public int max(int a, int b) {
  <span class="highlight-exec">if (a > b - 1) {</span> <span class="comment">// 2 > 4</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(400);

    leftCode.innerHTML =
        `public int max(int a, int b) {
  <span class="highlight-exec">if (a > b - 1) {</span> <span class="comment">// true</span>
    return a;
  } else {
    return b;
  }
}
`;
    await sleep(400);

    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a > b - 1) { <span class="comment">// true</span>
    return a;
  } else {
    <span class="highlight-exec">return b;</span>
  }
}
`;
    await sleep(400);

    leftCode.innerHTML =
        `public int max(int a, int b) {
  if (a > b - 1) { <span class="comment">// true</span>
    return a;
  } else {
    <span class="highlight-exec">return b;</span> <span class="comment">// return 5</span>
  }
}
`;
    await sleep(600);

    // Return result to test
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = <span class="highlight-exec">max(2, 5)</span>; <span class="comment">// = 5</span>
  assertEquals(5, result);
}`;

    await sleep(600);

    // Evaluate assertion - passes!
    rightCode.innerHTML =
        `void testMaxPicksHigherNumber() {
  int result = max(2, 5); <span class="comment">// = 5</span>
  <span class="highlight-exec">assertEquals(5, result);</span> <span class="comment">// 5 == 5 ✓</span>
}`;

    await sleep(800);

    // All tests pass; mutant survives
    await status.set(
        `<span class="ok"><span class="checkmark">✓</span> All current tests pass. The mutant survives!</span>  The attacker gains one point for creating a mutant that survives one test.`
    );

    await sleep(2500);

    // Final prompt
    promptEl.classList.remove("hidden");

    await sleep(10000);
    runLandingPageAnimation();
}

export default runLandingPageAnimation;
