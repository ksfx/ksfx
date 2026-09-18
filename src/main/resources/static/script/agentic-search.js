/*
 * Cross-agent search overlay (see AgenticSearchController). Same swap-the-body-with-a-server-
 * rendered-fragment pattern as the file browser and schedule overlays, plus one thing they don't
 * need: a "back to results" step, since clicking a result drills into a context view instead of
 * navigating away.
 */
(function () {
    'use strict';

    var overlay = document.getElementById('agenticSearchOverlay');

    if (!overlay) {
        return;
    }

    var searchEndpoint = overlay.dataset.searchEndpoint;

    var body = document.getElementById('agenticSearchBody');
    var input = document.getElementById('agenticSearchInput');
    var openBtn = document.getElementById('agenticSearchBtn');
    var closeBtn = document.getElementById('agenticSearchCloseBtn');
    var backBtn = document.getElementById('agenticSearchBackBtn');

    var lastQuery = '';
    var debounceTimer = null;

    function showError(message) {
        alert('Search: ' + message);
    }

    // Same lesson as the other overlays: surface the server's own reason instead of a bare
    // "HTTP 400", so a failure is diagnosable from the browser alone.
    function failWithBody(response) {
        return response.text().then(function (bodyText) {
            throw new Error('HTTP ' + response.status + (bodyText ? ' - ' + bodyText.substring(0, 200) : ''));
        });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text == null ? '' : text;
        return div.innerHTML;
    }

    function runSearch(term) {
        lastQuery = term;

        fetch(searchEndpoint + '/query?q=' + encodeURIComponent(term), { headers: { 'X-Requested-With': 'fetch' } })
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(function (html) {
                body.innerHTML = html;
                backBtn.hidden = true;
            })
            .catch(function (error) { showError(error.message); });
    }

    // Renders the 10-before/match/10-after window around one result. The fragment returned by the
    // server is the same messagesList markup the real chat page uses (see AgentController), so it
    // needs the same post-processing that page does for attachments/tool-activity chips - that's
    // exposed globally as window.agenticHydrateHistory specifically so this file can call it
    // without duplicating that rendering logic.
    function loadContext(messageId, agentName, agentId) {
        fetch(searchEndpoint + '/context?messageId=' + encodeURIComponent(messageId), { headers: { 'X-Requested-With': 'fetch' } })
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(function (html) {
                body.innerHTML = '<div class="agentic-search-context-header">'
                    + '<span class="agentic-search-context-agent">' + escapeHtml(agentName) + '</span>'
                    + '<a class="agentic-search-context-open" href="/agentic/chat/' + encodeURIComponent(agentId) + '">Open full chat <i class="fa fa-external-link"></i></a>'
                    + '</div>'
                    + '<div class="agentic-search-context-messages">' + html + '</div>';
                backBtn.hidden = false;

                if (typeof window.agenticHydrateHistory === 'function') {
                    window.agenticHydrateHistory();
                }

                var matched = body.querySelector('[data-message-id="' + messageId + '"]');

                if (matched) {
                    matched.classList.add('agentic-search-highlight');
                    matched.scrollIntoView({ block: 'center' });
                }
            })
            .catch(function (error) { showError(error.message); });
    }

    openBtn.addEventListener('click', function () {
        overlay.hidden = false;
        input.value = '';
        backBtn.hidden = true;
        input.focus();
        runSearch('');
    });

    closeBtn.addEventListener('click', function () {
        overlay.hidden = true;
    });

    // Click on the dark backdrop area (not the panel itself) closes too.
    overlay.addEventListener('click', function (event) {
        if (event.target === overlay) {
            overlay.hidden = true;
        }
    });

    input.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        var term = input.value;
        debounceTimer = setTimeout(function () { runSearch(term); }, 250);
    });

    backBtn.addEventListener('click', function () {
        runSearch(lastQuery);
    });

    // Result rows (search) and the "open full chat" link both live inside the swapped body, so one
    // delegated listener instead of rebinding after every swap.
    body.addEventListener('click', function (event) {
        var resultEl = event.target.closest('[data-search-context]');

        if (resultEl) {
            loadContext(resultEl.dataset.searchContext, resultEl.dataset.searchContextAgent, resultEl.dataset.searchContextAgentId);
        }
    });
})();
