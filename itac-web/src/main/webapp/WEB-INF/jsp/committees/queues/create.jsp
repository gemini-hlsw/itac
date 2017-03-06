<%@ page import="edu.gemini.tac.service.QueueCreationParameters" %>
<%@ page import="edu.gemini.tac.persistence.Partner" %>
<%@ page import="edu.gemini.tac.persistence.queues.partnerCharges.AdjustmentPartnerCharge" %>
<%@ page import="edu.gemini.tac.persistence.queues.partnerCharges.PartnerExchangePartnerCharge" %>

<%@ include file="../../fragments/header.jspf" %>

    <c:set var="northParams" value= "${northParams}" />
    <c:set var="southParams" value= "${southParams}" />


            <div class="span-19 colborder">
                <h2 id="new-queue-headline">Create new queue</h2>
                <div id="tabs">
                    <ul class="tab-headers">
                        <li><a href="#tabs-1">Initial</a></li>
                        <li><a href="#tabs-8">Rollovers</a></li>
                        <li><a href="#tabs-3">Band</a></li>
                        <li><a href="#tabs-4">Restrictions</a></li>
                        <li><a href="#tabs-5">Exchange</a></li>
                        <li><a href="#tabs-6">Partner</a></li>
                        <li><a href="#tabs-7">Name</a></li>
                        <li><a href="#tabs-9">Review and submit</a></li>
                    </ul>
                    <form action="/tac/committees/${committeeId}/queues/create" method="POST" id="createForm">
                    <div id="tabs-1">
                        <%@ include file="create/_initial.jspf" %>
                    </div>
                    <div id="tabs-8">
                        <%@ include file="create/_rollover.jspf"  %>
                    </div>
                    <div id="tabs-3">
                        <%@ include file="create/_band.jspf" %>
                    </div>
                    <div id="tabs-4">
                        <%@ include file="create/_restrictions.jspf" %>
                    </div>
                    <div id="tabs-5">
                        <%@ include file="create/_exchange.jspf" %>
                    </div>
                    <div id="tabs-6">
                        <%@ include file="create/_partner_quanta.jspf" %>
                    </div>
                    <div id="tabs-7">
                        <%@ include file="create/_notes_name.jspf" %>
                    </div>
                    <div id="tabs-9">
                        Please take a moment to review the information on the preceding tabs, and then when you
                        are satisfied: click submit in order to generate a queue.
                        <input type="submit" value="Submit" />
                    </div>


                    <!-- nonvisible checkboxes for accepting classical proposals -->
                    <!-- cheap cheat to pass the classical ids through to the service, but this will -->
                    <!-- make it easier to select classical proposals for queue creation in the future -->
                    <c:forEach items="${ classicalProposals }" var="proposal">
                        <span style="display: none">
                            <input name="classical-accepted" value="${ proposal.id }" type="checkbox" checked="true"/>
                        </span>
                    </c:forEach>


                    </form>
                </div>
            </div>

<script type="text/javascript">
    /*
    jQuery load function.

    Format the data table.
    Initialize all-tab data
     */
    $(function() {
        /* Formats the data table in the exchange tab */
        $('#exchangeProposals').dataTable({
            "bPaginate": false,
            "bFilter": false,
            "bSort": true,
            "bInfo": false,
            "aaSorting": [[ 0, "asc" ], [1, "asc"]],
            "sDom": '<"top"f>t',
            "aoColumns": [
              { "sType": "html" },
              { "sType": "html" },
              { "sType": "html" },
              { "sType": "num-html" },
              { "sType": "html" },
              { "sType": "html" },
            ]
        });

        var quanta = {};

        var previousQueueParams = {
           "north" : {
                "totalTimeAvailable"            : "${ northParams.totalTimeAvailable }",
                "partnerWithInitialPick"        : "${ northParams.partnerWithInitialPick.id }",
                "conditionSet"                  : "${ northParams.conditionSet.id }",
                "binConfiguration"              : "${ northParams.binConfiguration.id }",
                "band1Cutoff"                   : "${ northParams.band1Cutoff }",
                "band2Cutoff"                   : "${ northParams.band2Cutoff }",
                "band3Cutoff"                   : "${ northParams.band3Cutoff }",
                "band3ConditionsThreshold"      : "${ northParams.band3ConditionsThreshold }",
                "useBand3AfterThresholdCrossed" : "${ northParams.useBand3AfterThresholdCrossed }",
                "scheduleSubaruAsPartner"       : "${ northParams.scheduleSubaruAsPartner }",
                "rolloverSetId"                 : "${ northParams.rolloverSetId }",
                "restrictedBins"                : [
                    <c:forEach items="${ northParams.restrictedBins }" var="bin" varStatus="status">
                       {id: "${bin.id}", description: "${ bin.description }", value: "${ bin.value }", units: "${bin.units}", checked: true}<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ],
                "defaultRestrictedBins"                : [
                    <c:forEach items="${ northParams.defaultRestrictedBins }" var="bin" varStatus="status">
                       {id: "${bin.id}", description: "${ bin.description }", value: "${ bin.value }", units: "${bin.units}", checked: false}<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ],
                "subaruProposalsSelected"       : [
                    <c:forEach items="${ northParams.subaruProposalsSelected }" var="proposalId" varStatus="status">
                        ${ proposalId }<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ]
           },
           "south" : {
                "totalTimeAvailable"            : "${ southParams.totalTimeAvailable }",
                "partnerWithInitialPick"        : "${ southParams.partnerWithInitialPick.id }",
                "conditionSet"                  : "${ southParams.conditionSet.id }",
                "binConfiguration"              : "${ southParams.binConfiguration.id }",
                "band1Cutoff"                   : "${ southParams.band1Cutoff }",
                "band2Cutoff"                   : "${ southParams.band2Cutoff }",
                "band3Cutoff"                   : "${ southParams.band3Cutoff }",
                "band3ConditionsThreshold"      : "${ southParams.band3ConditionsThreshold }",
                "useBand3AfterThresholdCrossed" : "${ southParams.useBand3AfterThresholdCrossed }",
                "scheduleSubaruAsPartner"       : "${ southParams.scheduleSubaruAsPartner }",
                "rolloverSetId"                 : "${ southParams.rolloverSetId }",
                "restrictedBins"                : [
                    <c:forEach items="${ southParams.restrictedBins }" var="bin" varStatus="status">
                        {id: "${bin.id}", description: "${ bin.description }", value: "${ bin.value }", units: "${bin.units}", checked: true}<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ],
                "defaultRestrictedBins"                : [
                    <c:forEach items="${ northParams.defaultRestrictedBins }" var="bin" varStatus="status">
                       {id: "${bin.id}", description: "${ bin.description }", value: "${ bin.value }", units: "${bin.units}", checked: false}<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ],
                "subaruProposalsSelected"       : [
                    <c:forEach items="${ southParams.subaruProposalsSelected }" var="proposalId" varStatus="status">
                        ${ proposalId }<c:if test="${not status.last}">,</c:if>
                    </c:forEach>
                ]
           }
        };

        function applyQueueParameters(params) {
            $('#total-time-available').val(params.totalTimeAvailable);
            $('#partner-initial-pick').val(params.partnerWithInitialPick);
            $('#condition-bins').val(params.conditionSet);
            $('#bin-configurations').val(params.binConfiguration);
            $('#band-1-threshold').val(params.band1Cutoff);
            $('#band-2-threshold').val(params.band2Cutoff);
            $('#band-3-threshold').val(params.band3Cutoff);
            $('#band-3-conditions-threshold').val(params.band3ConditionsThreshold);
            $('#rollovers').val(params.rolloverSetId);
            if (params.scheduleSubaruAsPartner === "true") {
                $('#subaruAsQueue').attr('checked', 'checked');
            } else {
                $('#subaruAsExchange').attr('checked', 'checked');
            }
            $('input[name=exchange-accepted]').removeAttr('checked');
            for (var i = 0; i < params.subaruProposalsSelected.length; i++) {
                var selectedId = params.subaruProposalsSelected[i];
                $('#exchange-accepted-' + selectedId).attr('checked', 'checked');
            }
            $('#restricted-bins').empty();
            var restrictedBins = params.restrictedBins;
            var description = $.map(params.restrictedBins, function(e) {return e.description});
            var missingDefaults = $.grep(params.defaultRestrictedBins, function(e) {return description.indexOf(e.description) === -1;});
            $.merge(restrictedBins, missingDefaults);
            $.each(restrictedBins, function(k, b) {
                var li = $("<li/>").appendTo($('#restricted-bins'));
                var check = $('<input type="checkbox"/>').attr("id", "restricted-bins-" + b.id).attr("name", "restricted-bins").attr("value", b.id);
                if (b.checked) {
                    check.attr("checked", "checked");
                }
                check.appendTo(li);
                $('<label>Restriction: ' + b.description + '</label>').appendTo(li);
                $('<label> only allowed for </label><input type="text" size="3" name="restricted-bins-' + b.id + '-value" value="' + b.value + '"/> <label>'+ b.units + ' of observations.</label>').appendTo(li);
            });
        };

        function applyPreviousQueueParameters(northOrSouth) {
            if (northOrSouth === "North") {
                applyQueueParameters(previousQueueParams.north);
            } else {
                applyQueueParameters(previousQueueParams.south);
            }
            $("#subaruQueueToggle").buttonset("refresh");
            $(".scheduleCheckbox").each(function(){
                var isSelected = $(this)[0].checked;
                if(isSelected){
                    $(this).change();
                }
            });
        };

        page.helpContent = 'Create a new queue.  ' +
            'Note: Currently exchange proposals from North and South can be scheduled for every queue ' +
            'even though this does not make sense for most cases.';

        $("#tabs").tabs();
        $("#tabs ul.tab-headers li:last").attr("style", "float: right;");
        $("#site").change(function() {
            /* This is the North/South site selector, and this fn hides / displays the appropriate RolloverSets in the Rollover tab */
            var siteText = $("#site option:selected").text();
            setRolloverSite(siteText);
            setExchangeProposals(siteText);
            setInitialPicks(siteText);
            applyPreviousQueueParameters(siteText);
        });
        $("#site, #total-time-available").change(function() {
            var site = $("#site option:selected").text();
            quanta.updateQuantaTable(site);
            tangle.setValue("site", site);
            tangle.setValue("queueTime", $("#total-time-available").val().toFloat());
            onSiteSelected(site);
        });

        $("#createForm").validate();

        var partnerNames = [
          <c:forEach items="${partners}" var="partner">
            '${ partner.partnerCountryKey }',
          </c:forEach>
        ];

        var partners = {
            <c:forEach items="${partners}" var="partner">
            "${ partner.partnerCountryKey }" : {
                "id" : ${ partner.id },
                "name" : "${ partner.name }",
                "abbreviation" : "${ partner.abbreviation }",
                "partnerCountryKey" : "${ partner.partnerCountryKey }",
                "isCountry" : ${ partner.country },
                "percentageShare" : "${ partner.percentageShare }",
                "north" : ${ partner.north },
                "south" : ${ partner.south },
                "chargeable" : ${ partner.chargeable },
                "adjustmentNorth" : "<%= ((QueueCreationParameters) pageContext.getAttribute("northParams")).getPartnerCharge((Partner) pageContext.getAttribute("partner"), AdjustmentPartnerCharge.class) %>",
                "adjustmentSouth" : "<%= ((QueueCreationParameters) pageContext.getAttribute("southParams")).getPartnerCharge((Partner) pageContext.getAttribute("partner"), AdjustmentPartnerCharge.class) %>",
                "exchangeNorth" : "<%= ((QueueCreationParameters) pageContext.getAttribute("northParams")).getPartnerCharge((Partner) pageContext.getAttribute("partner"), PartnerExchangePartnerCharge.class) %>",
                "exchangeSouth" : "<%= ((QueueCreationParameters) pageContext.getAttribute("southParams")).getPartnerCharge((Partner) pageContext.getAttribute("partner"), PartnerExchangePartnerCharge.class) %>"
            },
            </c:forEach>
        };

        function setInitialPicks(northOrSouth) {
             $('#partner-inital-pick')
                .find('option')
                .remove()
                .end()
                .append('<option value="none"></option>')
                .val('none');


            if (northOrSouth === "North") {
                for (i = 0; i < partnerNames.length; i++) {
                    var partner = partners[partnerNames[i]];
                    if (partner.north && partner.chargeable) {
                        $('<option/>').attr('value', partner.id).text(partner.name).appendTo('#partner-inital-pick');
                    }
                }
            } else {
                for (i = 0; i < partnerNames.length; i++) {
                    var partner = partners[partnerNames[i]];
                    if (partner.south && partner.chargeable) {
                        $('<option/>').attr('value', partner.id).text(partner.name).appendTo('#partner-inital-pick');
                    }
                }
            }
        }

        var quanta = {
            lastSite : '',
            updateQuantaTable : function (northOrSouth) {
                var totalTime = $('#total-time-available').val();
                var siteChanged = !(northOrSouth === quanta.lastSite);
                var siteSet = (northOrSouth === 'North') || (northOrSouth === 'South');
                quanta.lastSite = northOrSouth;

                if ((totalTime > 0) && siteSet) {
                    var initialsString = '<td>Initial</td>';

                    var initialAccumulator = 0;
                    for (i = 0; i < partnerNames.length; i++) {
                        var partner = partners[partnerNames[i]];

                        var partnerHasRights = ((northOrSouth === 'North') && partner.north) || ((northOrSouth === 'South') && partner.south);
                        if (partnerHasRights && partner.chargeable) {
                            var partnerInitial = totalTime * (1/100) * partner.percentageShare;
                            initialAccumulator += partnerInitial;
                            initialsString += '<td>' + partnerInitial.toFixed(2) + '</td>' ;

                        }
                    };

                    initialsString += '<td>' + initialAccumulator.toFixed(2) + '</td>' ;


                    $('#quanta-initial')
                        .find('td')
                        .remove()
                        .end()
                        .append(initialsString);
                } else {
                    $('#quanta-partners')
                        .find('th')
                        .remove()
                        .end()
                        .append('<th>Please set total time and site</th>');

                     $('#quanta-initial, #quanta-adjustments, #quanta-exchanges')
                         .find('td')
                         .remove()
                         .end()
                }

                if (siteChanged) {
                    var adjustmentsString = '<td>Semester Reductions</td>';
                    var exchangesString = '<td>Exchanges</td>';
                    var headersString = '<th>&nbsp;</th>';


                    for (i = 0; i < partnerNames.length; i++) {
                        var partner = partners[partnerNames[i]];

                        var partnerHasRights = ((northOrSouth === 'North') && partner.north) || ((northOrSouth === 'South') && partner.south);
                        if (partnerHasRights && partner.chargeable) {
                            var partnerAdjustment = ((northOrSouth === 'North') ? partner.adjustmentNorth : partner.adjustmentSouth);
                            var partnerExchange = ((northOrSouth === 'North') ? partner.exchangeNorth : partner.exchangeSouth);
                            headersString += '<th class=\'quanta-partners-header\'>' + partner.abbreviation + ' (' + partner.percentageShare + '%)</th>' ;
                            adjustmentsString += '<td><input type="text" name="partnerAdjustment-' + partner.abbreviation + '" value="' + partnerAdjustment + '" class="number required" size="4"/></td>' ;
                            exchangesString += '<td><input type="text" name="partnerExchange-' + partner.abbreviation + '" value="' + partnerExchange + '" class="number required" size="4"/></td>' ;
                        }
                    }
                    headersString += '<th>Total (100%)</th>';
                    adjustmentsString += '<td>&nbsp;</td>';
                    exchangesString += '<td>&nbsp;</td>';

                    $('#quanta-partners')
                        .find('th')
                        .remove()
                        .end()
                        .append(headersString);
                    $('#quanta-adjustments')
                        .find('td')
                        .remove()
                        .end()
                        .append(adjustmentsString);
                    $('#quanta-exchanges')
                        .find('td')
                        .remove()
                        .end()
                        .append(exchangesString);
                }
            }
        };

        quanta.updateQuantaTable('');

        reactivePartnerTab();
    });
    /* ^^^ Place any final "onPageLoad()" functions above ^^^ */

    function setExchangeProposals(northOrSouth) {
        var proposalsTable = $("#exchangeProposals");
        var showRows = $([]);
        var hideRows = $([]);
        if (northOrSouth == "North") {
            showRows = proposalsTable.find("tr.North");
            hideRows = proposalsTable.find("tr.South");
        } else if (northOrSouth == "South") {
            showRows = proposalsTable.find("tr.South");
            hideRows = proposalsTable.find("tr.North");
        } else {
            showRows = proposalsTable.find("tr.North, tr.South");
        }

        showRows.each(function() {
           $(this).show();
        });
        hideRows.each(function() {
           $(this).hide();
        });

    }

    function setRolloverSite(northOrSouth){
        $("#rollovers").html("");

        var warningVisible = false;
        if(northOrSouth == "North"){
            <c:forEach items="${ rolloverSetsNorth }" var="northRolloverSet">
                 $('<option/>').attr('value', '${northRolloverSet.id}').text('${northRolloverSet.name} (${northRolloverSet.totalHours} hrs)').appendTo('#rollovers');
            </c:forEach>
        }else if(northOrSouth == "South"){
            <c:forEach items="${ rolloverSetsSouth }" var="southRolloverSet">
                 $('<option/>').attr('value', '${southRolloverSet.id}').text('${southRolloverSet.name} (${southRolloverSet.totalHours} hrs)').appendTo('#rollovers');
            </c:forEach>
        }else{
            //Blank
            warningVisible = true;
        }
        if(warningVisible){
            $("#site_selection_warning").show();
        }else{
            $("#site_selection_warning").hide();
        }
    }

    <%@ include file="fragments/exchangeTab.js" %>
    <%@ include file="fragments/partnerTab.js" %>
</script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
