(function() {
    require(["jquery", "bootstrap/modal"], function($, events, dom) {
        if ($('#activeObservation').length != 0) {
            $('#myModal').modal('show');
        }
    });
}).call(this);