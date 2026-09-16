/*
 * File browser overlay for the "Agentic" feature (see AgenticFileBrowserController). Every action
 * - navigate, upload, mkdir, rename, delete - is one request that comes back as the refreshed
 * listing fragment for the current directory, which this script swaps into the overlay body
 * wholesale. All navigation state (root, current path) lives in the fragment's data-* attributes,
 * so this script is stateless between swaps. Kept separate from agentic-chat.js on purpose: the
 * chat client works unchanged without this file.
 */
(function () {
    'use strict';

    var overlay = document.getElementById('agenticFileBrowser');

    if (!overlay) {
        return;
    }

    var filesEndpoint = overlay.dataset.filesEndpoint;
    var csrfHeader = overlay.dataset.csrfHeader;
    var csrfToken = overlay.dataset.csrfToken;
    var toastuiJs = overlay.dataset.toastuiJs;
    var toastuiCss = overlay.dataset.toastuiCss;

    var body = document.getElementById('agenticFileBrowserBody');
    var openBtn = document.getElementById('agenticFilesBtn');
    var closeBtn = document.getElementById('agenticFbCloseBtn');
    var uploadBtn = document.getElementById('agenticFbUploadBtn');
    var uploadInput = document.getElementById('agenticFbUploadInput');
    var mkdirBtn = document.getElementById('agenticFbMkdirBtn');
    var newMdBtn = document.getElementById('agenticFbNewMdBtn');

    function currentListing() {
        return document.getElementById('agenticFbListing');
    }

    // The "|| fallback"s aren't just paranoia: Thymeleaf drops data-* attributes whose value is
    // the empty string, so at the root directory the fragment carries no data-path at all and
    // dataset.path is undefined - which would otherwise get sent as the literal string
    // "undefined" in the next request's path parameter.
    function currentRoot() {
        var listing = currentListing();
        return (listing && listing.dataset.root) || 'workspace';
    }

    function currentPath() {
        var listing = currentListing();
        return (listing && listing.dataset.path) || '';
    }

    function swapListing(html) {
        body.innerHTML = html;
    }

    // Client-side only, against whatever's currently rendered - the write endpoint itself happily
    // overwrites an existing file (that's exactly what Save on an already-open file needs to do),
    // so "New .md" checks this first rather than risk silently wiping an existing file's content
    // just because the user typed a name that happens to collide with one already in this directory.
    function nameExistsInCurrentListing(name) {
        var nameSpans = body.querySelectorAll('.agentic-filebrowser-cell-name span');

        for (var i = 0; i < nameSpans.length; i++) {
            if (nameSpans[i].textContent === name) {
                return true;
            }
        }

        return false;
    }

    function showError(message) {
        alert('File browser: ' + message);
    }

    // On failure the controller's IllegalArgumentException handler puts the actual reason in a
    // plain-text 400 body - include it in the alert so errors are diagnosable without server logs.
    function failWithBody(response) {
        return response.text().then(function (bodyText) {
            throw new Error('HTTP ' + response.status + (bodyText ? ' - ' + bodyText.substring(0, 200) : ''));
        });
    }

    function loadListing(root, path) {
        var url = filesEndpoint + '/list?root=' + encodeURIComponent(root) + '&path=' + encodeURIComponent(path);

        fetch(url, { headers: { 'X-Requested-With': 'fetch' } })
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(swapListing)
            .catch(function (error) { showError(error.message); });
    }

    // Returns the raw response text (the refreshed listing fragment) without swapping it in - lets
    // a caller that wants different follow-up behavior (createMarkdownFile jumps into the editor
    // instead) reuse the same request/CSRF/error plumbing as postAction below.
    function postActionRaw(action, params, formData) {
        var url = filesEndpoint + '/' + action;
        var options = { method: 'POST', headers: {} };
        options.headers[csrfHeader] = csrfToken;

        if (formData) {
            Object.keys(params).forEach(function (key) { formData.append(key, params[key]); });
            options.body = formData;
        } else {
            options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
            options.body = Object.keys(params).map(function (key) {
                return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
            }).join('&');
        }

        return fetch(url, options).then(function (response) {
            if (!response.ok) { return failWithBody(response); }
            return response.text();
        });
    }

    function postAction(action, params, formData) {
        postActionRaw(action, params, formData)
            .then(swapListing)
            .catch(function (error) { showError(error.message); });
    }

    openBtn.addEventListener('click', function () {
        overlay.hidden = false;
        loadListing('workspace', '');
    });

    closeBtn.addEventListener('click', function () {
        overlay.hidden = true;
    });

    // Click on the dark backdrop area (not the dialog itself) closes too.
    overlay.addEventListener('click', function (event) {
        if (event.target === overlay) {
            overlay.hidden = true;
        }
    });

    uploadBtn.addEventListener('click', function () {
        uploadInput.click();
    });

    uploadInput.addEventListener('change', function () {
        if (!uploadInput.files.length) {
            return;
        }

        var formData = new FormData();

        Array.prototype.forEach.call(uploadInput.files, function (file) {
            formData.append('files', file);
        });

        uploadInput.value = '';

        postAction('upload', { root: currentRoot(), path: currentPath() }, formData);
    });

    mkdirBtn.addEventListener('click', function () {
        var name = prompt('New folder name:');

        if (!name || !name.trim()) {
            return;
        }

        postAction('mkdir', { root: currentRoot(), path: currentPath(), name: name.trim() });
    });

    newMdBtn.addEventListener('click', function () {
        var name = prompt('New markdown file name:');

        if (!name || !name.trim()) {
            return;
        }

        var fileName = name.trim();

        if (!/\.md$/i.test(fileName)) {
            fileName += '.md';
        }

        if (nameExistsInCurrentListing(fileName)) {
            alert('A file named "' + fileName + '" already exists here.');
            return;
        }

        createMarkdownFile(fileName);
    });

    // Lazily loads the vendored (see static/vendor/toastui-editor/) WYSIWYG markdown editor only
    // when a .md file is actually opened - most file-browser sessions never touch it, no reason to
    // ship ~500KB on every chat page load. Cached as a promise so re-opening a second file doesn't
    // re-fetch/re-inject the same <link>/<script> tags.
    var toastuiReady = null;

    function loadToastUiAssets() {
        if (toastuiReady) {
            return toastuiReady;
        }

        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = toastuiCss;
        document.head.appendChild(link);

        toastuiReady = new Promise(function (resolve, reject) {
            var script = document.createElement('script');
            script.src = toastuiJs;
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load the markdown editor.')); };
            document.head.appendChild(script);
        });

        return toastuiReady;
    }

    // Creates an empty file via the same /write endpoint Save already uses (it creates-or-
    // overwrites unconditionally - nameExistsInCurrentListing's check above is what actually
    // prevents this from clobbering an existing file), then jumps straight into the editor for it
    // instead of dropping back to the listing - one less click than "create, then find it, then
    // click Edit."
    function createMarkdownFile(name) {
        var root = currentRoot();
        var path = currentPath();

        postActionRaw('write', { root: root, path: path, name: name, content: '' })
            .then(function () { openEditor(name); })
            .catch(function (error) { showError(error.message); });
    }

    var activeEditor = null;

    function openEditor(name) {
        var root = currentRoot();
        var dirPath = currentPath();
        var filePath = dirPath ? dirPath + '/' + name : name;
        var downloadUrl = filesEndpoint + '/download?root=' + encodeURIComponent(root) + '&path=' + encodeURIComponent(filePath);

        Promise.all([loadToastUiAssets(), fetch(downloadUrl).then(function (r) {
            if (!r.ok) { return failWithBody(r); }
            return r.text();
        })]).then(function (results) {
            var content = results[1];

            body.innerHTML = '';

            var toolbar = document.createElement('div');
            toolbar.className = 'agentic-filebrowser-editor-toolbar';

            var title = document.createElement('span');
            title.className = 'agentic-filebrowser-editor-title';
            title.textContent = name;

            var saveBtn = document.createElement('button');
            saveBtn.type = 'button';
            saveBtn.className = 'agentic-filebrowser-btn';
            saveBtn.textContent = 'Save';

            var cancelBtn = document.createElement('button');
            cancelBtn.type = 'button';
            cancelBtn.className = 'agentic-filebrowser-btn';
            cancelBtn.textContent = 'Cancel';

            toolbar.appendChild(title);
            toolbar.appendChild(saveBtn);
            toolbar.appendChild(cancelBtn);

            var mount = document.createElement('div');
            mount.id = 'agenticFbEditorMount';

            body.appendChild(toolbar);
            body.appendChild(mount);

            activeEditor = new toastui.Editor({
                el: mount,
                height: '420px',
                initialEditType: 'wysiwyg',
                previewStyle: 'tab',
                initialValue: content
            });

            saveBtn.addEventListener('click', function () {
                var markdown = activeEditor.getMarkdown();
                activeEditor = null;
                postAction('write', { root: root, path: dirPath, name: name, content: markdown });
            });

            cancelBtn.addEventListener('click', function () {
                activeEditor = null;
                loadListing(root, dirPath);
            });
        }).catch(function (error) {
            showError(error.message);
        });
    }

    // Navigation, rename and delete are per-row controls inside the swapped fragment, so one
    // delegated listener on the stable overlay body instead of rebinding after every swap.
    body.addEventListener('click', function (event) {
        var editEl = event.target.closest('[data-fb-edit]');

        if (editEl) {
            openEditor(editEl.dataset.fbEdit);
            return;
        }

        var upEl = event.target.closest('.agentic-filebrowser-up');

        if (upEl) {
            var path = currentPath();
            var lastSlash = path.lastIndexOf('/');
            loadListing(currentRoot(), lastSlash < 0 ? '' : path.substring(0, lastSlash));
            return;
        }

        var navEl = event.target.closest('[data-fb-nav]');

        if (navEl) {
            event.preventDefault();
            loadListing(currentRoot(), navEl.dataset.fbNav);
            return;
        }

        var tabEl = event.target.closest('[data-fb-root]');

        if (tabEl) {
            loadListing(tabEl.dataset.fbRoot, '');
            return;
        }

        var renameEl = event.target.closest('[data-fb-rename]');

        if (renameEl) {
            var oldName = renameEl.dataset.fbRename;
            var newName = prompt('Rename "' + oldName + '" to:', oldName);

            if (!newName || !newName.trim() || newName.trim() === oldName) {
                return;
            }

            postAction('rename', { root: currentRoot(), path: currentPath(), name: oldName, newName: newName.trim() });
            return;
        }

        var deleteEl = event.target.closest('[data-fb-delete]');

        if (deleteEl) {
            var name = deleteEl.dataset.fbDelete;
            var isDir = deleteEl.dataset.fbDeleteDir === 'true';
            var question = isDir
                ? 'Delete folder "' + name + '" and ALL of its contents?'
                : 'Delete "' + name + '"?';

            if (!confirm(question)) {
                return;
            }

            postAction('delete', { root: currentRoot(), path: currentPath(), name: name });
        }
    });
})();
