<%@ include file="../../fragments/header.jspf" %>

<div class="span-18 colborder ">
    <h2>Proposal "${ proposal.phaseIProposal.title }" currently mode ${ proposal.phaseIProposal.observingMode } </h2>
    <i>ToO, band 3 request and visitor information is not preserved when toggling.  Please consider duplicating the
    proposal before making this lossy transformation.</i>

    <form action="toggle-mode" method="POST" name="confirmForm"/>
        <a class="button" id="confirm" href="#"
           onClick="document.confirmForm.submit(); return false;" method="POST">
            <c:choose>
            <c:when test="${ proposal.classical}">
            Confirm switch to queue mode?
            </c:when>
            <c:otherwise>
            Confirm switch to classical mode?
            </c:otherwise>
            </c:choose>
        </a>
        <a class="button" href=".">Cancel</a>
    </form>


</div>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

<script type="text/javascript">
		$(function() {
			$("a.button").button();
            var label = $("a#confirm").button( "option", "label");
			$("a#confirm").button( "option", "label", "<strong>" + label + "</strong>");
		});
</script>
