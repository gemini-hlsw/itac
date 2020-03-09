<%@ include file="../../fragments/header.jspf" %>


			<div class="span-18 colborder ">
                <div class="span-18">
                    <h2 style="display: inline">${queue.name}</h2><br/>
                    <h3 style="display: inline">${ confirmTitle } - ${queue.detailsString}</h3>
                </div>

                </hr>
                <div style="clear: both">&nbsp;</div>

                <form action="${ action }" method="POST" name="confirmForm"/>
                <a class="button" id="confirm" href="#" method="POST">${ confirmYes }</a>
                <a class="button" id="cancel" href=".">${ confirmNo }</a>
			</div>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript">
    $(function() {
        page.helpContent = 'Confirm operation or cancel it.'
        });

    $(window).unload(function() {
            wait(false);
        });

	$(document).ready(function() {
		$('#confirm').button();
        $('#cancel').button();

        $('#confirm').click(function() {
            wait(true);
            document.confirmForm.submit();

            return false;
        });
	});
</script>
