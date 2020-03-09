<%@ include file="../../fragments/header.jspf" %>
	<style type="text/css">
	#sortable1 li, #sortable2 li { margin: 0 5px 5px 5px; padding: 5px; font-size: 1.2em; width: 120px; }
	</style>
			
			<div class="span-18 colborder ">
				<h2>List</h2>
				<ol>
					<c:forEach items="${memberships}" var="membership">
						<!--<li><a href="/tac/people/${membership.person.id}">${membership.person.name}</a></li>-->
						<li>${membership.person.name}</li>
					</c:forEach>
				</ol>
			</div>
			<script type="text/javascript">
				$(function() {
				});
			</script>		
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
