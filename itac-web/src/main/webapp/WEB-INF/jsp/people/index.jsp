<%@ include file="../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">
				<form action="/tac/people/" id="me">
					<p>
						<label class="span-2" for="name">Name</label>
						<input type="text" id="name" value="Devin Dawson">
						<a href="#">Edit</a>
					</p>
					<p>
						<label class="span-2" for="password">Password</label>
						<input type="password" id="password" value="secret">
					</p>					
					<p>
						<input type="submit" value="Save changes">
					</p>
				</form>
			</div>	
			
			<script type="text/javascript">
				$(function() {
				});
			</script>
<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
