/*
 * Chat client for the "Agentic" feature: sends a message, consumes the SSE stream returned by
 * POST /agentic/chat/{id} via fetch()+ReadableStream (EventSource can't POST), and renders text,
 * tool-call activity and errors as they arrive. Kept in its own file, separate from
 * script/utils.js, so the Agentic module can be lifted out of KSFX later.
 */
(function () {
    'use strict';

    var root = document.getElementById('agenticChatRoot');

    if (!root) {
        return;
    }

    var chatEndpoint = root.dataset.chatEndpoint;
    var downloadEndpoint = root.dataset.downloadEndpoint;
    var csrfHeader = root.dataset.csrfHeader;
    var csrfToken = root.dataset.csrfToken;

    // Fills the rest of the viewport below the sidebar/chat panes instead of the fixed
    // "calc(100vh - 300px)" the CSS used to hardcode - that guessed constant didn't account for
    // the page's actual chrome height (test-instance banner, nav wrapping to two lines on a
    // narrow window, etc.), so it under- or over-shot depending on the page/screen and left a
    // dead strip of unused space below the composer on some screens (see agentic-chat.css's
    // .agentic-sidebar/.agentic-chat-wrapper comments). Measuring the real position at runtime
    // instead is correct regardless of what's above it. Re-run on resize (viewport height/width
    // change, e.g. rotating a tablet or the nav wrapping differently) and window load (web fonts/
    // images loading late can still shift layout after DOMContentLoaded).
    //
    // Each element's own top, not a shared one from their common ancestor: .agentic-chat-wrapper
    // sits below .agentic-toolbar (the title row) inside .agentic-main, so it starts ~40px lower
    // than .agentic-sidebar does - sizing both off one shared top (originally .agentic-layout's)
    // undersized that gap for the wrapper, pushing its bottom edge that far past the viewport and
    // forcing the whole page to scroll just to reach the composer. Capped at maxHeight so a very
    // tall monitor doesn't turn this into a mostly-empty giant panel either.
    function sizeAgenticPanes() {
        var bottomBreathingRoom = 24;
        var minHeight = 420;
        var maxHeight = 820;

        document.querySelectorAll('.agentic-sidebar, .agentic-chat-wrapper').forEach(function (el) {
            // Below the mobile breakpoint .agentic-sidebar becomes a fixed-position slide-in overlay
            // sized by CSS (top:0/bottom:0, see agentic-chat.css) - leave it alone here, an inline
            // height clamped to [420,820]px would either fight that or (on a short phone screen)
            // just be wrong, since this clamp is tuned for the two-pane desktop layout.
            if (getComputedStyle(el).position === 'fixed') {
                return;
            }

            var top = el.getBoundingClientRect().top;
            var height = Math.min(maxHeight, Math.max(minHeight, window.innerHeight - top - bottomBreathingRoom));

            el.style.height = height + 'px';
        });
    }

    sizeAgenticPanes();
    window.addEventListener('resize', sizeAgenticPanes);
    window.addEventListener('load', sizeAgenticPanes);

    var messagesEl = document.getElementById('agenticMessages');
    var inputEl = document.getElementById('agenticInput');
    var sendBtn = document.getElementById('agenticSendBtn');
    var attachBtn = document.getElementById('agenticAttachBtn');
    var fileInputEl = document.getElementById('agenticFileInput');
    var pendingFilesEl = document.getElementById('agenticPendingFiles');
    var pendingFiles = [];

    var ROLE_LABELS = { user: 'You', assistant: 'Assistant', system: 'System', agent: 'Agent' };

    var TOOL_ICONS = {
        Bash: 'fa-terminal',
        Read: 'fa-file-text-o',
        Write: 'fa-pencil-square-o',
        Edit: 'fa-pencil',
        Grep: 'fa-search',
        Glob: 'fa-folder-open-o',
        WebFetch: 'fa-globe',
        WebSearch: 'fa-globe',
        Task: 'fa-sitemap'
    };

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function createMessageEl(role) {
        var msg = document.createElement('div');
        msg.className = 'agentic-msg agentic-msg--' + role;

        var label = document.createElement('div');
        label.className = 'agentic-role-label';
        label.textContent = ROLE_LABELS[role] || role;
        msg.appendChild(label);

        var bubble = document.createElement('div');
        bubble.className = 'agentic-bubble';
        msg.appendChild(bubble);

        messagesEl.appendChild(msg);
        scrollToBottom();

        return { root: msg, bubble: bubble, toolBody: null };
    }

    function showTyping(bubble) {
        var typing = document.createElement('span');
        typing.className = 'agentic-typing';
        typing.innerHTML = '<span></span><span></span><span></span>';
        bubble.appendChild(typing);
    }

    function clearTyping(bubble) {
        var typing = bubble.querySelector('.agentic-typing');

        if (typing) {
            bubble.removeChild(typing);
        }
    }

    // Renders {fileName, path?} chips into `container` - used both for a just-sent message
    // (container is a freshly created div, `path` not yet known - the upload happens server-side
    // as part of the in-flight request) and for history reloaded from the DB (container is the
    // template's own .agentic-attachments/.agentic-generated-files div, see the parsing loops
    // below, `path` is set there so the chip can link to the download endpoint). Also reused for
    // agent-generated files (see the 'generated_files' SSE handler and the matching history loop)
    // with a different iconClass to distinguish "you gave this to the agent" from the reverse.
    // `path` is always relative to the agent's workspace root - the full path is sent (not just
    // the last segment), since generated files can live in subfolders, not just uploads/.
    function renderAttachmentChips(container, attachments, iconClass) {
        (attachments || []).forEach(function (a) {
            var chip = document.createElement(a.path ? 'a' : 'span');
            chip.className = 'agentic-attachment-chip';

            if (a.path) {
                chip.href = downloadEndpoint + '/' + a.path.split('/').map(encodeURIComponent).join('/');
                chip.target = '_blank';
                chip.rel = 'noopener';
            }

            var icon = document.createElement('i');
            icon.className = 'fa ' + (iconClass || 'fa-paperclip');
            chip.appendChild(icon);
            chip.appendChild(document.createTextNode(' ' + (a.fileName || 'File')));

            container.appendChild(chip);
        });
    }

    // The row of removable chips shown above the composer for files chosen but not yet sent.
    function renderPendingFiles() {
        pendingFilesEl.innerHTML = '';

        pendingFiles.forEach(function (file, index) {
            var chip = document.createElement('span');
            chip.className = 'agentic-attachment-chip agentic-attachment-chip--pending';

            var icon = document.createElement('i');
            icon.className = 'fa fa-paperclip';
            chip.appendChild(icon);
            chip.appendChild(document.createTextNode(' ' + file.name));

            var remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'agentic-attachment-remove';
            remove.setAttribute('aria-label', 'Remove');
            remove.textContent = '×';
            remove.addEventListener('click', function () {
                pendingFiles.splice(index, 1);
                renderPendingFiles();
            });
            chip.appendChild(remove);

            pendingFilesEl.appendChild(chip);
        });

        pendingFilesEl.style.display = pendingFiles.length > 0 ? 'flex' : 'none';
    }

    function clearPendingFiles() {
        pendingFiles = [];
        renderPendingFiles();
    }

    function ensureToolContainer(message) {
        if (!message.toolBody) {
            var details = document.createElement('details');
            details.className = 'agentic-tool-activity';

            var summary = document.createElement('summary');
            summary.innerHTML = '<i class="fa fa-wrench"></i> Tool Activity';
            details.appendChild(summary);

            var body = document.createElement('div');
            body.className = 'agentic-tool-activity-body';
            details.appendChild(body);

            message.root.appendChild(details);
            message.toolBody = body;
        }

        return message.toolBody;
    }

    // Truncates long text for display, returning both the shown slice and whether it was cut.
    function truncate(text, max) {
        if (text.length <= max) {
            return { text: text, truncated: false };
        }

        return { text: text.substring(0, max), truncated: true };
    }

    // Renders `fullText` into a <pre>, with a "show full" toggle if it was truncated.
    function appendTruncatable(parent, fullText, max) {
        var pre = document.createElement('pre');
        var cut = truncate(fullText, max);
        pre.textContent = cut.text + (cut.truncated ? '…' : '');
        parent.appendChild(pre);

        if (!cut.truncated) {
            return;
        }

        var expanded = false;
        var toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'agentic-tool-expand';
        toggle.textContent = 'Show full';

        toggle.addEventListener('click', function () {
            expanded = !expanded;
            pre.textContent = expanded ? fullText : (cut.text + '…');
            toggle.textContent = expanded ? 'Collapse' : 'Show full';
        });

        parent.appendChild(toggle);
    }

    // Renders one structured tool-activity entry ({type:'tool_use',...} or {type:'tool_result',...})
    // as a card. Used identically for live SSE events and for history reloaded from the DB, so
    // there's exactly one place that decides what a tool call looks like.
    function renderToolEntry(container, entry) {
        if (entry.type === 'tool_use') {
            var card = document.createElement('div');
            card.className = 'agentic-tool-call';

            var header = document.createElement('div');
            header.className = 'agentic-tool-call-header';
            header.innerHTML = '<i class="fa ' + (TOOL_ICONS[entry.tool] || 'fa-wrench') + '"></i> <span>' + entry.tool + '</span>';
            card.appendChild(header);

            var input = entry.input || {};

            if (typeof input.description === 'string') {
                var desc = document.createElement('div');
                desc.className = 'agentic-tool-call-desc';
                desc.textContent = input.description;
                card.appendChild(desc);
            }

            if (typeof input.command === 'string') {
                var cmd = document.createElement('pre');
                cmd.className = 'agentic-tool-call-command';
                cmd.textContent = '$ ' + input.command;
                card.appendChild(cmd);
            }

            Object.keys(input).forEach(function (key) {
                if (key === 'command' || key === 'description') {
                    return;
                }

                var value = input[key];
                var valueText = typeof value === 'string' ? value : JSON.stringify(value);
                var cut = truncate(valueText, 200);

                var row = document.createElement('div');
                row.className = 'agentic-tool-call-field';

                var keyEl = document.createElement('span');
                keyEl.className = 'agentic-tool-call-field-key';
                keyEl.textContent = key + ': ';
                row.appendChild(keyEl);

                var valueEl = document.createElement('span');
                valueEl.className = 'agentic-tool-call-field-value';
                valueEl.textContent = cut.text + (cut.truncated ? '…' : '');
                row.appendChild(valueEl);

                card.appendChild(row);
            });

            container.appendChild(card);
        } else if (entry.type === 'tool_result') {
            var resultBlock = document.createElement('div');
            resultBlock.className = 'agentic-tool-result';

            var label = document.createElement('div');
            label.className = 'agentic-tool-result-label';
            label.innerHTML = '<i class="fa fa-reply"></i> Result';
            resultBlock.appendChild(label);

            appendTruncatable(resultBlock, entry.result || '', 500);

            container.appendChild(resultBlock);
        }
    }

    function autoResize() {
        inputEl.style.height = 'auto';
        inputEl.style.height = Math.min(inputEl.scrollHeight, 200) + 'px';
    }

    function setSending(sending) {
        sendBtn.disabled = sending;
        inputEl.disabled = sending;
    }

    // Parses one raw multi-line SSE chunk (from the fetch()+ReadableStream buffer in sendMessage())
    // into an {eventName, data} pair and applies it - the manual parsing EventSource does natively
    // but which fetch()-based streaming (needed for POST - EventSource is GET-only) has to redo by
    // hand. The EventSource-based reconnect below (see the 'agentRunning' block) doesn't go through
    // this at all - its listeners get eventName/data pre-split by the browser and call applyEvent
    // directly - but both paths end up rendering through the exact same applyEvent, so a live-sent
    // turn and one you reconnect to mid-flight are visually indistinguishable.
    function handleSseEvent(rawEvent, assistantMessage) {
        var eventName = 'message';
        var dataLines = [];

        rawEvent.split('\n').forEach(function (line) {
            if (line.indexOf('event:') === 0) {
                eventName = line.substring(6).trim();
            } else if (line.indexOf('data:') === 0) {
                dataLines.push(line.substring(5).trim());
            }
        });

        applyEvent(eventName, dataLines.join('\n'), assistantMessage);
    }

    function applyEvent(eventName, data, assistantMessage) {
        if (eventName === 'text') {
            clearTyping(assistantMessage.bubble);

            // JSON-encoded server-side (see ClaudeCliSessionService) so embedded newlines/paragraph
            // breaks in the text can't be mistaken for the blank-line SSE event boundary below.
            var textChunk;
            try {
                textChunk = JSON.parse(data);
            } catch (parseError) {
                textChunk = data;
            }

            assistantMessage.bubble.appendChild(document.createTextNode(textChunk));
        } else if (eventName === 'tool_use' || eventName === 'tool_result') {
            clearTyping(assistantMessage.bubble);

            try {
                renderToolEntry(ensureToolContainer(assistantMessage), JSON.parse(data));
            } catch (parseError) {
                renderToolEntry(ensureToolContainer(assistantMessage), { type: eventName, result: data });
            }

            // Re-show the indicator: there's often a gap before the next tool call or the
            // final text while the agent keeps working, and without this it looks hung.
            showTyping(assistantMessage.bubble);
        } else if (eventName === 'usage') {
            var tokens = document.createElement('div');
            tokens.className = 'agentic-message-tokens';
            tokens.textContent = JSON.parse(data) + ' tokens';
            assistantMessage.root.appendChild(tokens);
        } else if (eventName === 'generated_files') {
            var filesDiv = document.createElement('div');
            filesDiv.className = 'agentic-attachments';
            renderAttachmentChips(filesDiv, JSON.parse(data), 'fa-download');
            assistantMessage.root.appendChild(filesDiv);
        } else if (eventName === 'error') {
            clearTyping(assistantMessage.bubble);
            createMessageEl('system').bubble.textContent = data;
        }

        scrollToBottom();
    }

    function sendMessage() {
        var text = inputEl.value.trim();

        if (!text && pendingFiles.length === 0) {
            return;
        }

        var userMessage = createMessageEl('user');
        userMessage.bubble.textContent = text;

        if (pendingFiles.length > 0) {
            var attachDiv = document.createElement('div');
            attachDiv.className = 'agentic-attachments';
            renderAttachmentChips(attachDiv, pendingFiles.map(function (f) { return { fileName: f.name }; }));
            userMessage.root.appendChild(attachDiv);
        }

        var formData = new FormData();
        formData.append('message', text);
        pendingFiles.forEach(function (file) {
            formData.append('files', file);
        });
        clearPendingFiles();

        inputEl.value = '';
        autoResize();
        setSending(true);

        var assistantMessage = createMessageEl('assistant');
        showTyping(assistantMessage.bubble);

        // No Content-Type header here - the browser sets multipart/form-data with the correct
        // boundary itself; setting it manually would drop the boundary and break parsing server-side.
        var headers = {};
        headers[csrfHeader] = csrfToken;

        fetch(chatEndpoint, {
            method: 'POST',
            headers: headers,
            body: formData
        }).then(function (response) {
            var reader = response.body.getReader();
            var decoder = new TextDecoder();
            var buffer = '';

            function pump() {
                return reader.read().then(function (result) {
                    if (result.done) {
                        clearTyping(assistantMessage.bubble);
                        setSending(false);
                        return;
                    }

                    buffer += decoder.decode(result.value, { stream: true });

                    var events = buffer.split('\n\n');
                    buffer = events.pop();

                    events.forEach(function (rawEvent) {
                        if (rawEvent.trim()) {
                            handleSseEvent(rawEvent, assistantMessage);
                        }
                    });

                    return pump();
                });
            }

            return pump();
        }).catch(function (error) {
            clearTyping(assistantMessage.bubble);
            createMessageEl('system').bubble.textContent = 'Connection error: ' + error;
            setSending(false);
        });
    }

    sendBtn.addEventListener('click', sendMessage);

    inputEl.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    inputEl.addEventListener('input', autoResize);

    attachBtn.addEventListener('click', function () {
        fileInputEl.click();
    });

    fileInputEl.addEventListener('change', function () {
        Array.prototype.forEach.call(fileInputEl.files, function (file) {
            pendingFiles.push(file);
        });
        fileInputEl.value = ''; // reset so picking the same file again later still fires 'change'
        renderPendingFiles();
    });

    // Server-rendered history only embeds each message's attachments as a raw JSON string (see
    // agent_chat.html) - render the same chips used right after sending, right here, so there's
    // one formatting implementation instead of duplicating it in Thymeleaf.
    document.querySelectorAll('.agentic-attachments[data-attachments]').forEach(function (el) {
        try {
            renderAttachmentChips(el, JSON.parse(el.dataset.attachments));
        } catch (parseError) {
            // malformed - leave the (empty) placeholder rather than losing the message
        }
    });

    document.querySelectorAll('.agentic-generated-files[data-generated-files]').forEach(function (el) {
        try {
            renderAttachmentChips(el, JSON.parse(el.dataset.generatedFiles), 'fa-download');
        } catch (parseError) {
            // malformed - leave the (empty) placeholder rather than losing the message
        }
    });

    // Server-rendered history only embeds each message's tool activity as a raw JSON string
    // (see agent_chat.html) - render it into the same cards used for live streaming, right here,
    // so there's one formatting implementation instead of duplicating it in Thymeleaf.
    document.querySelectorAll('.agentic-tool-activity-body[data-tool-activity]').forEach(function (body) {
        try {
            JSON.parse(body.dataset.toolActivity).forEach(function (entry) {
                renderToolEntry(body, entry);
            });
        } catch (parseError) {
            // Pre-existing rows saved before this format changed (or anything malformed) - show
            // as-is rather than losing the data.
            var pre = document.createElement('pre');
            pre.textContent = body.dataset.toolActivity;
            body.appendChild(pre);
        }
    });

    // If this agent had a turn already running when the page loaded (see AgentController.chat()'s
    // partialText/partialToolActivity and the '#agenticInProgressMessage' block in agent_chat.html),
    // catch up on it: show the busy indicator and keep appending everything still to come, exactly
    // like a turn started from this page - see EventSource below. Without this, navigating to
    // another agent and back used to silently lose both the busy indicator and any not-yet-persisted
    // output, even though the turn itself kept running server-side the whole time.
    if (root.dataset.agentRunning === 'true') {
        var inProgressEl = document.getElementById('agenticInProgressMessage');

        var assistantMessage = inProgressEl
            ? {
                root: inProgressEl,
                bubble: inProgressEl.querySelector('.agentic-bubble'),
                // Already populated by the history-hydration loop above if the server sent partial
                // tool activity - reusing it here (instead of leaving toolBody unset) stops
                // ensureToolContainer from creating a second, empty <details> block alongside it.
                toolBody: inProgressEl.querySelector('.agentic-tool-activity-body')
            }
            : createMessageEl('assistant');

        showTyping(assistantMessage.bubble);
        setSending(true);

        // GET, unlike the send flow's POST - EventSource can't do POST, but that's fine here since
        // this connection only ever subscribes to an already-running turn, never starts one.
        var stream = new EventSource(root.dataset.streamEndpoint);

        ['text', 'tool_use', 'tool_result', 'usage', 'generated_files'].forEach(function (name) {
            stream.addEventListener(name, function (e) {
                applyEvent(name, e.data, assistantMessage);
            });
        });

        // EventSource's own 'error' fires both for a genuine connection problem and for the normal
        // "server closed the stream" case (how a completed/failed turn ends this connection) - either
        // way there's nothing more coming, so just stop showing busy and let the next page load (or
        // the already-persisted AgentMessage on a future reload) reflect the final state. Not
        // attempting to special-case a real mid-turn error here (unlike the send flow's own 'error'
        // SSE event, which does): the SYSTEM error message is already persisted server-side by
        // executeTurn regardless, this connection existing purely to catch up on an in-flight turn.
        stream.addEventListener('error', function () {
            clearTyping(assistantMessage.bubble);
            setSending(false);
            stream.close();
        });
    }

    // Per-message delete (the small trash icon revealed on hover, see .agentic-msg-delete in
    // agentic-chat.css) - event delegation on the container, same pattern as the sidebar's stop
    // button below, since messages are added dynamically (sendMessage()) as well as server-rendered.
    messagesEl.addEventListener('click', function (e) {
        var deleteBtn = e.target.closest('.agentic-msg-delete');

        if (!deleteBtn) {
            return;
        }

        e.preventDefault();

        if (!window.confirm('Diese Nachricht löschen?')) {
            return;
        }

        var msgEl = deleteBtn.closest('.agentic-msg');
        var headers = {};
        headers[csrfHeader] = csrfToken;

        fetch(deleteBtn.dataset.deleteEndpoint, { method: 'POST', headers: headers })
            .then(function (response) { return response.ok ? response.json() : { deleted: false }; })
            .then(function (result) {
                if (result.deleted && msgEl) {
                    msgEl.remove();
                }
            })
            .catch(function () {
                // best-effort - leave the message in place, user can retry
            });
    });

    scrollToBottom();
    autoResize();
})();

/*
 * Sidebar live-status polling: shows a pulsing dot + tooltip on any agent that's currently
 * working (thinking / tool / etc.), even for agents you're not actively chatting with. Separate
 * IIFE from the chat client above since it only needs the sidebar, not #agenticChatRoot.
 */
(function () {
    'use strict';

    var sidebar = document.querySelector('.agentic-sidebar');

    if (!sidebar) {
        return;
    }

    // Mobile: sidebar starts as a closed slide-in overlay (see the max-width:768px block in
    // agentic-chat.css) - the hamburger button opens it, the backdrop or Escape closes it. No
    // open/closed state persisted across page loads: every agent switch is a full navigation
    // (not a SPA), so "start closed on the new page" is already the right default, same as most
    // mobile chat apps landing on the conversation rather than the list.
    var sidebarToggle = document.getElementById('agenticSidebarToggle');
    var backdrop = document.getElementById('agenticBackdrop');

    function closeSidebar() {
        sidebar.classList.remove('agentic-sidebar--open');

        if (backdrop) {
            backdrop.classList.remove('agentic-backdrop--visible');
        }
    }

    if (sidebarToggle && backdrop) {
        sidebarToggle.addEventListener('click', function () {
            sidebar.classList.add('agentic-sidebar--open');
            backdrop.classList.add('agentic-backdrop--visible');
        });

        backdrop.addEventListener('click', closeSidebar);

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeSidebar();
            }
        });
    }

    var statusEndpoint = sidebar.dataset.statusEndpoint;
    var csrfHeader = sidebar.dataset.csrfHeader;
    var csrfToken = sidebar.dataset.csrfToken;
    var POLL_INTERVAL_MS = 2500;

    function applyStatuses(statuses) {
        var items = sidebar.querySelectorAll('.agentic-sidebar-item[data-agent-id]');

        items.forEach(function (item) {
            var statusText = statuses[item.dataset.agentId];
            var statusEl = item.querySelector('.agentic-sidebar-item-status');

            if (statusText) {
                item.classList.add('agentic-sidebar-item--running');
                statusEl.title = statusText;
            } else {
                item.classList.remove('agentic-sidebar-item--running');
                statusEl.title = '';
            }
        });
    }

    function poll() {
        fetch(statusEndpoint)
            .then(function (response) { return response.ok ? response.json() : {}; })
            .then(applyStatuses)
            .catch(function () {
                // transient polling error - just wait for the next tick
            });
    }

    sidebar.addEventListener('click', function (e) {
        var stopBtn = e.target.closest('.agentic-sidebar-item-stop');

        if (!stopBtn) {
            return;
        }

        e.preventDefault();

        var headers = {};
        headers[csrfHeader] = csrfToken;

        fetch(stopBtn.dataset.stopEndpoint, { method: 'POST', headers: headers })
            .then(poll)
            .catch(function () {
                // best-effort - the next scheduled poll will reconcile the sidebar either way
            });
    });

    poll();
    setInterval(poll, POLL_INTERVAL_MS);
})();
