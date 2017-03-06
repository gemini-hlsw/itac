<%@ include file="../../fragments/header.jspf" %>

			
<div class="span-18 colborder ">
    <h2 style="display: inline">${queue.name}</h2></br>
    <h3 style="display: inline">Skeleton Export Result - ${queue.detailsString}</h3>

    <hr/>
    <div style="clear: both">&nbsp;</div>

    <c:if test="${failedResults > 0}">
        <div id="non-exported" class="error">
            <table>
                <caption class="loud">Failed exports:</caption>
                <thead>
                    <tr>
                        <th>Proposal</th>
                        <th>Status</th>
                        <th>Error</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${results}" var="result" varStatus ="status">
                        <c:if test="${!result.successful}">
                            <tr>
                                <td><a href="/tac/committees/${committee.id}/proposals/${result.proposalId}">${result.proposalTitle}</a></td>
                                <td>${result.status}</td>
                                <td>${result.error}</td>
                            </tr>
                        </c:if>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <c:if test="${succesfulResults > 0}">
        <div id="exported" class="success">
            <table>
                <caption class="loud">Successful exports:</caption>
                <thead>
                    <tr>
                        <th>Proposal</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${results}" var="result" varStatus ="status">
                        <c:if test="${result.successful}">
                            <tr>
                                <td><a href="/tac/committees/${committee.id}/proposals/${result.proposalId}">${result.proposalTitle}</a></td>
                            </tr>
                        </c:if>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

</div>
			
<script type="text/javascript">
	$(function() {
        var importerTables = ['#non-exported table', '#exported table'];

        $('#non-exported table').dataTable({
            "bPaginate": false,
            "bFilter": true,
            "bSort": true,
            "bInfo": false,
            "aaSorting": [[ 0, "asc" ]],
            "sDom": '<"top"f>t',
            "aoColumns": [
              { "sType": "html" },
              { "sType": "html" },
              { "sType": "html" }
            ]
        });

        $('#exported table').dataTable({
            "bPaginate": false,
            "bFilter": true,
            "bSort": true,
            "bInfo": false,
            "aaSorting": [[ 0, "asc" ]],
            "sDom": '<"top"f>t',
            "aoColumns": [
              { "sType": "html" }
            ]
        });
	});
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

