<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">
                <p>Delete proposal "${proposal.phaseIProposal.title}" <br/>
                    (${proposal.phaseIProposal.submissionsKey} | ${proposal.id})?</p>
                <form action="delete" method="POST" name="confirmForm"/>
                <a class="button" id="confirm" href="#"
                    onClick="document.confirmForm.submit(); return false;" method="POST">
                Yes, delete
                </a>
                <a class="button" id="cancel" href=".">No, return to original</a>
			</div>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript">
	$(document).ready(function() {
		$('#confirm').button();
        $('#cancel').button();
	});
</script>
