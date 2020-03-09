<%@ include file="../../fragments/header.jspf" %>
			<div class="span-19 colborder ">
                <div class="span-8">
                    <h2 style="display: inline">${queue.name}</h2></br>
                    <h3 style="display: inline">Queue Emails - ${queue.detailsString}</h3>
                </div>

                <div class="span-9">
                    <p style="display: inline; float: right;" id="sort-header" class="span-6 colborder sort-button"><a href="#" id="partners_link">Sort: Band</a></p>
                </div>

                <%@ include file="../../fragments/_email_sort.jspf" %>


				<hr/>
                <div style="clear: both">&nbsp;</div>


				<div id="all-accordion" style="display:none">
					<c:forEach items="${emails}" var="email" varStatus ="status">
					<%@ include file="../../fragments/_accordion_email.jspf" %>
					</c:forEach>

				</div>       <!-- end all-accordion -->
            </div>
			<script type="text/javascript">

				$(function() {
                    page.helpContent = 'A list of all emails for a queue.';

					$("#tabs").tabs();

					$("#all-accordion").accordion({
						event: "click",
						clearStyle: true,
						collapsible: true,
						active: false
					});
				});
			</script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

<script type="text/javascript" src="/static/javascript/itac/email_sort.js"></script>

<script type="text/javascript">
    $(document).ready(function(){
    	$('#all-accordion').fadeIn(3000);
	});

    $(function() {
        $("div.controls input").button();
        $("div.controls input.save").hide();
        $("div.controls input.cancel").hide();
        $("div.controls input.send").hide();
        $("div.controls input.resend").hide();
        $("div.controls input.index").hide();

        $("div.controls input.edit").click(function() {
            $(this).siblings("input.save").show();
            $(this).siblings("input.cancel").show();
            $(this).siblings("input.send").show();
            $(this).siblings("input.resend").show();
            $(this).hide();
			$(this).parent().parent().children().find("textarea").attr("disabled", "");
        });
    });

</script>

<%@ include file="../../fragments/_email_sort_generators.jspf" %>

