<%@ include file="../fragments/header.jspf" %>
			<div class="span-23 prepend-1">
				<h2>Members</h3>
				<table id="members">
				    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Partner</th>
                        </tr>
				    </thead>
				    <tbody>
					<c:forEach items="${ committee.members }" var="member">
					    <tr>
						    <td>${ member.name }</td>
						    <td>${ member.partner.name }</td>
						</tr>
					</c:forEach>
					</tbody>
				</table>
			</div>

            <script type="text/javascript">
                $(document).ready(function() {
                    page.helpContent = 'This page lists the members of the current committee.';
                    $('#members').dataTable({
                        "bPaginate": false,
                        "bFilter": true,
                        "bSort": true,
                        "bInfo": false
                    });
                });
            </script>
<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
