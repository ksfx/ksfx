/*
 * Live editing of an existing wiki page together with others (branch multiwiki) - the browser side
 * of WikiCollabService. The document is a Yjs CRDT shown in a Milkdown editor (vendored bundle
 * collab-editor.min.js, window.KsfxCollab); the server only relays and remembers:
 *
 * - join:  the server says whether THIS client fills the shared document from the saved Markdown
 *          (exactly one client per session) and hands over every update so far,
 * - push:  our Yjs updates (batched), our awareness (name, colour, cursor) and a Markdown snapshot
 *          (debounced) - the snapshot is what agents read and what is saved if everybody leaves,
 * - poll:  long-polling (answers as soon as something is new, else after 25 s) - works through any
 *          proxy, incl. the web isolation that buffers streams,
 * - leave: sendBeacon on unload.
 *
 * Agents write through their own API; while a session runs, their write arrives here (only at the
 * leader, the longest-present client) and is merged three-way into the live document.
 * "Save" stores the current state as a new version via the normal form, as before.
 *
 * A NEW page uses the same editor alone (no collab base URL): a local Yjs document, no server
 * session - the page gets one once it exists, from its first save on.
 */
(function () {
    'use strict';

    var mount = document.getElementById('wikiCollabMount');

    if (!mount) {
        return;
    }

    var base = mount.dataset.collabBase;
    var solo = !base; // new page - nothing to share yet
    var csrfHeader = mount.dataset.csrfHeader;
    var csrfParam = mount.dataset.csrfParam;
    var csrfToken = mount.dataset.csrfToken;
    var statusEl = document.getElementById('wikiCollabStatus');
    var peopleEl = document.getElementById('wikiCollabPeople');
    var form = document.getElementById('wikiEditForm');
    var contentField = document.getElementById('wikiEditContent');
    var saveBtn = document.getElementById('wikiEditSaveBtn');

    var K, ydoc, awareness, editor;
    var clientId;
    var index = 0, awarenessSince = 0, externalSince = 0;
    var pendingUpdates = [];
    var pendingAwareness = null;
    var pendingMarkdown = null;
    var pushTimer = null, markdownTimer = null;
    var stopped = false;
    var snapshotStale = false;

    function setStatus(text, kind) {
        if (!statusEl) {
            return; // new page: no live status bar
        }

        statusEl.textContent = text;
        statusEl.className = 'wiki-collab-status' + (kind ? ' wiki-collab-status--' + kind : '');
    }

    // Images (pasted, dropped, or via the toolbar) are uploaded as wiki assets first - the same
    // endpoint the old editor used - and only their URL goes into the document.
    function uploadFile(file) {
        var formData = new FormData();
        formData.append('file', file, file.name || 'pasted-image.png');

        var headers = {};
        headers[csrfHeader] = csrfToken;
        setStatus('uploading image…', 'warn');

        return fetch(mount.dataset.assetUploadEndpoint, { method: 'POST', credentials: 'same-origin', headers: headers, body: formData })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (result) {
                setStatus(solo ? '' : 'live', solo ? '' : 'ok');
                return result.url;
            })
            .catch(function (error) {
                setStatus('image upload failed', 'error');
                alert('Image upload failed: ' + error.message);
                throw error;
            });
    }

    function loadAssets() {
        return new Promise(function (resolve, reject) {
            var link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = mount.dataset.collabCss;
            document.head.appendChild(link);

            var script = document.createElement('script');
            script.src = mount.dataset.collabJs;
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load the editor.')); };
            document.head.appendChild(script);
        });
    }

    function post(path, body) {
        var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        headers[csrfHeader] = csrfToken;

        return fetch(base + path, { method: 'POST', credentials: 'same-origin', headers: headers, body: JSON.stringify(body) })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            });
    }

    // ---- Sending -------------------------------------------------------------------------------

    function schedulePush() {
        if (!pushTimer) {
            pushTimer = setTimeout(flush, 120);
        }
    }

    function flush() {
        pushTimer = null;

        if (stopped || (!pendingUpdates.length && !pendingAwareness && pendingMarkdown === null)) {
            return;
        }

        var body = { clientId: clientId, seen: index, updates: pendingUpdates, awareness: pendingAwareness, markdown: pendingMarkdown };
        pendingUpdates = [];
        pendingAwareness = null;
        pendingMarkdown = null;

        post('/push', body).then(function (answer) {
            if (answer.ended) {
                rejoin();
                return;
            }

            if (answer.inSync) {
                // Nobody else wrote in between: we have everything up to here, incl. our own updates.
                index = Math.max(index, answer.index);
            } else if (body.markdown !== null) {
                // Someone else's changes are on their way to us - send the snapshot again once they
                // are in (next poll), and in any case shortly, in case that poll already came back.
                snapshotStale = true;
                setTimeout(function () {
                    if (snapshotStale && editor) {
                        snapshotStale = false;
                        onMarkdownChange(editor.getMarkdown());
                    }
                }, 600);
            }
        }).catch(function () {
            // Put it back and try again shortly - nothing typed may get lost.
            pendingUpdates = body.updates.concat(pendingUpdates);
            pendingAwareness = pendingAwareness || body.awareness;
            pendingMarkdown = pendingMarkdown !== null ? pendingMarkdown : body.markdown;
            setStatus('connection problem - retrying', 'warn');
            setTimeout(schedulePush, 2000);
        });
    }

    function onMarkdownChange(markdown) {
        clearTimeout(markdownTimer);
        markdownTimer = setTimeout(function () {
            pendingMarkdown = markdown;
            schedulePush();
        }, 1200);
    }

    // ---- Receiving -----------------------------------------------------------------------------

    function poll() {
        if (stopped) {
            return;
        }

        var url = base + '/poll?clientId=' + clientId + '&since=' + index + '&awarenessSince=' + awarenessSince + '&externalSince=' + externalSince;

        fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (answer) {
                if (answer.ended) {
                    rejoin();
                    return;
                }

                apply(answer);
                setStatus('live', 'ok');
                poll();
            })
            .catch(function () {
                setStatus('connection problem - retrying', 'warn');
                setTimeout(poll, 3000);
            });
    }

    function apply(answer) {
        (answer.updates || []).forEach(function (update) {
            K.Y.applyUpdate(ydoc, K.fromBase64(update), 'remote');
        });
        index = Math.max(index, answer.index);

        if (snapshotStale && editor) {
            snapshotStale = false;
            onMarkdownChange(editor.getMarkdown());
        }

        (answer.awareness || []).forEach(function (state) {
            K.applyAwarenessUpdate(awareness, K.fromBase64(state.update), 'remote');
        });

        if (answer.gone && answer.gone.length) {
            K.removeAwarenessStates(awareness, answer.gone, 'remote');
        }

        awarenessSince = answer.awarenessVersion;
        renderPeople(answer.present || []);

        (answer.externals || []).forEach(mergeExternal);
    }

    function renderPeople(present) {
        peopleEl.innerHTML = '';

        present.forEach(function (person) {
            var chip = document.createElement('span');
            chip.className = 'wiki-collab-person' + (person.clientId === clientId ? ' wiki-collab-person--me' : '');
            chip.style.borderColor = person.color;
            chip.title = person.clientId === clientId ? 'You' : person.name + ' is editing';

            var dot = document.createElement('span');
            dot.className = 'wiki-collab-person-dot';
            dot.style.background = person.color;
            chip.appendChild(dot);
            chip.appendChild(document.createTextNode(person.clientId === clientId ? person.name + ' (you)' : person.name));
            peopleEl.appendChild(chip);
        });
    }

    // An agent wrote the page while we edit it: merge its change into the live document - against
    // what the agent had read, so only what IT changed is applied, never a blind overwrite.
    function mergeExternal(external) {
        externalSince = Math.max(externalSince, external.id);

        var ours = editor.getMarkdown();
        var merged = K.mergeMarkdown(external.base || '', ours, external.theirs || '');

        if (merged.markdown !== ours) {
            editor.replaceAll(merged.markdown);
        }

        post('/external-applied', { clientId: clientId, externalId: external.id, markdown: merged.markdown })
            .catch(function () { /* the server saves the agent's version itself after a timeout */ });

        notify(external.agentName + ' edited this page' + (merged.conflicts
            ? ' - ' + merged.conflicts + ' conflicting passage(s) kept as they were; the agent\'s full version is in the history'
            : ' - merged in'));
    }

    function notify(text) {
        var note = document.createElement('div');
        note.className = 'wiki-collab-note';
        note.textContent = text;
        var toolbar = document.getElementById('wikiCollabToolbar');
        toolbar.parentNode.insertBefore(note, toolbar);
        setTimeout(function () { note.remove(); }, 8000);
    }

    // ---- Lifecycle -----------------------------------------------------------------------------

    function join() {
        return post('/join', { clientId: clientId }).then(function (answer) {
            (answer.updates || []).forEach(function (update) {
                K.Y.applyUpdate(ydoc, K.fromBase64(update), 'remote');
            });
            index = answer.index;

            awareness.setLocalStateField('user', { name: answer.userLabel, color: answer.color });

            return answer;
        });
    }

    // The session ended under us (server restart, long sleep): start a fresh one with a fresh document.
    function rejoin() {
        stopped = true;
        setStatus('session ended - reloading…', 'warn');
        window.location.reload();
    }

    function start() {
        K = window.KsfxCollab;
        ydoc = new K.Y.Doc();
        clientId = ydoc.clientID;
        awareness = new K.Awareness(ydoc);

        if (solo) {
            return K.createEditor({ el: mount, ydoc: ydoc, awareness: awareness, initialMarkdown: contentField.value || '', uploadFile: uploadFile })
                .then(function (created) { editor = created; });
        }

        ydoc.on('update', function (update, origin) {
            if (origin !== 'remote') {
                pendingUpdates.push(K.toBase64(update));
                schedulePush();
            }
        });

        awareness.on('update', function (changes, origin) {
            if (origin !== 'remote') {
                pendingAwareness = K.toBase64(K.encodeAwarenessUpdate(awareness, [ydoc.clientID]));
                schedulePush();
            }
        });

        return join().then(function (answer) {
            return K.createEditor({
                el: mount,
                ydoc: ydoc,
                awareness: awareness,
                initialMarkdown: answer.initialize ? (answer.markdown || '') : null,
                onChange: onMarkdownChange,
                uploadFile: uploadFile
            });
        }).then(function (created) {
            editor = created;
            setStatus('live', 'ok');
            poll();
        });
    }

    document.getElementById('wikiCollabToolbar').addEventListener('mousedown', function (event) {
        var button = event.target.closest('button[data-cmd]');

        if (button && editor) {
            event.preventDefault(); // keep the selection in the editor

            if (button.dataset.cmd === 'image') {
                imageInput.click();
            } else {
                editor.command(button.dataset.cmd);
            }
        }
    });

    var imageInput = document.getElementById('wikiCollabImageInput');

    imageInput.addEventListener('change', function () {
        var file = imageInput.files[0];
        imageInput.value = '';

        if (file && editor) {
            editor.insertImageFile(file).catch(function () { /* reported in uploadFile */ });
        }
    });

    // Save and leave the editor - the button below the editor and the same one above it.
    function saveAndClose() {
        if (!form.reportValidity() || !editor) {
            return;
        }

        stopped = true;
        contentField.value = editor.getMarkdown();
        form.submit();
    }

    // Ctrl+S / Cmd+S: save a version and stay in the editor (the live session goes on). Posts the
    // same form in the background; the server answers with a redirect - to the page on success,
    // back to /edit (with the reason) when the save was refused, e.g. without a title.
    var saving = false;

    function saveAndStay() {
        if (solo) {
            saveAndClose(); // a new page is created by its first save - that leaves the editor once
            return;
        }

        if (!form.reportValidity() || !editor || saving) {
            return;
        }

        saving = true;
        contentField.value = editor.getMarkdown();
        setStatus('saving…', 'warn');

        fetch(form.action, { method: 'POST', credentials: 'same-origin', body: new FormData(form) })
            .then(function (response) {
                if (!response.ok || /\/edit(\?|$)/.test(response.url)) {
                    throw new Error('not saved - check the title and the parent page');
                }

                var now = new Date();
                setStatus('saved ' + ('0' + now.getHours()).slice(-2) + ':' + ('0' + now.getMinutes()).slice(-2), 'ok');
                setTimeout(function () { setStatus('live', 'ok'); }, 4000);
            })
            .catch(function (error) {
                setStatus('save failed', 'error');
                notify('Save failed: ' + error.message);
            })
            .then(function () {
                saving = false;
            });
    }

    saveBtn.addEventListener('click', saveAndClose);
    document.getElementById('wikiCollabSaveTop').addEventListener('click', saveAndClose);

    // Capture phase: before the editor (or the browser's own "save page as") sees the keystroke.
    document.addEventListener('keydown', function (event) {
        if ((event.ctrlKey || event.metaKey) && !event.altKey && (event.key === 's' || event.key === 'S')) {
            event.preventDefault();
            saveAndStay();
        }
    }, true);

    window.addEventListener('pagehide', function () {
        stopped = true;

        if (!solo && clientId && navigator.sendBeacon) {
            var data = new URLSearchParams();
            data.append('clientId', clientId);
            data.append(csrfParam, csrfToken);
            navigator.sendBeacon(base + '/leave', data);
        }
    });

    loadAssets().then(start).catch(function (error) {
        setStatus('could not start the live editor: ' + error.message, 'error');
    });
})();
