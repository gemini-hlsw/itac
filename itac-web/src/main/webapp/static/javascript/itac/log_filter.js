$(document).ready(function() {
    $("#tag-filter input").change(function() {
        var activeTags = new Array();

        $("form#tag-filter input").each(function() {
            if ($(this).attr("checked")) {
                activeTags.push($(this).attr("name"));
            }
        });

        var allLogItems = $("#log-items li");
        if (activeTags.length > 0) {
            allLogItems.each(function() {
                $(this).hide();
            });

            for (var i = 0; i < activeTags.length; i++) {
                $("#log-items li." + activeTags[i]).each(function() {
                    $(this).show();
                });
            };
        } else {
            allLogItems.each(function() {
                $(this).show();
            });
        };
    });
});
