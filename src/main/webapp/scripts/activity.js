(function() {
    require(["jquery", "bootstrap/modal"], function($, events, dom) {
        if ($('#activeActivity').length != 0) {
            $('#myModal').modal('show');
        }
    });
}).call(this);