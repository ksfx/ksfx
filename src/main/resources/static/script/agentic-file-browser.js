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

    var body = document.getElementById('agenticFileBrowserBody');
    var openBtn = document.getElementById('agenticFilesBtn');
    var closeBtn = document.getElementById('agenticFbCloseBtn');
    var uploadBtn = document.getElementById('agenticFbUploadBtn');
    var uploadInput = document.getElementById('agenticFbUploadInput');
    var mkdirBtn = document.getElementById('agenticFbMkdirBtn');

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

    function postAction(action, params, formData) {
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

        fetch(url, options)
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
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

    // Navigation, rename and delete are per-row controls inside the swapped fragment, so one
    // delegated listener on the stable overlay body instead of rebinding after every swap.
    body.addEventListener('click', function (event) {
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
