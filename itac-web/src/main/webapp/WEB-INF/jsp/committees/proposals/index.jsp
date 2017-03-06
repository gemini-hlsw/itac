<%@ include file="../../fragments/header.jspf" %>
			<div class="span-19 colborder ">
				<h2 style="display: inline" class="span-6">Proposals (<span id="proposalsShowing">${fn:length(committeeProposals)}</span>)</h2>
				<div>
					<p style="display: inline; float: right;" id="filter-header" class="span-6 last filter-button"><a href="#" id="filters_link">Excluded: None</a></p>
					<p style="display: inline; float: right;" id="sort-header" class="span-6 colborder sort-button"><a href="#" id="partners_link">Sort: Partner/Partner ID</a></p>
				</div>

				<%@ include file="../../fragments/_proposal_sort.jspf" %>
				<%@ include file="../../fragments/_proposal_filter.jspf" %>

				<hr/>
                <div id="all-accordion" style="display:none">
				    <c:forEach items="${committeeProposals}" var="proposal" varStatus ="status">
					<%@ include file="../../fragments/_accordion_proposal.jspf" %>
					</c:forEach>
				</div>
				<!-- end all-accordion -->
            </div>
			<script type="text/javascript">

				$(function() {
                    page.helpContent = 'This page contains information about all the proposals available for the committee to review.  ' +
                        'It supports filtering and sorting those proposals according to a fixed set of criteria.' +
                        '  Click on a proposal to expand and view additional information.  Within the additional ' +
                        'information, you will be able to view the proposal details.';


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

<script type="text/javascript" src="/static/javascript/itac/proposal_filter.js"></script>
<script type="text/javascript" src="/static/javascript/itac/proposal_sort.js"></script>
<script type="text/javascript">
    var filterCleared = false;

    $(document).bind("ready", function(){
        $('#all-accordion').fadeIn(3000);

        //ITAC-548: Admin and Secretary see all proposals, others filtered to partner
        if(${adminOrSecretary}  || sessionStorage.getItem("partnerFilter") == "false"){
        } else {
            //Set filters to exclude non-partner values
            var partner = "${user.partner.abbreviation}";
            if(typeof partner != 'undefined'){
                var selector = "#only-" + partner;
                var checkbox = $(selector);
                checkbox.attr("checked", true);
                excludeAllBut(checkbox);
                var selectors = $(".only-control input");

            }
        }
    });
</script>

<c:set var="proposals" value="${committeeProposals}"/>
<%@ include file="../../fragments/_proposal_sort_generators.jspf" %>

