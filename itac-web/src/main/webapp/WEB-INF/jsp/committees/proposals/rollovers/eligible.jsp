<%@ include file="../../../fragments/header.jspf" %>


<div class="span-18 colborder ">
    <div class="span-18">
        <h2 style="display: inline">${queue.name}</h2><br/>
        <h3 style="display: inline">Rollovers - ${queue.site.displayName}<c:if test="${queue.finalized == true}"> - final</c:if></h3>
    </div>

    </hr>
    <div style="clear: both">&nbsp;</div>


    <h4>Indicate proposals eligible for rollover with a check</h4>

    <ul id="proposals">
        <c:forEach var="proposal" items="${proposals}">
            <li class="siteQualityRadio proposals">
                <c:set var="ite" value="${proposal.itac}"/>
                <c:set var="is_eligible" value="${ite.accept.rollover eq 'true' ? true : false }"/>
                <input class="rolloverEligible" type="checkbox" name="rolloverEligible" id="${proposal.id}"
                        <c:if test="${is_eligible}">
                            checked="true"
                        </c:if>
                        />${proposal.partner.abbreviation}
                    ${proposal.phaseIProposal.investigators.pi.lastName}
                <c:forEach items="${ proposal.phaseIProposal.submissions }" var="entry">
                    <c:if test="${ !empty entry.receipt.receiptId }">
                        <c:if test="${ !empty entry.partner.abbreviation }">${ fn:toUpperCase(entry.partner.abbreviation) }-</c:if>${ entry.receipt.receiptId }
                    </c:if>
                </c:forEach>
                "${proposal.phaseIProposal.title}"
                <c:if test="${proposal.jointComponent}">
                    (Component of Joint Proposal)
                </c:if>

            </li>
        </c:forEach>
    </ul>
</div>

<%@ include file="../../../fragments/sidebar.jspf" %>
<%@ include file="../../../fragments/footer.jspf" %>

<script type="text/javascript">

    $(function() {
        page.helpContent = 'Select proposals that are eligible for rollover.';

        //Sort by display value, which I happen to know is Partner / PI / title
        var els = $(".rolloverEligible");
        var sorted_els = els.sort(function(a, b) {
            var aText = a.parentNode.textContent.trim().toLowerCase();
            var bText = b.parentNode.textContent.trim().toLowerCase();
            return ((aText < bText) ? -1 : ((aText > bText) ? 1 : 0));
        });
        $.each(sorted_els, function(idx, el) {
            var pNode = $(el.parentNode);
            pNode.detach();
            $("#proposals").append(pNode);
        });


        //Handle the checkbox by sending off an Ajax query
        $(".rolloverEligible").change(function () {
            var proposalId = $(this).attr('id');
            var isEligible = $(this).attr('checked');
            $('body').css('cursor', 'wait');
            $.ajax({
                        url : 'rollover-eligible',
                        data : { 'proposalId' : proposalId, 'isRolloverEligible' : isEligible },
                        success :  function(responseData) {
                            //alert(responseData);
                            $('body').css('cursor', 'auto');
                        },
                        type : 'PUT'
                    });
        });
    });
</script>
