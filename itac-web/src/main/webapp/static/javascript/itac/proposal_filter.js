function showNontaggedBooleans(header_element, classesOfThisElement, listOfExcludedTags, booleanTags) {
    /*
     If it gets here, the element is still not excluded explicitly. It may be hidden (default)
     or it may be showing, because it has a non-excluded member in an 'active' set.
     */
    var i = 0;
    header_element.show();
    for (i = 0; i < booleanTags.length; i += 1) {
        var tag = booleanTags[i];
        if ($.inArray(tag, classesOfThisElement) > -1) {
            if ($.inArray(tag, listOfExcludedTags) > -1) {
                if (header_element.is(":visible")) {
                    header_element.hide();
                    return false;
                }
            }
        }
    }
    return true;
}

function showIfSetIsActive(header_element, classes, excludeTags, setValues) {
    var setIsActive = false;
    for (var i = 0; i < setValues.length; i += 1) {
        var setValue = setValues[i];
        if ($.inArray(setValue, excludeTags) > -1) {
            setIsActive = true;
            break;
        }
    }
    if (setIsActive) {
        var notTagged = true,
                hasClassInSet = false;
        for (i = 0; i < classes.length; i += 1) {
            var elementClass = classes[i];
            if ($.inArray(elementClass, setValues) > -1) {
                hasClassInSet = true;
                //$(this) has a class (tag) that is in the set that is being reviewed
                if ($.inArray(elementClass, excludeTags) > -1) {
                    notTagged = false;
                }
            }
        }
        if (hasClassInSet && notTagged) {
            header_element.show();
            return true;
        } else {
            header_element.hide();
            return false;
        }
    }
    return true;
}

function setFilters() {
    var i = 0;
    $("body").css("cursor", "progress");

    var excludeTags = [],
            locations = ["NORTH", "SOUTH"],
            partners = [ "US", "CA", "CL", "AU", "BR", "AR", "UH", "LP", "Subaru", "Keck", "GeminiStaff"],
            toos = ["rapidToO", "standardToO"],
            modes = [ "Queue", "Classical"],
            exchanges = ["G4SK", "KS4G"],
            tags = [ "Joint", "LGS", "MCAO", "PW", "NotBand3"],
            problems = ["OK", "Warnings", "Errors"],
            allProposalAccordions = $("#all-accordion .ui-accordion-content"),
            allProposalHeaders = $("#all-accordion .ui-accordion-header"),
            proposalsShowing = 0;

    $("a", ".resources").button();

    $("#tag-filter .filter-control input, #tag-filter .boolean_control input").each(function() {
        if ($(this).attr("checked")) {
            excludeTags.push($(this).attr("name"));
        }
    });

     if (excludeTags.length > 0) {
         var excludedText = excludeTags.join();
         if(excludeTags.length > 6){
            var included = $.grep(partners, function (item) {
                return $.inArray(item, excludeTags) < 0;
            });
            excludedText = "All but " + included;
        }
        $("#filter-header a span").text("Excluded: " + excludedText);

        /* Hide shown accordion content */
        allProposalAccordions.hide();
        allProposalHeaders.each(function() {
            $(this).hide();
            var classes = $(this).attr("class").split(' '),
                    keepChecking = showIfSetIsActive($(this), classes, excludeTags, locations);
            if (keepChecking) {
                keepChecking = showIfSetIsActive($(this), classes, excludeTags, partners);
                /* Special treatment for joint proposals -- if any partner is not on the exclude list, then show it */
                var partner_abbreviation = $(this).find("li[name='partner_abbreviation']").text().trim();
                if (partner_abbreviation.indexOf("J:") > -1) {
                    var sansPrefix = partner_abbreviation.substring(partner_abbreviation.indexOf("J:") + 2, partner_abbreviation.length);
                    var sansSuffix = sansPrefix.substring(0, sansPrefix.indexOf("|"));
                    var partnerList = sansSuffix.split("/");
                    var allPartnersExcluded = true;
                    for (i = 0; i < partnerList.length; i+=1) {
                        var partner = partnerList[i].trim();
                        if ($.inArray(partner, excludeTags) === -1) {
                            allPartnersExcluded = false;
                        }
                    }
                    if ((! allPartnersExcluded) && !$(this).is(":visible")) {
                        $(this).show();
                        keepChecking = true;
                    }
                }
            }
            if (keepChecking) {
                keepChecking = showIfSetIsActive($(this), classes, excludeTags, modes);
            }
            if (keepChecking) {
                keepChecking = showIfSetIsActive($(this), classes, excludeTags, toos);
            }
            if (keepChecking) {
                keepChecking = showIfSetIsActive($(this), classes, excludeTags, exchanges);
            }
            if (keepChecking) {
                keepChecking = showNontaggedBooleans($(this), classes, excludeTags, tags);
            }
            if (keepChecking) {
                showNontaggedBooleans($(this), classes, excludeTags, problems);
            }
            if ($(this).is(":visible")) {
                proposalsShowing += 1;
            }
        });

    } else {
        $("#filter-header a span").text("Excluded:" + "None");
        $("#all-accordion h3").each(function() {
            $(this).show();
            proposalsShowing += 1;
        });
    }
    $("body").css("cursor", "auto");

    $("#proposalsShowing").html(proposalsShowing);
}

function excludeAllBut(el){
    $("body").css("cursor", "progress");
    var thisOne = $(el).attr("checked");
    if (thisOne) {
        var tagToKeep = $(el).attr("name");
        var parentPanel = $(el).closest(".ui-tabs-panel");
        var excludeInputs = parentPanel.find(".filter-control");
        excludeInputs.each(function() {
            var t = $(this).find(":input");
            var isOnly = t.attr("name") === tagToKeep;
            t.attr("checked", ! isOnly);
        });
    }
    setFilters();
    $(el).attr("checked", false);
    $("body").css("cursor", "auto");
}

$(function() {
    $("a", ".filter-button").button();

    $('#filter-header a').toggle(function() {
        $("#configure-filters").slideDown();
    }, function() {
        $("#configure-filters").slideUp();
    });

    $("#configure-filters").hide();

    //Clear old filters
    $("#tag-filter input").each(function() {
        $(this).attr("checked", false);
    });

    $("#tag-filter .filter-control input").change(setFilters);


    /* When an "only" checkbox is selected, exclude all but "this" name in current tab */
    $("#tag-filter .only-control input").change(function() {
        excludeAllBut($(this));
    });

    /* Special treatment for boolean tags */
    $("#boolean_filter").change(function() {
        setFilters();
    });

    $(".boolean-only-control").change(function() {
        setFilters();
        $("body").css("cursor", "progress");
        var thisOne = $(this).attr("checked");
        $(this).attr("checked", false);
        if (thisOne) {
            var tagToKeep = $(this).attr("name");
            var allProposalHeaders = $("#all-accordion .ui-accordion-header");
            var proposalsShowing = 0;
            $("body").css("cursor", "progress");
            allProposalHeaders.each(function() {
                var classes = $(this).attr("class").split(' ');
                if ($(this).is(":visible") && $.inArray(tagToKeep, classes) === -1) {
                    $(this).hide();
                }
                if ($(this).is(":visible")) {
                    proposalsShowing += 1;
                }
            });
            $("#proposalsShowing").html(proposalsShowing);
        }
        $("body").css("cursor", "auto");
    });


    $("#filter-tabs").tabs();

    //ITAC-548
    $("#partner-filter-clear").button();
    $("#partner-filter-clear").click(function(){
        $(this).parents("tr").find("input").each(function() {
            $(this).attr("checked", false);
        });
        sessionStorage.setItem("partnerFilter", "false");
        setFilters();
        //If we are currently on a partner-list page, redirect to the main proposals page
        if(window.location.pathname.contains("partner")){
            window.location.href = "../..";
            return false;
        }

    });
});
