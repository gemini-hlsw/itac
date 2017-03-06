<%@ include file="../../fragments/header.jspf" %>
			<div class="span-18 colborder ">
                <h2>Can not automatically delete proposal "${proposal.phaseIProposal.title}"</h2>

                <p>This proposal is a component of a joint proposal. You must remove it from the joint proposal before deleting it.</p>
                <a class="button" id="confirm" href="../joint">Continue to joint proposal editing</a>
                <a class="button" id="cancel" href=".">Return</a>
			</div>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript">
	$(document).ready(function() {
		$('#confirm').button();
        $('#cancel').button();
	});
</script>
