<%@ include file="../../fragments/header.jspf" %>
			<div class="span-18 colborder ">
				<div class="span-9">
					<h3 style="display: inline">Proposal log</h3>
				</div>
                <%@ include file="../../fragments/_log_filters.jspf" %>
				<div style="clear: both">&nbsp;</div>
				<div id="log">
                    <ul id="log-items">
                        <c:forEach items="${ logEntries }" var="logEntry">
                            <%@ include file="../../fragments/_log_entry.jspf" %>
                        </c:forEach>
                    </ul>
				</div>
			</div>

<script type="text/javascript">
    $(document).ready(function() {
        page.helpContent = 'This page lists events relevant to the proposal.  Timestamps of when an observation is deactivated, the proposal is reranked, etc.' +
            'You can select only specific events by using the various filters.';

        $('input#search').quicksearch('#log-items li');
    });
</script>


<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript" src="/static/javascript/itac/log_filter.js"></script>


