<%@ include file="../../../fragments/header.jspf" %>
<div class="span-19 colborder">
    <h1>Bulk Export</h1>
    <p>Select proposals to be exported as HTML. Press "Export" when ready.</p>
    <form name="proposals" action="export/export-html-do" method="GET">
        <button type="submit" name="submit" value="submit">Export</button>
        <span style="float: right">
            <button type="button" id="select-all" value="select">Select all</button>
            <button type="button" id="deselect-all" value="deselect">Deselect all</button>
        </span>
        <ul id="list" class="manual proposals">
            <c:forEach var="proposal" items="${proposals}">
                <c:set var="sortKey" value="${proposal.partner.abbreviation}_${proposal.partnerReferenceNumber}_${proposal.phaseIProposal.investigators.pi.lastName}"/>
                <li id="${sortKey}"><input type="checkbox" selected="false" name="ids" value="${proposal.id}">
                ${proposal.partner.abbreviation} ${proposal.partnerReferenceNumber} ${proposal.phaseIProposal.investigators.pi.lastName} "${proposal.phaseIProposal.title}"</li>
            </c:forEach>
        </ul>
    </form>
</div>

<script type="text/javascript">
    function id_compare(a,b) {
         return ($(a).attr('id') > $(b).attr('id') ? 1 : (($(a).attr('id') < $(b).attr('id')) ? -1 : 0));
    }

    $(function() {
        var ids = $("li");
        ids.sort(id_compare);
        var length = ids.length;
        for(var i = 0; i < length; i++){
            var id = ids[i];
            $(id).detach();
            $("#list").append($(id));
        }

        $("#select-all").click(function() {
            $("input[name=ids]").each(function() {
                this.checked = true;
            });

            return false;
        });

        $("#deselect-all").click(function() {
            $("input[name=ids]").each(function() {
                this.checked = false;
            });

            return false;
        });


		<!-- help -->
        page.helpContent =
            'Lorem ipsum.'
    });

</script>

<%@ include file="../../../fragments/sidebar.jspf" %>
<%@ include file="../../../fragments/footer.jspf" %>
