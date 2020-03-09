<%@ include file="../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">

				<form action="/tac/session" method="POST">
					<fieldset>
						<legend>Login</legend>				
						<p>
							<label class="span-2" for="userName">Name</label>
							<input type="text" id="user-name" name="userName">
							<span style="float: right">
								<input type="SUBMIT" value="Login">
							</span>
						</p>
						<p>
							<label class="span-2" for="password">Password</label>
							<input type="password" id="password" name="password">
						</p>					
					</fieldset>
				</form>
			</div>	
			
			<script type="text/javascript">
				$(function() {
					//$("#submit").button();

				});
			</script>
<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
