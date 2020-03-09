<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">
                <div class="span-18">
                    <h2 style="display: inline">${queue.name}</h2></br>
                   <h3 style="display: inline">Queue Constraints - ${queue.detailsString}</h3>
                 </div>

                </hr>
                <div style="clear: both">&nbsp;</div>

				<div id="tabs">
					<ul>
						<li><a href="#tabs-1">Initial</a></li>
						<li><a href="#tabs-3">Band 3</a></li>
						<li><a href="#tabs-4">Restrictions</a></li>
						<li><a href="#tabs-5">Partner Charges</a></li>
						<li><a href="#tabs-6">Summary</a></li>
					</ul>
					<form action="#" method="POST">
					<div id="tabs-1">
						<%@ include file="create/_initial.jspf" %>
					</div>
					<div id="tabs-3">
						<%@ include file="create/_band.jspf" %>					
					</div>
					<div id="tabs-4">
						<%@ include file="create/_restrictions.jspf" %>
					</div>
					<div id="tabs-5">
						<%@ include file="../../fragments/_partner_charges.jspf" %>
					</div>
					<div id="tabs-6">
						<%@ include file="create/_initial.jspf" %>
						<%@ include file="create/_band.jspf" %>
						<%@ include file="create/_restrictions.jspf" %>
						<%@ include file="../../fragments/_partner_charges.jspf" %>
					</div>
					</form>
				</div>
			</div>
			<script type="text/javascript">
	
				$(function() {
					$("#tabs").tabs();
					
					$("#tabs").find("input").each(function() {
						$(this).attr("disabled", "disabled");
					});
					$("#tabs").find("option").each(function() {
						$(this).attr("disabled", "disabled");
					});
					$("#tabs").find("textarea").each(function() {
						$(this).attr("disabled", "disabled");
					});		
				});
			</script>		
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
