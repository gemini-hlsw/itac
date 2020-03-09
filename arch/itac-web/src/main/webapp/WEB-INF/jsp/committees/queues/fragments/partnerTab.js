/**
 * Code to manipulate the "Partner" / _partner_quanta.jspf
 */

function reactivePartnerTab() {
    //Attach tangle handler to fields in "partner" tab
    var adjId = "";
    <c:forEach items="${partners}" var="partner">
    adjId = "partnerAdjustment${partner.partnerCountryKey}";
    $("#" + adjId).change(function(){
        var val = $(this).val().toFloat();
        tangle.setValue("adj${partner.partnerCountryKey}", val);
    });
    </c:forEach>

    //Remove columns for Keck and Subaru
    removeColumn("#colKECK");
    removeColumn("#colSUBARU");

    /* When a site is selected, hide the non-relevant column */
    onSiteSelected(tangle.getValue("site"));
}

function onSiteSelected(site) {
    if (site == "?") {
        return;
    }
    var isNorth = site == "North";
    displayColumn("#colUH", isNorth);
    displayColumn("#colCL", ! isNorth);
    displayRow("#rowNorthReductions", isNorth);
    displayRow("#rowSouthReductions", ! isNorth);
    displayRow("#rowNorthExchanges", isNorth);
    displayRow("#rowSouthExchanges", ! isNorth);
    displayRow("#rowNorthNet", isNorth);
    displayRow("#rowSouthNet", ! isNorth);
}

function displayRow(rowSelector, enable){
    if(enable){
        $(rowSelector).show();
    }else{
        $(rowSelector).hide();
    }
}

function displayColumn(columnSelector, enable) {
    var idx = $(columnSelector).closest("th").prevAll("th").length;
    $(columnSelector).parents("table").find("tr").each(function() {
        $(this).find("td:eq(" + idx + "), th:eq(" + idx + ")").fadeOut('slow', function() {
            if (enable) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });
    });
}

function removeColumn(selector) {
    var idx = $(selector).closest("th").prevAll("th").length;
    $(selector).parents("table").find("tr").each(function() {
        $(this).find("td:eq(" + idx + "), th:eq(" + idx + ")").fadeOut('slow', function() {
            $(this).remove();
        });
    });
}
