/**
 * Code to manipulate the "Exchange" tab of the queue creation page
 */

/* TODO: REMOVE THIS DEBUGGING CODE !!! */
if (!window['console']) {
    // Enable console
    if (window['loadFirebugConsole']) {
        window.loadFirebugConsole();
    } else {
        // No console, use Firebug Lite
        var firebugLite = function(F,i,r,e,b,u,g,L,I,T,E){if(F.getElementById(b))return;E=F[i+'NS']&&F.documentElement.namespaceURI;E=E?F[i+'NS'](E,'script'):F[i]('script');E[r]('id',b);E[r]('src',I+g+T);E[r](b,u);(F[e]('head')[0]||F[e]('body')[0]).appendChild(E);E=new Image;E[r]('src',I+L);};
        firebugLite(document,'createElement','setAttribute','getElementsByTagName','FirebugLite','4','firebug-lite.js','releases/lite/latest/skin/xp/sprite.png','https://getfirebug.com/','#startOpened');
    }
} else {
    // console is already available, no action needed.
}
/* TODO: REMOVE ABOVE DEBUGGING CODE */
console.log("Beginning execution of exchange tab");

try{
//Since this is #included in page fragment, the scope here is within the jQuery $() page function
tangle = buildTangle();
console.log("buildTangle: OK");
exchangeQueueCreate(tangle);
console.log("exchangeQueueCreate: OK");
reactiveExchangeTab(tangle);
console.log("reactiveExchangeTab: OK");
}catch(x){
    console.log("Exception thrown building exchange tab:" + x);
}
function exchangeQueueCreate(tangle) {
    var exchangeProposals = {
    <c:forEach items = "${ fromKeckProposals }" var = "proposal" varStatus = "status">
        <c:set var = "submissionRecommendedTime" value = "${ proposal.totalRecommendedTime }" />
        '${ proposal.id }': {
            scope: 'Keck',
            requestedTime: ${(!empty submissionRecommendedTime) ? submissionRecommendedTime.valueInHours : 0},
        north: ${proposal.site.displayName == "North"},
        south: ${proposal.site.displayName == "South"},
        scheduled: false
        }<c:if test = "${ !status.last }" > ,</c:if>
    </c:forEach>
    <c:if test = "${ !empty fromKeckProposals && !empty fromSubaruProposals }" >, </c:if>
    <c:forEach items="${ fromSubaruProposals }" var="proposal" varStatus="status">
        <c:set var="submissionRecommendedTime" value="${ proposal.totalRecommendedTime }"/>
        '${ proposal.id }': {
            scope: 'Subaru',
            requestedTime: ${(!empty submissionRecommendedTime) ? submissionRecommendedTime.valueInHours : 0},
        north: ${proposal.site.displayName == "North"},
        south: ${proposal.site.displayName == "South"},
        scheduled: false
        }<c:if test = "${ !status.last }"> ,</c:if>
    </c:forEach >
    };


    var keck = {
        available: 0,
        claimed: 0,
        north: 0,
        south: 0,
        schedule: function(north, south, time) {
            this.available += time;
            if (north && south) {
                this.north += time / 2;
                this.south += time / 2;
            } else if (north) {
                this.north += time;
            } else {
                this.south += time;
            }
        },
        unschedule: function(north, south, time) {
            this.available -= time;
            if (north && south) {
                this.north -= time / 2;
                this.south -= time / 2;
            } else if (north) {
                this.north -= time;
            } else {
                this.south -= time;
            }
        }
    };

    var subaru = {};
    $.extend(subaru, keck);

    var scheduleProposal = function(proposal) {
        if (proposal.scope === 'Keck') {
            keck.schedule(proposal.north, proposal.south, proposal.requestedTime);
        } else {
            subaru.schedule(proposal.north, proposal.south, proposal.requestedTime);
        }
    };

    var unscheduleProposal = function(proposal) {
        if (proposal.scope === 'Keck') {
            keck.unschedule(proposal.north, proposal.south, proposal.requestedTime);
        } else {
            subaru.unschedule(proposal.north, proposal.south, proposal.requestedTime);
        }
    };

    //Handlers for Schedule buttons
    $(".scheduleButton").change(function() {
        var checkbox = $(this).children(".scheduleCheckbox")[0];
        var isSelected = checkbox.checked;
        var proposalId = $(this).attr("name");
        var proposal = exchangeProposals[proposalId];
        if(isSelected){
            scheduleProposal(proposal);
        }else{
            unscheduleProposal(proposal);
        }
        displayState();
    });


    $(".unschedule").hide();
    $('tr.exchangeScope td a.schedule').each(function() {
        $(this).click(function(event) {
            $(this).siblings('.unschedule').show();
            $(this).hide();
            var checkbox = $(this).siblings('span').find('input').first();
            var proposalId = checkbox.attr('value');
            var proposal = exchangeProposals[proposalId];
            scheduleProposal(proposal);
            checkbox.attr('checked', true);
            displayState();
            event.preventDefault();
        });
    });

    $('tr.exchangeScope td a.unschedule').each(function() {
        $(this).click(function(event) {
            $(this).siblings('.schedule').show();
            $(this).hide();
            var checkbox = $(this).siblings('span').find('input').first();
            var proposalId = checkbox.attr('value');
            var proposal = exchangeProposals[proposalId];
            unscheduleProposal(proposal);
            checkbox.attr('checked', false);
            displayState();
            event.preventDefault();
        });
    });

    var formatHours = function(hours) {
        return Math.round(hours * 10) / 10;
    };

    var setClaimedClass = function(scope, elements) {
        var difference = scope.claimed - scope.available;
        if (Math.abs(difference) < 0.1) {
            elements.each(function() {
                $(this).parent().addClass('success').removeClass('error');
            });
        } else {
            elements.each(function() {
                $(this).parent().addClass('error').removeClass('success');
            });
        }
    };

    var changeCost = function(scope, spanSelector, element, numerator) {
        return (scope.available != 0) ? element.closest('tr').find(spanSelector).attr('value') * (numerator / scope.available) : 0;
    };

    var displayState = function() {
        $('.northAvailable').each(function() {
            $(this).text(formatHours(keck.north + subaru.north));
            //tangle.setValue("scheduledExchangeProposalsNorthHours", keck.north + subaru.north);
        });

        $('.southAvailable').each(function() {
            $(this).text(formatHours(keck.south + subaru.south));
            //tangle.setValue("scheduledExchangeProposalsSouthHours", keck.south + subaru.south);
        });
        $('.keckAvailable').each(function() {
            $(this).text(formatHours(keck.available));
            //tangle.setValue("scheduledExchangeProposalsKeckHours", keck.available);
        });
        $('.subaruAvailable').each(function() {
            $(this).text(formatHours(subaru.available));
            //tangle.setValue("scheduledExchangeProposalsSubaruHours", subaru.available);
        });
        $('#keckRemaining').text(formatHours(keck.available - keck.claimed));
        $('#subaruRemaining').text(formatHours(subaru.available - subaru.claimed));

        setClaimedClass(keck, $('.keck'));
        setClaimedClass(subaru, $('.subaru'));

        $('.northCost').each(function() {
            var keckCost = changeCost(keck, '.keck', $(this), keck.north);
            var subaruCost = changeCost(subaru, '.subaru', $(this), subaru.north);
            var netNorthExchange = (keckCost + subaruCost);
            $(this).text(netNorthExchange.toFixed(2));
        });
        $('.southCost').each(function() {
            var keckCost = changeCost(keck, '.keck', $(this), keck.south);
            var subaruCost = changeCost(subaru, '.subaru', $(this), subaru.south);
            var netSouthExchange = (keckCost + subaruCost);
            $(this).text(netSouthExchange.toFixed(2));
        });
    };

    displayState();

    var allocationAffectors = $('.affectsAllocation');
    allocationAffectors.each(function() {
        $(this).change(function(event) {
            var keckTotal = 0;
            var subaruTotal = 0;
            $('.keck').each(function() {
                keckTotal += ($(this).attr('value')) * 1;
            });
        $('.subaru').each(function() {
            subaruTotal += ($(this).attr('value')) * 1;
        });
        keck.claimed = keckTotal;
        subaru.claimed = subaruTotal;
        displayState();
    });
});
}

    function buildTangle() {
        var model = {
            initialize: function() {
                //Total queue time
                this.queueTime = 0;
                //North or South
                this.site = "?";
                <c:forEach items = "${partners}" var = "partner">
                this.nominalPct${partner.partnerCountryKey} = ${partner.percentageShare};
                this.nominalHrs${partner.partnerCountryKey} = this.nominalPct${partner.partnerCountryKey}/ 100 * this.queueTime;

                // Manual "tweaks" set in "Partners" tab
                //TODO:DELETE
                //this.reductionsHrsNorth${partner.partnerCountryKey} = 0.0;
                //this.reductionsHrsSouth${partner.partnerCountryKey} = 0.0;
                //Total number of hours exchanged
                this.exchangesNorth${partner.partnerCountryKey} = 0.0;
                this.exchangesSouth${partner.partnerCountryKey} = 0.0;
                this.exchSubaru${partner.partnerCountryKey} = 0;
                this.exchKeck${partner.partnerCountryKey} = 0;

                //Net percentage and hours (after reductions and exchanges)
                this.pctNorth${partner.partnerCountryKey} = this.nominalPercent;
                this.pctSouth${partner.partnerCountryKey} = this.nominalPercent;
                this.hrsNorth${partner.partnerCountryKey} = 0;
                this.hrsSouth${partner.partnerCountryKey} = 0;
                </c:forEach>

                this.netNorthReductions = 0;
                this.netSouthReductions = 0;
                this.netNorthExchange = 0;
                this.netSouthExchange = 0;

                this.netNorthAdjustments = 0;
                this.netSouthAdjustments = 0;

                this.netNorthTotalTime = 0;
                this.netNorthTotalPct = 0;

                //this.scheduledExchangeProposalsNorthHours = 0;
                //this.scheduledExchangeProposalsSouthHours = 0;
                //this.scheduledExchangeProposalsKeckHours = 0;
                //this.scheduledExchangeProposalsSubaruHours = 0;
                this.subaruQueueStrategy = false;
            },
        update: function() {
            <c:forEach items = "${partners}" var = "partner" >
            this.nominalHrs${partner.partnerCountryKey} = this.nominalPct${partner.partnerCountryKey} / 100 * this.queueTime;

            this.adjustmentNorth${partner.partnerCountryKey} = this.exchangesNorth${partner.partnerCountryKey} - this.reductionsHrsNorth${partner.partnerCountryKey};
            this.adjustmentSouth${partner.partnerCountryKey} = this.exchangesSouth${partner.partnerCountryKey} - this.reductionsHrsSouth${partner.partnerCountryKey};

            this.hrsNorth${partner.partnerCountryKey} =
            this.nominalHrs${partner.partnerCountryKey} +
            this.adjustmentNorth${partner.partnerCountryKey} -
            this.exchSubaru${partner.partnerCountryKey} -
            this.exchKeck${partner.partnerCountryKey};
            this.pctNorth${partner.partnerCountryKey} = this.hrsNorth${partner.partnerCountryKey} / this.queueTime * 100;

            this.hrsSouth${partner.partnerCountryKey} = this.nominalHrs${partner.partnerCountryKey} +
            this.adjustmentSouth${partner.partnerCountryKey} -
            this.exchSubaru${partner.partnerCountryKey} -
            this.exchKeck${partner.partnerCountryKey};
            this.pctSouth${partner.partnerCountryKey} = this.hrsSouth${partner.partnerCountryKey} / this.queueTime * 100;

            </c:forEach >
            //Chile has no time in the north
            this.hrsNorthCL = 0;
            this.pctNorthCL = 0;
            //UH has no time in the south
            this.hrsSouthUH = 0;
            this.pctSouthCL = 0;

            this.netNorthReductions =
                <c:forEach items = "${partners}" var = "partner" varStatus = "status" >
                this.reductionsHrsNorth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach>;
            this.netSouthReductions =
                <c:forEach items="${partners}" var="partner" varStatus="status">
                this.reductionsHrsSouth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach >;

            this.netNorthExchange =
                <c:forEach items = "${partners}" var = "partner" varStatus = "status">
                this.exchangesNorth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach>;
            this.netSouthExchange =
                <c:forEach items="${partners}" var="partner" varStatus="status">
                this.exchangesSouth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach >;

            this.netNorthAdjustments =
                <c:forEach items = "${partners}" var = "partner" varStatus = "status" >
                this.adjustmentNorth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach>;
            this.netSouthAdjustments =
                <c:forEach items="${partners}" var="partner" varStatus="status">
                this.adjustmentSouth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach>;

            this.netNorthTotalTime =
                <c:forEach items = "${partners}" var = "partner" varStatus = "status" >
                this.hrsNorth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach>;

            this.netSouthTotalTime =
                <c:forEach items="${partners}" var="partner" varStatus="status">
                this.hrsSouth${partner.partnerCountryKey} ${not status.last ? '+' : ''}
                </c:forEach >;

            if($("#subaruQueueToggle").find("label.ui-state-active").length == 1){
                this.subaruQueueStrategy = $("#subaruQueueToggle").find("label.ui-state-active")[0].htmlFor == "subaruAsQueue";
            }
            onUpdatedSubaruQueueStrategy(this.subaruQueueStrategy);
        }
    };

    //N.B.: non-local var!
    var tangle = new Tangle(tabs, model);
    return tangle;
}

function reactiveExchangeTab(tangle) {
    var tabs = $("#tabs").get(0);

    //Attach tangle handler to contribution fields
    <c:forEach items = "${partners}" var = "partner">

    var keckId = "exchKeck${partner.partnerCountryKey}";
    var subaruId = "exchSubaru${partner.partnerCountryKey}";

    $(document.getElementById(keckId)).change(function() {
        var val = $(this).val().toFloat();
        tangle.setValue($(this).attr("id"), val);
        //displayState();
    });
    $(document.getElementById(subaruId)).change(function() {
        var val = $(this).val().toFloat();
        tangle.setValue($(this).attr("id"), val);
        //displayState();
    });

    </c:forEach>
}

    console.log("Subaru scheduling strategy buttonset begin");
    // Set up toggling buttons for Subaru scheduling strategy
    //ITAC-627: Firefox ESR Sep 2012 defect
    if ( $.browser.mozilla != true || $.browser.version >= '15.0' ) {
        $("#subaruQueueToggle").buttonset();
    }

    $("#subaruAsQueue, #subaruAsExchange").change(function() {
    var useQueue = $("#subaruAsQueue").attr("checked");
    onUpdatedSubaruQueueStrategy(useQueue);
});
console.log("Subaru scheduling buttonset: OK")

//Labels are bold, we don't want that for our schedule labels
$(".scheduleLabel").css("font-weight", "normal");

function onUpdatedSubaruQueueStrategy(useQueue) {
    //Set labels
    $("#scheduleHeader").text(useQueue ? "Consider" : "Schedule");
    $(".scheduleLabel").text(useQueue ? "Consider" : "Schedule");
    this.subaruQueueStrategy = useQueue;
    //Set hidden field used to hold data
    $("#subaruStrategyUseQueue").val(useQueue);
    //Toggle tangle var
    //tangle.setValue("subaruQueueStrategy", ! tangle.getValue("subaruQueueStrategy"));
}
