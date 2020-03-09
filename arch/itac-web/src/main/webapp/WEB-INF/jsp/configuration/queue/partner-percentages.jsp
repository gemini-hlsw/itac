<%@ include file="../../fragments/header.jspf" %>


<div class="span-18 colborder ">
    <H1>Partner Percentages</H1>
    <H1 class="warning" style="display:none"><span class="percentAllocated">${ sumOfPercents }</span> Allocated</H1>
    <c:forEach items="${partnerList}" var="partner" varStatus="status">
        <c:set var="site" value="both"/>
        <c:if test="${partner.north == true && partner.south == false}">
            <c:set var="site" value="north"/>
        </c:if>
        <c:if test="${partner.north == false && partner.south == true}">
            <c:set var="site" value="south"/>
        </c:if>
        <h2>${partner.name}</h2>
        <p><span class="partner_percent" data-partner="${partner.name}" data-site="${site}">${ partner.percentageShare}</span>
        <c:if test="${site == 'north'}">
            &nbsp;(North Only)
        </c:if>
        <c:if test="${site == 'south'}">
            &nbsp;(South Only)
        </c:if>

        </p>
    </c:forEach>
</div>



<script type="text/javascript">
    function percentAllocated(){
        var north_total = 0;
        var south_total = 0;
        $('.partner_percent').each(function(ix, el){
            var pct = parseFloat(el.innerHTML.trim());
            if($(this).attr("data-site") == 'north'){
                north_total += pct;
            }else if($(this).attr("data-site") == 'south'){
                south_total += pct;
            }else { //Both
                north_total += pct;
                south_total += pct;
            }
        });
        return [south_total, north_total];
    }

    function validatePercentages(){
        var pcts = percentAllocated();
        $(".percentAllocated").html("South " + pcts[0] + "% North " + pcts[1] + "%");
        //Highlight if trouble
        if(pcts[0] != 100 || pcts[1] != 100){
            $('.span-18').css('background-color', '#FBE3E4');
            $('.warning').show();
        }else{
            $('.span-18').css('background-color', 'white');
            $('.warning').hide();
        }
    }
    $(function() {
        validatePercentages();

        //Editable text for %s
        $('span.partner_percent').editableText({
            newLinesEnabled: false
        });

        $('span.partner_percent').change(function() {
            var newValue = $(this).html().trim();
            $.ajax({
                type: "POST",
                url: "./partner-percentages",
                data: "partnerName=" + $(this).attr('data-partner') + "&newPercentage=" + newValue,
                success: function(data) {
                    var messageBox = $('#message_box');
                    var messageBoxMessage = $('#message_box_message');
                    messageBoxMessage.text('Percentage share successfully changed to ' + newValue + '.');
                    messageBoxMessage.addClass('success');
                    messageBox.fadeIn().delay(5000).fadeOut();
                    validatePercentages();
                },
                error: function(msg) {
                    var messageBox = $('#message_box');
                    var messageBoxMessage = $('#message_box_message');
                    messageBoxMessage.text('Change to percentages failed.  ' + msg);
                    messageBoxMessage.addClass('error');
                    messageBox.fadeIn().delay(5000).fadeOut();
                }
            });
        });

        <!-- highlight field on edit -->
        $('span.partner_percent').bind('onStartEditing', function() {
            $(this).css('background-color', '#ffc');
            $(this).css('border', '1px dotted');
            $(this).css('font-size', '16px');
            $(this).css('padding', '4px');
            oldValue = $(this).html();
        });
        <!-- Clear highlights -->
        $('span.partner_percent').bind('onStopEditing', function() {
            $(this).css('background-color', '');
            $(this).css('border', '');
            $(this).css('font-size', '');
            $(this).css('padding', '');
        });


        <!-- help -->
        page.helpContent =
                'Edit partner percentages. ';


    });
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
