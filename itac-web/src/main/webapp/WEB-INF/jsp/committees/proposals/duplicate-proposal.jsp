<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">
                <p>Create duplicate of proposal "${original.phaseIProposal.title}"?</p>
                <form action="duplicate" method="POST" name="confirmForm"/>
                <a class="button" id="confirm" href="#"
                    onClick="document.confirmForm.submit(); return false;" method="POST">
                Yes, duplicate
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
