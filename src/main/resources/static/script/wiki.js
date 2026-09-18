/*
 * Wiki pages: renders markdown read-only (wiki_page.html) and drives the WYSIWYG editor
 * (wiki_edit.html), both via the same vendored Toast UI Editor bundle already used by the Agentic
 * file browser (see agentic-file-browser.js) - Editor.factory({viewer:true}) is Toast UI's own
 * read-only mode, so this is one rendering codepath and one vendored asset for both views, no new
 * dependency for markdown-to-HTML.
 */
(function () {
    'use strict';

    var toastuiReady = null;

    function loadToastUiAssets(jsUrl, cssUrl) {
        if (toastuiReady) {
            return toastuiReady;
        }

        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = cssUrl;
        document.head.appendChild(link);

        toastuiReady = new Promise(function (resolve, reject) {
            var script = document.createElement('script');
            script.src = jsUrl;
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load markdown renderer.')); };
            document.head.appendChild(script);
        });

        return toastuiReady;
    }

    // --- Read-only page view -------------------------------------------------------------------
    var viewerMount = document.getElementById('wikiPageViewer');

    if (viewerMount) {
        var markdown = document.getElementById('wikiPageMarkdown').value;

        loadToastUiAssets(viewerMount.dataset.toastuiJs, viewerMount.dataset.toastuiCss).then(function () {
            toastui.Editor.factory({
                el: viewerMount,
                viewer: true,
                initialValue: markdown
            });
        }).catch(function (error) {
            viewerMount.textContent = markdown;
            console.error(error);
        });
    }

    // --- Editor ----------------------------------------------------------------------------------
    var editorMount = document.getElementById('wikiEditorMount');

    if (editorMount) {
        var form = document.getElementById('wikiEditForm');
        var contentField = document.getElementById('wikiEditContent');
        var saveBtn = document.getElementById('wikiEditSaveBtn');
        var assetUploadEndpoint = editorMount.dataset.assetUploadEndpoint;
        var csrfHeader = editorMount.dataset.csrfHeader;
        var csrfToken = editorMount.dataset.csrfToken;
        var editor = null;

        loadToastUiAssets(editorMount.dataset.toastuiJs, editorMount.dataset.toastuiCss).then(function () {
            editor = new toastui.Editor({
                el: editorMount,
                height: '520px',
                initialEditType: 'wysiwyg',
                previewStyle: 'tab',
                initialValue: contentField.value,
                hooks: {
                    // Toast UI calls this with the pasted/dropped image blob and a callback expecting
                    // (url, altText) - upload it as a WikiAsset and hand back the URL it's served from
                    // (WikiController.asset), instead of Toast UI's default of inlining the image as a
                    // base64 data URL directly into the markdown.
                    addImageBlobHook: function (blob, callback) {
                        var formData = new FormData();
                        formData.append('file', blob, blob.name || 'pasted-image.png');

                        var options = { method: 'POST', headers: {}, body: formData };
                        options.headers[csrfHeader] = csrfToken;

                        fetch(assetUploadEndpoint, options)
                            .then(function (response) { return response.json(); })
                            .then(function (result) { callback(result.url, blob.name || 'image'); })
                            .catch(function (error) { alert('Image upload failed: ' + error.message); });

                        return false;
                    }
                }
            });
        }).catch(function (error) {
            editorMount.textContent = 'Could not load the editor: ' + error.message;
        });

        saveBtn.addEventListener('click', function () {
            if (!form.reportValidity()) {
                return;
            }

            contentField.value = editor ? editor.getMarkdown() : contentField.value;
            form.submit();
        });

        // Rare action, so hidden by default - see the wiki-move comment in wiki_edit.html.
        var moveToggle = document.getElementById('wikiMoveToggle');

        if (moveToggle) {
            moveToggle.addEventListener('click', function (event) {
                event.preventDefault();
                moveToggle.hidden = true;
                document.getElementById('wikiMoveField').hidden = false;
            });
        }
    }

    // --- Page attachments (see wiki_page.html's wiki-attachments section) ------------------------
    var attachmentInput = document.getElementById('wikiAttachmentInput');

    if (attachmentInput) {
        // Picking files IS the upload - the form submits immediately on selection.
        attachmentInput.addEventListener('change', function () {
            if (attachmentInput.files.length > 0) {
                attachmentInput.form.submit();
            }
        });
    }

    document.querySelectorAll('.wiki-attachment-delete-form').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('Delete attachment "' + form.dataset.filename + '"?')) {
                event.preventDefault();
            }
        });
    });

    // --- Wiki switcher (see wiki_tree.html :: switcher) -----------------------------------------
    var switcherSelect = document.getElementById('wikiSwitcherSelect');

    if (switcherSelect) {
        switcherSelect.addEventListener('change', function () {
            window.location.href = '/wiki/' + switcherSelect.value + '/';
        });
    }

    var newWikiBtn = document.getElementById('wikiSwitcherNewBtn');

    if (newWikiBtn) {
        newWikiBtn.addEventListener('click', function () {
            var name = window.prompt('Name for the new wiki:');

            if (!name || !name.trim()) {
                return;
            }

            document.getElementById('wikiCreateNameInput').value = name.trim();
            document.getElementById('wikiCreateForm').submit();
        });
    }

    var deleteWikiBtn = document.getElementById('wikiDeleteBtn');

    if (deleteWikiBtn) {
        deleteWikiBtn.addEventListener('click', function () {
            var wikiName = deleteWikiBtn.dataset.wikiName;

            if (!window.confirm('Delete the wiki "' + wikiName + '" with ALL its folders, pages, version history and attachments? This cannot be undone.')) {
                return;
            }

            // Deliberately more than a confirm() - see the button's comment in wiki_tree.html.
            var typed = window.prompt('Type the wiki name to confirm deletion:');

            if (typed === null) {
                return;
            }

            if (typed.trim() !== wikiName) {
                alert('Name did not match - nothing was deleted.');
                return;
            }

            document.getElementById('wikiDeleteForm').submit();
        });
    }

    // --- Narrow-screen sidebar toggle - same pattern as agentic-chat.js's sidebar ----------------
    var sidebarEl = document.querySelector('.wiki-sidebar');
    var sidebarToggle = document.getElementById('wikiSidebarToggle');
    var backdrop = document.getElementById('wikiBackdrop');

    function closeSidebar() {
        sidebarEl.classList.remove('wiki-sidebar--open');

        if (backdrop) {
            backdrop.classList.remove('wiki-backdrop--visible');
        }
    }

    if (sidebarEl && sidebarToggle && backdrop) {
        sidebarToggle.addEventListener('click', function () {
            sidebarEl.classList.add('wiki-sidebar--open');
            backdrop.classList.add('wiki-backdrop--visible');
        });

        backdrop.addEventListener('click', closeSidebar);

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeSidebar();
            }
        });
    }

    // --- Folder actions (new page/new folder/rename/delete) - see wiki_tree.html :: sidebar ------
    var sidebar = document.querySelector('.wiki-sidebar');

    if (sidebar) {
        var wikiId = sidebar.dataset.wikiId;
        var sidebarCsrfHeader = sidebar.dataset.csrfHeader;
        var sidebarCsrfToken = sidebar.dataset.csrfToken;

        function postFolderAction(url, params) {
            var body = new URLSearchParams();

            Object.keys(params).forEach(function (key) {
                if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
                    body.append(key, params[key]);
                }
            });

            var options = { method: 'POST', headers: {}, body: body };
            options.headers[sidebarCsrfHeader] = sidebarCsrfToken;
            options.headers['Content-Type'] = 'application/x-www-form-urlencoded';

            fetch(url, options).then(function (response) {
                if (!response.ok) {
                    return response.text().then(function (text) {
                        throw new Error('HTTP ' + response.status + (text ? ' - ' + text.substring(0, 200) : ''));
                    });
                }

                window.location.reload();
            }).catch(function (error) {
                alert('Wiki: ' + error.message);
            });
        }

        sidebar.addEventListener('click', function (event) {
            var newPageBtn = event.target.closest('[data-folder-newpage]');

            if (newPageBtn) {
                var folderIdForNewPage = newPageBtn.dataset.folderNewpage;
                window.location.href = '/wiki/' + wikiId + '/page/new'
                    + (folderIdForNewPage ? '?folderId=' + encodeURIComponent(folderIdForNewPage) : '');
                return;
            }

            var newFolderBtn = event.target.closest('[data-folder-newfolder]');

            if (newFolderBtn) {
                var name = window.prompt('Folder name:');

                if (!name || !name.trim()) {
                    return;
                }

                postFolderAction('/wiki/' + wikiId + '/folder/create', {
                    name: name.trim(),
                    parentFolderId: newFolderBtn.dataset.folderNewfolder
                });
                return;
            }

            var renameBtn = event.target.closest('[data-folder-rename]');

            if (renameBtn) {
                var currentName = renameBtn.dataset.folderRenameName;
                var newName = window.prompt('Rename folder:', currentName);

                if (!newName || !newName.trim() || newName.trim() === currentName) {
                    return;
                }

                postFolderAction('/wiki/' + wikiId + '/folder/' + renameBtn.dataset.folderRename + '/rename', { name: newName.trim() });
                return;
            }

            var deleteBtn = event.target.closest('[data-folder-delete]');

            if (deleteBtn) {
                var folderName = deleteBtn.dataset.folderDeleteName;

                if (!window.confirm('Delete folder "' + folderName + '" and everything inside it (subfolders, pages, history)? This cannot be undone.')) {
                    return;
                }

                postFolderAction('/wiki/' + wikiId + '/folder/' + deleteBtn.dataset.folderDelete + '/delete', {});
            }
        });
    }
})();
