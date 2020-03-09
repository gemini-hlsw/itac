$(function() {
    $("a", ".sort-button").button();

    $('#sort-header a').toggle(function() {
        $("#configure-sort").slideDown();
    }, function() {
        $("#configure-sort").slideUp();
    });

    var applySortButtonText = function(activeSort) {
        if (localStorage) {
            localStorage.defaultSort = activeSort;
        }
        var buttonText = ''
        if (activeSort == 'partner') {
            buttonText = "Partner / Partner ID";
        } else if (activeSort == 'instrument') {
            buttonText = "Instrument";
        } else if (activeSort == 'title') {
            buttonText = "Title";
        } else if (activeSort == "pi") {
            buttonText = "PI";
        } else if (activeSort == "band_merge_index") {
            buttonText = "Band / Merge index";
        } else if (activeSort == "band_partner") {
            buttonText = "Band / Partner/Partner ID";
        } else if (activeSort == "band_instrument") {
            buttonText = "Band / Instrument";
        } else if (activeSort == "band_title") {
            buttonText = "Band / Title";
        } else if (activeSort == "band_pi") {
            buttonText = "Band / PI";
        } else if (activeSort == "rank_by_partner") {
            buttonText = "Partner / Rank";
        }

        // Set button text
        $("#sort-header a span").text("Sort: " + buttonText);

        // Set the sort radio button
        $("#configure-sort input:radio").removeAttr("checked");
        $("#configure-sort input[value='" + activeSort + "']:radio").attr("checked", "checked");
    }

    $("#configure-sort input").change(function() {
        var activeSort = $(this).val();
        applySortButtonText(activeSort);
    });

    // Apply default sort
    var defaultSort = 'partner';
    if (localStorage && localStorage.defaultSort) {
        defaultSort = localStorage.defaultSort;
    }

    if ($("#configure-sort input[value='band_merge_index']").length === 0 && defaultSort.indexOf("band") >= 0) {
        defaultSort = 'partner';
    }

    // Apply initial sort
    if (defaultSort === 'partner') {
        reorder(partner_sort());
    } else if (defaultSort === 'rank_by_partner') {
        reorder(rank_sort());
    } else if (defaultSort === 'instrument') {
        reorder(instrument_sort());
    } else if (defaultSort === 'title') {
        reorder(title_sort());
    } else if (defaultSort === 'pi') {
        reorder(primary_investigator_sort());
    } else if (defaultSort === 'band_merge_index') {
        reorder(band_merge_index_sort());
    } else if (defaultSort === 'band_partner') {
        reorder(band_partner_sort());
    } else if (defaultSort === 'band_instrument') {
        reorder(band_instrument_sort());
    } else if (defaultSort === 'band_title') {
        reorder(band_title_sort());
    } else if (defaultSort === 'band_pi') {
        reorder(band_primary_investigator_sort());
    }
    applySortButtonText(defaultSort);
});

function reorder(kvs) {
    for(var kv in kvs){
        var id = kvs[kv].value;
        var header = $('#accordion_header_' + id);
        header.detach();
        var bdy = $('#accordion_body_' + id);
        bdy.detach();
        $("#all-accordion").append(header).append(bdy);
    }
}


