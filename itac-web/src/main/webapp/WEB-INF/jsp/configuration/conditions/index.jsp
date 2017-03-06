<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">
				<ol id="condition-list">
					<c:forEach items="${conditionSets}" var="conditionSet">
						<li id="condition-set-${conditionSet.id}" class="condition">
							<h3><a href="#">${conditionSet.name} - ${fn:length(conditionSet.conditions)} condition bins</a></h3>
							<div>
								<form action="#">
									<table>
										<thead>
											<tr>
												<th>Name</th>
												<th>IQ</th>
												<th>BG</th>
												<th>CC</th>
												<th>WV</th>
												<th>Available</th>
											</tr>
										</thead>
										<tbody>
											<c:forEach items="${conditionSet.conditions}" var="condition"> 
											<tr>
												<td><input type="text" size="30" disabled value="${condition.name}"/></td>
												<td><input type="text" size="4" disabled value="${condition.imageQuality.displayName}"/></td>
												<td><input type="text" size="4" disabled value="${condition.skyBackground.displayName}"/></td>
												<td><input type="text" size="4" disabled value="${condition.cloudCover.displayName}"/></td>
												<td><input type="text" size="2" disabled value="${condition.waterVapor.displayName}"/></td>
												<td><input class="percent" type="text" size="4" disabled value="${condition.availablePercentage}"/></td>
											</tr>
											</c:forEach>
										</tbody>
									</table>
									<a href="/tac/configuration/condition-bins/${conditionSet.id}/copy">Copy</a>
								</form>
							</div>
						</li>
					</c:forEach>
				</ol>
			</div>
			
			<script type="text/javascript">
				$(function() {
					$(".condition h3 a").button();
					$('.condition h3 a').toggle(function() {
						var targetDiv = $(this).closest("li").children("div");
  						targetDiv.slideDown();
					}, function() {
						var targetDiv = $(this).closest("li").children("div");
  						targetDiv.slideUp();
					});	
					$(".condition div").hide();									
				});
			</script>
				
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>




