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
import {Collapse} from '../thirdparty/bootstrap';
import CodeMirror from '../thirdparty/codemirror';
import CodeCompletion from '../game/code_completion';
import {getTestMethods} from '../game/test_completion';

/**
 * Turn the unified diff returned by the Mutle API into a readable code view.
 *
 * The API owns the patch representation, so this only changes the text that
 * is presented in the read-only editor.  In particular, the synthetic file
 * names and hunk markers are useful to patch tooling but not to a player
 * trying to understand one small mutation.
 */
export const formatMutleDiff = diff => {
    if (typeof diff !== 'string') return '';
    return diff
        .split(/\r?\n/)
        .filter(line => !/^(?:---|\+\+\+) \/dev\/null$/.test(line))
        .filter(line => !/^@@ .* @@(?:.*)$/.test(line))
        .join('\n');
};

const root = typeof document === 'undefined' ? null : document.querySelector('#mutle-play');

if (root) {
    const api = `${document.head.dataset.contextPath}/api/mutle`;
    const text = root.dataset;
    const marks = {NONE: '⬜', METHOD: '🟨', LINE: '🟧', KILLED: '💀'};
    const markText = mark => text[`mark${mark[0]}${mark.slice(1).toLowerCase()}`] || mark;
    const coverageClasses = [
        'mutle-covered-none',
        'mutle-covered-method',
        'mutle-covered-line',
        'mutle-covered-killed'
    ];
    const content = document.querySelector('#mutle-content');
    const loadError = document.querySelector('#mutle-load-error');
    const notice = document.querySelector('#mutle-notice');
    const noticeMessage = document.querySelector('#mutle-notice-message');
    const noticeSpinner = document.querySelector('#mutle-notice-spinner');
    const submit = document.querySelector('#mutle-submit');
    const submitIcon = document.querySelector('#mutle-submit-icon');
    const submitSpinner = document.querySelector('#mutle-submit-spinner');
    const submitLabel = document.querySelector('#mutle-submit-label');
    const cutCard = document.querySelector('#mutle-cut').closest('.mutle-code-card');
    const testPanel = document.querySelector('#mutle-test-panel');
    const testRulesPanel = document.querySelector('#mutle-test-rules-panel');
    const revealPanel = document.querySelector('#mutle-reveal-panel');
    const history = document.querySelector('#mutle-history');
    const feedbackPanel = document.querySelector('#mutle-feedback-panel');
    const historyPanel = document.querySelector('#mutle-history-panel');
    const revealSection = document.querySelector('#mutle-reveal');
    const noticeDetails = document.querySelector('#mutle-notice-details');
    const noticeDetailText = document.querySelector('#mutle-notice-detail-text');
    const share = document.querySelector('#mutle-share');
    const historyEditors = [];
    const revealEditors = [];
    const resizeObservers = new WeakMap();
    let state;
    let initializedTemplate = false;

    const labelEditorInput = (editor, label) => {
        editor.getInputField().setAttribute('aria-label', label);
    };
    const observeResize = editor => {
        if (typeof ResizeObserver === 'undefined') return;
        const observer = new ResizeObserver(() => editor.refresh());
        observer.observe(editor.getWrapperElement());
        resizeObservers.set(editor, observer);
    };

    const cut = CodeMirror.fromTextArea(document.querySelector('#mutle-cut'), {
        mode: 'text/x-java',
        readOnly: true,
        lineNumbers: true,
        matchBrackets: true,
        autoRefresh: true
    });
    cut.getWrapperElement().classList.add('codemirror-readonly');
    labelEditorInput(cut, document.querySelector('#mutle-cut').getAttribute('aria-label'));
    observeResize(cut);
    const test = CodeMirror.fromTextArea(document.querySelector('#mutle-test'), {
        mode: 'text/x-java',
        lineNumbers: true,
        indentUnit: 4,
        smartIndent: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        extraKeys: {
            'Ctrl-Space': 'completeMutleTest',
            'Tab': 'insertSoftTab'
        },
        keyMap: text.keymap,
        autoRefresh: true
    });
    labelEditorInput(test, document.querySelector('#mutle-test').getAttribute('aria-label'));
    observeResize(test);
    const codeCompletion = new CodeCompletion();
    codeCompletion.registerCodeCompletionCommand(test, 'completeMutleTest');
    codeCompletion.setCompletionPool('testMethods', new Set(getTestMethods('JUNIT4,HAMCREST', false)));

    const isReadOnly = () => state && (state.solved || state.failed || state.expired);
    const syncRevealHeight = () => {
        revealPanel.style.height = isReadOnly() ? `${cutCard.getBoundingClientRect().height}px` : '';
    };
    const call = async (path, body, fallback) => {
        const response = await fetch(`${api}${path}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify(body)
        });
        if (response.ok) {
            try {
                return await response.json();
            } catch {
                throw new Error(fallback);
            }
        }
        let diagnostic = null;
        if ([413, 422, 429, 503].includes(response.status)) {
            try {
                const rejected = await response.json();
                diagnostic = typeof rejected.message === 'string' ? rejected.message : null;
            } catch {
                diagnostic = null;
            }
        }
        if (response.status === 404) diagnostic = text.unavailable;
        if (response.status === 409) diagnostic = text.inProgress;
        throw new Error(diagnostic || fallback);
    };
    const setNotice = (kind, message, busy = false) => {
        const detailsMarker = '\n\nDetails:\n';
        const detailsIndex = message ? message.indexOf(detailsMarker) : -1;
        const summary = detailsIndex < 0 ? message : message.slice(0, detailsIndex);
        const details = detailsIndex < 0 ? '' : message.slice(detailsIndex + detailsMarker.length);
        notice.className = `alert mutle-notice alert-${kind}`;
        notice.classList.toggle('d-none', !message);
        noticeMessage.textContent = summary || '';
        noticeDetailText.textContent = details;
        noticeDetails.classList.toggle('d-none', !details);
        noticeDetails.open = false;
        noticeSpinner.classList.toggle('d-none', !busy);
        root.setAttribute('aria-busy', String(busy));
    };
    const setSubmitting = submitting => {
        submitIcon.classList.toggle('d-none', submitting);
        submitSpinner.classList.toggle('d-none', !submitting);
        submitLabel.textContent = submitting ? text.running : text.submitLabel;
        submit.disabled = submitting || isReadOnly();
    };
    const destroyEditors = editors => {
        editors.splice(0).forEach(editor => {
            resizeObservers.get(editor)?.disconnect();
            editor.toTextArea();
        });
    };
    const clearCoverage = () => cut.eachLine(line => {
        coverageClasses.forEach(name => cut.removeLineClass(line, 'background', name));
    });
    const applyCoverage = guess => {
        clearCoverage();
        if (!guess) return;
        guess.coveredLines.forEach(line => {
            cut.addLineClass(line - 1, 'background', `mutle-covered-${guess.mark.toLowerCase()}`);
        });
    };
    const selectGuess = (index, openHistory = false) => {
        const guess = state?.guesses[index];
        if (!guess) return;
        applyCoverage(guess);
        document.querySelectorAll('.mutle-cell-button').forEach((button, buttonIndex) => {
            const selected = buttonIndex === index;
            button.classList.toggle('selected', selected);
            button.setAttribute('aria-pressed', String(selected));
        });
        document.querySelectorAll('.mutle-history-button').forEach((button, buttonIndex) => {
            button.classList.toggle('selected', buttonIndex === index);
        });
        if (openHistory) {
            const panel = document.querySelector(`#mutle-test-collapse-${index}`);
            const button = document.querySelector(`#mutle-test-heading-${index} button`);
            Collapse.getOrCreateInstance(panel, {parent: history}).show();
            button.focus();
            panel.closest('.accordion-item').scrollIntoView({behavior: 'smooth', block: 'nearest'});
        }
    };
    const renderCells = guesses => {
        const cells = document.querySelector('#mutle-results');
        cells.replaceChildren();
        for (let index = 0; index < 6; index++) {
            const guess = guesses[index];
            const cell = document.createElement('li');
            const button = document.createElement('button');
            cell.className = `mutle-cell ${guess ? guess.mark.toLowerCase() : ''}`;
            button.type = 'button';
            button.className = 'mutle-cell-button';
            button.textContent = index + 1;
            button.disabled = !guess;
            button.setAttribute('aria-pressed', 'false');
            button.setAttribute('aria-label', guess
                ? text.guess.replace('{0}', index + 1).replace('{1}', markText(guess.mark))
                : text.empty.replace('{0}', index + 1));
            if (guess) button.addEventListener('click', () => selectGuess(index, true));
            cell.append(button);
            cells.append(cell);
        }
    };
    const renderHistory = guesses => {
        destroyEditors(historyEditors);
        history.replaceChildren();
        const hasHistory = guesses.length > 0;
        feedbackPanel.classList.toggle('col-lg-4', hasHistory);
        feedbackPanel.classList.toggle('col-lg-6', !hasHistory);
        feedbackPanel.classList.toggle('mx-auto', !hasHistory);
        historyPanel.classList.toggle('d-none', !hasHistory);
        guesses.forEach((guess, index) => {
            const item = document.createElement('div');
            const heading = document.createElement('h3');
            const button = document.createElement('button');
            const badge = document.createElement('span');
            const label = document.createElement('span');
            const collapse = document.createElement('div');
            const body = document.createElement('div');
            const textarea = document.createElement('textarea');
            item.className = 'accordion-item';
            heading.className = 'accordion-header';
            heading.id = `mutle-test-heading-${index}`;
            button.className = 'accordion-button collapsed mutle-history-button';
            button.type = 'button';
            button.setAttribute('data-bs-toggle', 'collapse');
            button.setAttribute('data-bs-target', `#mutle-test-collapse-${index}`);
            button.setAttribute('aria-controls', `mutle-test-collapse-${index}`);
            button.setAttribute('aria-expanded', 'false');
            button.addEventListener('click', () => selectGuess(index));
            badge.className = `mutle-history-mark ${guess.mark.toLowerCase()}`;
            badge.textContent = index + 1;
            label.textContent = text.test.replace('{0}', index + 1).replace('{1}', markText(guess.mark));
            button.append(badge, label);
            heading.append(button);
            collapse.className = 'accordion-collapse collapse';
            collapse.id = `mutle-test-collapse-${index}`;
            collapse.setAttribute('data-bs-parent', '#mutle-history');
            collapse.setAttribute('aria-labelledby', heading.id);
            body.className = 'accordion-body p-0 mutle-readonly-code';
            textarea.value = guess.source;
            if (!isReadOnly()) {
                const actions = document.createElement('div');
                const reuse = document.createElement('button');
                const reuseIcon = document.createElement('i');
                const reuseLabel = document.createElement('span');
                const reuseText = text.reuseTest;
                actions.className = 'mutle-history-actions';
                reuse.type = 'button';
                reuse.className = 'btn btn-sm btn-outline-secondary mutle-reuse-button';
                reuse.setAttribute('aria-label', reuseText);
                reuseIcon.className = 'fa fa-copy me-1';
                reuseIcon.setAttribute('aria-hidden', 'true');
                reuseLabel.textContent = reuseText;
                reuse.append(reuseIcon, reuseLabel);
                reuse.addEventListener('click', () => {
                    test.setValue(guess.source);
                    test.focus();
                    setNotice('success', text.testCopied);
                });
                actions.append(reuse);
                body.append(actions);
            }
            body.append(textarea);
            collapse.append(body);
            item.append(heading, collapse);
            history.append(item);
            const editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                matchBrackets: true,
                mode: 'text/x-java',
                readOnly: true,
                autoRefresh: true
            });
            editor.getWrapperElement().classList.add('codemirror-readonly');
            labelEditorInput(editor, text.test.replace('{0}', index + 1).replace('{1}', markText(guess.mark)));
            observeResize(editor);
            historyEditors.push(editor);
            collapse.addEventListener('shown.bs.collapse', () => editor.refresh());
        });
    };
    const revealCodeItem = (accordion, index, headingText, source, mode, expanded = false) => {
        const item = document.createElement('div');
        const heading = document.createElement('h3');
        const button = document.createElement('button');
        const collapse = document.createElement('div');
        const body = document.createElement('div');
        const textarea = document.createElement('textarea');
        item.className = `accordion-item${expanded ? ' expanded' : ''}`;
        heading.className = 'accordion-header';
        heading.id = `mutle-reveal-heading-${index}`;
        button.className = `accordion-button${expanded ? '' : ' collapsed'}`;
        button.type = 'button';
        button.textContent = headingText;
        button.setAttribute('data-bs-toggle', 'collapse');
        button.setAttribute('data-bs-target', `#mutle-reveal-collapse-${index}`);
        button.setAttribute('aria-controls', `mutle-reveal-collapse-${index}`);
        button.setAttribute('aria-expanded', String(expanded));
        heading.append(button);
        collapse.className = `accordion-collapse collapse${expanded ? ' show' : ''}`;
        collapse.id = `mutle-reveal-collapse-${index}`;
        collapse.setAttribute('data-bs-parent', '#mutle-reveal-accordion');
        collapse.setAttribute('aria-labelledby', heading.id);
        body.className = 'accordion-body p-0 mutle-readonly-code mutle-reveal-code';
        textarea.value = source;
        body.append(textarea);
        collapse.append(body);
        item.append(heading, collapse);
        accordion.append(item);
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: mode === 'text/x-java',
            mode,
            readOnly: true,
            autoRefresh: true
        });
        editor.getWrapperElement().classList.add('codemirror-readonly');
        labelEditorInput(editor, headingText);
        observeResize(editor);
        revealEditors.push(editor);
        collapse.addEventListener('show.bs.collapse', () => item.classList.add('expanded'));
        collapse.addEventListener('hide.bs.collapse', () => item.classList.remove('expanded'));
        collapse.addEventListener('shown.bs.collapse', () => editor.refresh());
        if (expanded) requestAnimationFrame(() => editor.refresh());
    };
    const renderReveal = reveal => {
        destroyEditors(revealEditors);
        revealSection.replaceChildren();
        share.parentElement.classList.toggle('d-none', !reveal);
        if (!reveal) return;
        const intro = document.createElement('div');
        const icon = document.createElement('i');
        const copy = document.createElement('div');
        const title = document.createElement('h2');
        const explanation = document.createElement('p');
        const accordion = document.createElement('div');
        intro.className = 'mutle-reveal-intro';
        icon.className = 'fa fa-search';
        icon.setAttribute('aria-hidden', 'true');
        title.textContent = text.reveal;
        explanation.textContent = reveal.explanation;
        copy.append(title, explanation);
        intro.append(icon, copy);
        accordion.className = 'accordion mutle-reveal-accordion';
        accordion.id = 'mutle-reveal-accordion';
        revealSection.append(intro, accordion);
        revealCodeItem(accordion, 0, text.mutantSource, reveal.source, 'text/x-java');
        revealCodeItem(accordion, 1, text.mutantDiff, formatMutleDiff(reveal.diff), 'text/x-diff', true);
        revealCodeItem(accordion, 2, text.proofTest, reveal.proofTestSource, 'text/x-java');
    };
    const show = value => {
        state = value;
        document.querySelector('#mutle-loading').classList.add('d-none');
        loadError.classList.add('d-none');
        content.classList.remove('d-none');
        document.querySelector('#mutle-series').textContent = `${text.series} ${value.series}`;
        document.querySelector('#mutle-title').textContent = value.title;
        document.querySelector('#mutle-specification').textContent = value.specification;
        document.querySelector('#mutle-remaining').textContent = text.remaining.replace('{0}', value.remainingGuesses);
        document.querySelector('#mutle-status').textContent = value.solved
            ? text.solved
            : (value.failed ? text.failed : (value.expired ? text.expired : ''));
        const completed = isReadOnly();
        testPanel.classList.toggle('d-none', completed);
        testRulesPanel.classList.toggle('d-none', completed);
        revealPanel.classList.toggle('d-none', !completed);
        cut.setValue(value.originalSource);
        syncRevealHeight();
        codeCompletion.setCompletionPool('case', codeCompletion.getCompletionsForJavaFiles([
            value.originalSource,
            value.testTemplate || ''
        ]));
        renderHistory(value.guesses);
        renderCells(value.guesses);
        const latestGuessIndex = value.guesses.length - 1;
        if (latestGuessIndex < 0) {
            clearCoverage();
        } else {
            selectGuess(latestGuessIndex);
        }
        test.setOption('readOnly', isReadOnly());
        test.getWrapperElement().classList.toggle('codemirror-readonly', isReadOnly());
        submit.disabled = isReadOnly();
        if (!isReadOnly() && !initializedTemplate && !test.getValue().trim() && value.testTemplate) {
            test.setValue(value.testTemplate);
            initializedTemplate = true;
        }
        renderReveal(value.reveal);
    };
    const load = async () => {
        const existingGame = Boolean(root.dataset.gameId);
        const body = existingGame
            ? {gameId: Number(root.dataset.gameId)}
            : (root.dataset.fixtureKey ? {fixtureKey: root.dataset.fixtureKey} : {});
        show(await call(existingGame ? '/state' : '/start', body, text.loadError));
    };
    submit.addEventListener('click', async () => {
        setSubmitting(true);
        setNotice('info', text.running, true);
        try {
            const value = await call('/guess', {gameId: state.gameId, source: test.getValue()}, text.submitError);
            show(value);
            const latest = value.guesses.at(-1);
            setNotice('success', text.accepted.replace('{0}', markText(latest.mark)));
        } catch (cause) {
            setNotice('danger', cause.message || text.submitError);
        } finally {
            setSubmitting(false);
        }
    });
    share.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(`Mutle #${state.series} ${state.guesses.length}/6\n${state.guesses.map(guess => marks[guess.mark]).join('')}`);
            document.querySelector('#mutle-copy-status').textContent = text.copied;
        } catch {
            document.querySelector('#mutle-copy-status').textContent = text.copyFailed;
        }
    });
    window.addEventListener('resize', () => requestAnimationFrame(syncRevealHeight));
    load().catch(cause => {
        document.querySelector('#mutle-loading').classList.add('d-none');
        loadError.textContent = cause.message || text.loadError;
        loadError.classList.remove('d-none');
    });
}
