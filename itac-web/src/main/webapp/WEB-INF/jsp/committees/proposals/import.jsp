<%@ include file="../../fragments/header.jspf" %>
<!--<div class="span-18 colborder ">-->
<div class="span-19 colborder">
    <h4>Please upload a file containing proposals for bulk import into this committee.  This can be a lengthy process, lasting between 2 to 3 minutes so please be patient and do not
    resubmit the form.</h4>
	<form id="file-upload" method="post" action="import" enctype="multipart/form-data">
	    <div>
	        <table>
	            <tbody>
	                <tr>
	                    <td class="label">
                            <label for="input-file">File name:</label>
                        </td>
                        <td>
                            <input id="input-file" type="file" name="file">
                        </td>
                    </tr>
	                <tr>
	                    <td class="label">
                            <label>Options:</label>
                        </td>
                        <td>
                	    	<fieldset>
	    	                    <input type="checkbox" name="replaceProposals" value="true">Replace all existing proposals</input><br>
	                        	<input type="checkbox" name="ignoreErrors" value="true">Ignore documents with errors</input>
	    	                </fieldset>
                        </td>
                    </tr>
                </tbody>
            </table>
		</div>
		<div>
	    	<input type="submit" value="Upload"/>
	    </div>
	</form>
</div>

<script type="text/javascript">
    <%--
	$(document).ready(function() {
		$("#file-upload input").button();
	});
	--%>

    $(function() {
		<!-- help -->
        page.helpContent =
            'Import any number of tarred and/or gzipped proposals.</br>' +
            'Choose one of the following options:</br>' +
            '<ul>' +
            '<li><b>Replace</b><br/>On the first occurrence of a proposal key that already exists, ' +
            'ALL existing proposals (including joints) that have the same proposal key will be deleted before ' +
            'importing the new ones. This will replace already existing proposals instead of merging them ' +
            'with the new ones.</li>' +
            '<li><b>Ignore</b><br/>If a document can not be imported properly instead of stopping the whole ' +
            'import the document will be ignored. This allows for partial imports in case single files cause ' +
            'problems and they can be ignored for now and imported later.</li>' +
            '</ul>' +
            '<br/>Note: We have encountered some difficulty with different archiving formats (a dialect of tar), and ' +
            'if you are facing difficulty getting a archive of proposals imported, one troubleshooting tip is to ' +
            'attempt to archive it with another program.'
    });

</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
