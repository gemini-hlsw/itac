<%@ include file="../../fragments/header.jspf" %>

<div class="span-18 colborder ">
    <h4>${message}</h4>
    <c:if test="${failedCount gt 0}">
        <div id="non-imported-files" class="error">
            <table>
                <caption class="loud">Failed imports:</caption>
                <thead>
                    <tr>
                        <th>Filename</th>
                        <th>Imported?</th>
                        <th>Message</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach items="${importerResults}" var="result">
                    <c:if test="${!result.successful}">
                       <tr>
                           <td>${result.fileName}</td><td>${result.state.description}</td><td>${result.message}</td>
                      </tr>
                    </c:if>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
    <c:if test="${successfulCount gt 0}">
        <div id="imported-files" class="success">
            <table>
                <caption class="loud">Successful imports:</caption>
                <thead>
                    <tr>
                        <th>Filename</th>
                        <th>Imported?</th>
                        <th>Message</th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach items="${importerResults}" var="result">
                    <c:if test="${result.successful}">
                       <tr>
                           <td>${result.fileName}</td><td>${result.state.description}</td><td>${result.message}</td>
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
        var importerTables = ['#non-imported-files table', '#imported-files table'];

        $('#non-imported-files table').dataTable({
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

        $('#imported-files table').dataTable({
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

		<!-- help -->
        page.helpContent =
            'For each file a status will be reported after the import.' +
            'If the import for a file failed, the whole operation will be rolled back unless the option "Ignore documents with errors" was selected.';
    });

	$(document).ready(function() {
		$("#file-upload input").button();
	});
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
