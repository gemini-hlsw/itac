<%@ include file="../fragments/header.jspf" %>
			<div class="span-18 colborder ">
				<div class="span-9">
					<h3 style="display: inline">Committee log for ${ committee.name }</h3>
				</div>
                <%@ include file="../fragments/_log_filters.jspf" %>
				<div style="clear: both">&nbsp;</div>
				<div id="log">
                    <ul id="log-items">
                        <c:forEach items="${ logEntries }" var="logEntry">
                            <%@ include file="../fragments/_log_entry.jspf" %>
                        </c:forEach>
                    </ul>
				</div>
			</div>

<script type="text/javascript">
    $(document).ready(function() {
        page.helpContent = 'This page lists events relevant to the committee.  Timestamps of when queues are generated, proposals edited, etc.' +
            'You can select only specific events by using the various filters.';

        $('input#search').quicksearch('#log-items li');
        $('input#search').keyup();
    });
</script>


<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
<script type="text/javascript" src="/static/javascript/itac/log_filter.js"></script>


