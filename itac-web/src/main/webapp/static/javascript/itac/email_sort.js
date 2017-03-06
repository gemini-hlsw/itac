$(function() {
	$("a", ".sort-button").button();

	$('#sort-header a').toggle(function() {
		$("#configure-sort").slideDown();
	}, function() {
		$("#configure-sort").slideUp();
	});				
	$("#configure-sort").hide();


    var applySortButtonText = function(activeSort) {
		if (activeSort == 'band') {
			$("#sort-header a span").text("Sort: Band");
		} else if (activeSort == 'partner') {
			$("#sort-header a span").text("Sort: Partner");
		} else if (activeSort == 'reference') {
			$("#sort-header a span").text("Sort: Partner Reference");
		} else if (activeSort == 'address') {
			$("#sort-header a span").text("Sort: Receiver Address");
		} else if (activeSort == "failed") {
            $("#sort-header a span").text("Sort: Failed Timestamp");
        }
    }

	$("#configure-sort input").change(function() {
		var activeSort = $(this).val();
        applySortButtonText(activeSort);
	});

    // Apply default sort
    reorder(band_sort());
});

function reorder(kvs) {

    for(kv in kvs){
        var id = kvs[kv].value;
        var header = $('#accordion_header_' + id);
        header.detach();
        var bdy = $('#accordion_body_' + id);
        bdy.detach();
        $("#all-accordion").append(header);
        $("#all-accordion").append(bdy);
    }
}


