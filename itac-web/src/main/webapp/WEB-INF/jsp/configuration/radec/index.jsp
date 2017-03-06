<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-20 colborder ">
				<form action="#">
					<table>
						<thead>
							<tr>
								<td>Name</td>
								<td>RA Bin size (H:M)</td>
								<td>Dec bin size (D)</td>
							</tr>
						</thead>
						<tbody>
							<c:forEach items="${binConfigurations}" var="binConfiguration">
								<%@ include file="_binConfiguration.jspf" %>
							</c:forEach>
						</tbody>
					</table
				</form>
			</div>
			
			<script type="text/javascript">
				$(function() {
				});
			</script>
				
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>



