/*
 * Issues UI: renders markdown read-only (issue descriptions + comments via Toast UI's viewer
 * factory), drives the editors (issue description, comment composer - the general Markdown editor,
 * ksfx-markdown-editor.js, the same one as in the wiki), the tracker
 * switcher/delete, attachment auto-upload and the narrow-screen sidebar - all the same patterns as
 * wiki.js (see that file), just against the /issues endpoints and .issues-* DOM.
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

    // The general Markdown editor (ksfx-markdown-editor.js - same editor as the wiki), with image
    // upload through the issues' own asset endpoint.
    function markdownEditor(mountEl, initialMarkdown, minHeight) {
        return KsfxMarkdownEditor.create(mountEl, {
            initialMarkdown: initialMarkdown,
            uploadEndpoint: mountEl.dataset.assetUploadEndpoint,
            csrfHeader: mountEl.dataset.csrfHeader,
            csrfToken: mountEl.dataset.csrfToken,
            minHeight: minHeight
        });
    }

    // --- Read-only markdown viewers (description + every comment) --------------------------------
    // Each .issues-md-viewer renders the markdown from the hidden .issues-md-source textarea
    // immediately preceding it (issues_view.html pairs them up).
    var viewers = document.querySelectorAll('.issues-md-viewer');

    if (viewers.length > 0) {
        var firstViewer = viewers[0];

        loadToastUiAssets(firstViewer.dataset.toastuiJs, firstViewer.dataset.toastuiCss).then(function () {
            viewers.forEach(function (viewerEl) {
                var source = viewerEl.previousElementSibling;
                var markdown = source && source.classList.contains('issues-md-source') ? source.value : '';

                if (markdown.trim() === '') {
                    viewerEl.innerHTML = '<span class="issues-no-description">No description.</span>';
                    return;
                }

                toastui.Editor.factory({
                    el: viewerEl,
                    viewer: true,
                    initialValue: markdown
                });
            });
        }).catch(function (error) {
            console.error(error);
        });
    }

    // --- Issue description editor (issues_edit.html) ---------------------------------------------
    var editorMount = document.getElementById('issuesEditorMount');

    if (editorMount) {
        var editForm = document.getElementById('issuesEditForm');
        var contentField = document.getElementById('issuesEditContent');
        var saveBtn = document.getElementById('issuesEditSaveBtn');
        var editor = null;

        markdownEditor(editorMount, contentField.value, 300).then(function (created) {
            editor = created;
        }).catch(function (error) {
            editorMount.textContent = 'Could not load the editor: ' + error.message;
        });

        saveBtn.addEventListener('click', function () {
            if (!editForm.reportValidity()) {
                return;
            }

            contentField.value = editor ? editor.getMarkdown() : contentField.value;
            editForm.submit();
        });
    }

    // --- Comment composer (issues_view.html) -----------------------------------------------------
    var commentMount = document.getElementById('issuesCommentEditorMount');

    if (commentMount) {
        var commentForm = document.getElementById('issuesCommentForm');
        var commentField = document.getElementById('issuesCommentContent');
        var commentBtn = document.getElementById('issuesCommentSubmitBtn');
        var commentEditor = null;

        markdownEditor(commentMount, '', 140).then(function (created) {
            commentEditor = created;
        }).catch(function (error) {
            commentMount.textContent = 'Could not load the editor: ' + error.message;
        });

        commentBtn.addEventListener('click', function () {
            var markdown = commentEditor ? commentEditor.getMarkdown() : '';

            if (markdown.trim() === '') {
                return;
            }

            commentField.value = markdown;
            commentForm.submit();
        });
    }

    // --- Attachments -----------------------------------------------------------------------------
    var attachmentInput = document.getElementById('issuesAttachmentInput');

    if (attachmentInput) {
        attachmentInput.addEventListener('change', function () {
            if (attachmentInput.files.length > 0) {
                attachmentInput.form.submit();
            }
        });
    }

    document.querySelectorAll('.issues-attachment-delete-form').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('Delete attachment "' + form.dataset.filename + '"?')) {
                event.preventDefault();
            }
        });
    });

    // --- Tracker switcher / create / delete ------------------------------------------------------
    var switcherSelect = document.getElementById('issuesSwitcherSelect');

    if (switcherSelect) {
        switcherSelect.addEventListener('change', function () {
            window.location.href = '/issues/' + switcherSelect.value + '/';
        });
    }

    var newTrackerBtn = document.getElementById('issuesSwitcherNewBtn');

    if (newTrackerBtn) {
        newTrackerBtn.addEventListener('click', function () {
            var name = window.prompt('Name for the new tracker:');

            if (!name || !name.trim()) {
                return;
            }

            document.getElementById('issuesTrackerCreateNameInput').value = name.trim();
            document.getElementById('issuesTrackerCreateForm').submit();
        });
    }

    var deleteTrackerBtn = document.getElementById('issuesTrackerDeleteBtn');

    if (deleteTrackerBtn) {
        deleteTrackerBtn.addEventListener('click', function () {
            var trackerName = deleteTrackerBtn.dataset.trackerName;

            if (!window.confirm('Delete the tracker "' + trackerName + '" with ALL its issues, comments, labels and attachments? This cannot be undone.')) {
                return;
            }

            var typed = window.prompt('Type the tracker name to confirm deletion:');

            if (typed === null) {
                return;
            }

            if (typed.trim() !== trackerName) {
                alert('Name did not match - nothing was deleted.');
                return;
            }

            document.getElementById('issuesTrackerDeleteForm').submit();
        });
    }

    // --- Narrow-screen sidebar toggle - same pattern as wiki.js/agentic-chat.js ------------------
    var sidebarEl = document.querySelector('.issues-sidebar');
    var sidebarToggle = document.getElementById('issuesSidebarToggle');
    var backdrop = document.getElementById('issuesBackdrop');

    function closeSidebar() {
        sidebarEl.classList.remove('issues-sidebar--open');

        if (backdrop) {
            backdrop.classList.remove('issues-backdrop--visible');
        }
    }

    if (sidebarEl && sidebarToggle && backdrop) {
        sidebarToggle.addEventListener('click', function () {
            sidebarEl.classList.add('issues-sidebar--open');
            backdrop.classList.add('issues-backdrop--visible');
        });

        backdrop.addEventListener('click', closeSidebar);

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeSidebar();
            }
        });
    }
})();
