/*
 * Schedule overlay for the "Agentic" feature (see AgenticScheduleOverlayController). Same
 * swap-the-body-with-a-server-rendered-fragment pattern as agentic-file-browser.js - every action
 * (list, edit, save, delete) comes back as a fragment this script drops into the overlay wholesale.
 * Kept in its own file for the same reason the file browser is: this overlay works or doesn't,
 * independent of anything else on the chat page.
 */
(function () {
    'use strict';

    var overlay = document.getElementById('agenticScheduleOverlay');

    if (!overlay) {
        return;
    }

    var schedulesEndpoint = overlay.dataset.schedulesEndpoint;
    var csrfHeader = overlay.dataset.csrfHeader;
    var csrfToken = overlay.dataset.csrfToken;

    var body = document.getElementById('agenticScheduleBody');
    var openBtn = document.getElementById('agenticScheduleBtn');
    var closeBtn = document.getElementById('agenticScheduleCloseBtn');
    var newBtn = document.getElementById('agenticScheduleNewBtn');

    function swapBody(html) {
        body.innerHTML = html;
    }

    function showError(message) {
        alert('Schedules: ' + message);
    }

    // Same lesson as the file browser and voice input: surface the server's own reason instead of
    // a bare "HTTP 400", so a failure is diagnosable from the browser alone.
    function failWithBody(response) {
        return response.text().then(function (bodyText) {
            throw new Error('HTTP ' + response.status + (bodyText ? ' - ' + bodyText.substring(0, 200) : ''));
        });
    }

    function loadList() {
        fetch(schedulesEndpoint + '/list', { headers: { 'X-Requested-With': 'fetch' } })
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(swapBody)
            .catch(function (error) { showError(error.message); });
    }

    function loadForm(id) {
        var url = schedulesEndpoint + '/edit' + (id ? '?id=' + encodeURIComponent(id) : '');

        fetch(url, { headers: { 'X-Requested-With': 'fetch' } })
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(swapBody)
            .catch(function (error) { showError(error.message); });
    }

    function saveForm() {
        var form = document.getElementById('agenticScheduleForm');

        if (!form) {
            return;
        }

        var options = { method: 'POST', headers: {} };
        options.headers[csrfHeader] = csrfToken;
        options.body = new FormData(form);

        fetch(schedulesEndpoint + '/save', options)
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(swapBody) // either the refreshed list (success) or the form again (validation errors)
            .catch(function (error) { showError(error.message); });
    }

    function deleteSchedule(id) {
        var options = { method: 'POST', headers: {} };
        options.headers[csrfHeader] = csrfToken;

        fetch(schedulesEndpoint + '/delete/' + encodeURIComponent(id), options)
            .then(function (response) {
                if (!response.ok) { return failWithBody(response); }
                return response.text();
            })
            .then(swapBody)
            .catch(function (error) { showError(error.message); });
    }

    openBtn.addEventListener('click', function () {
        overlay.hidden = false;
        loadList();
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

    newBtn.addEventListener('click', function () {
        loadForm(null);
    });

    // Edit/delete row actions and the form's own Save/Cancel buttons all live inside the swapped
    // fragment, so one delegated listener on the stable overlay body instead of rebinding after
    // every swap.
    body.addEventListener('click', function (event) {
        var editEl = event.target.closest('[data-sched-edit]');

        if (editEl) {
            loadForm(editEl.dataset.schedEdit);
            return;
        }

        var deleteEl = event.target.closest('[data-sched-delete]');

        if (deleteEl) {
            var name = deleteEl.dataset.schedDeleteName;

            if (!confirm('Delete schedule "' + name + '"?')) {
                return;
            }

            deleteSchedule(deleteEl.dataset.schedDelete);
            return;
        }

        if (event.target.id === 'agenticScheduleSaveBtn') {
            saveForm();
            return;
        }

        if (event.target.id === 'agenticScheduleCancelBtn') {
            loadList();
        }
    });
})();
