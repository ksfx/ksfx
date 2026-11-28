/*
 * The general Markdown editor of KSFX - issues, the agents' file browser and wherever Markdown is
 * written outside the live wiki. Same editor and toolbar as the wiki (vendored bundle
 * collab-editor.min.js, window.KsfxCollab.mountStandalone), alone, with a Markdown source toggle and
 * YAML front matter kept apart. This file only loads the bundle once and wires image uploads.
 *
 *   KsfxMarkdownEditor.create(container, {
 *       initialMarkdown: '...',
 *       uploadEndpoint: '/issues/asset/upload', csrfHeader: 'X-CSRF-TOKEN', csrfToken: '...', // optional: images
 *       minHeight: 260, sourceToggle: true, startInSource: false
 *   }).then(function (editor) { ... editor.getMarkdown() ... });
 *
 * The bundle URLs come from data-editor-js / data-editor-css on the container (Thymeleaf @{...},
 * so a context path is respected), else the root-relative defaults.
 */
(function () {
    'use strict';

    var loading = null;

    function load(container) {
        if (window.KsfxCollab) {
            return Promise.resolve();
        }

        if (!loading) {
            loading = new Promise(function (resolve, reject) {
                var link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = container.dataset.editorCss || '/vendor/collab-editor/collab-editor.min.css';
                document.head.appendChild(link);

                var script = document.createElement('script');
                script.src = container.dataset.editorJs || '/vendor/collab-editor/collab-editor.min.js';
                script.onload = resolve;
                script.onerror = function () { loading = null; reject(new Error('Could not load the editor.')); };
                document.head.appendChild(script);
            });
        }

        return loading;
    }

    function uploader(options) {
        if (!options.uploadEndpoint) {
            return null;
        }

        return function (file) {
            var formData = new FormData();
            formData.append('file', file, file.name || 'pasted-image.png');

            var headers = {};
            if (options.csrfHeader) {
                headers[options.csrfHeader] = options.csrfToken;
            }

            return fetch(options.uploadEndpoint, { method: 'POST', credentials: 'same-origin', headers: headers, body: formData })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('HTTP ' + response.status);
                    }
                    return response.json();
                })
                .then(function (result) { return result.url; })
                .catch(function (error) {
                    alert('Image upload failed: ' + error.message);
                    throw error;
                });
        };
    }

    window.KsfxMarkdownEditor = {
        create: function (container, options) {
            options = options || {};

            return load(container).then(function () {
                return window.KsfxCollab.mountStandalone(container, {
                    initialMarkdown: options.initialMarkdown || '',
                    uploadFile: uploader(options),
                    minHeight: options.minHeight || 260,
                    sourceToggle: options.sourceToggle !== false,
                    startInSource: !!options.startInSource
                });
            });
        }
    };
})();
