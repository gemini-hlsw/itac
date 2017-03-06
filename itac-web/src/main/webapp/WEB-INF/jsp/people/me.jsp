<%@ include file="../fragments/header.jspf" %>

			
			<div class="span-8 colborder prepend-1">
				<form action="/tac/people/me" id="me" method="POST">
					<p>
						<label class="span-2" for="name">Name</label>
						<input type="text" id="name" name="name" class="required" value="${ user.name }">
					</p>
					<p>
						<label class="span-2" for="password">Password</label>
						<input type="password" id="password" name="password" minlength="4" value="${ user.password }">
					</p>
					<p>
						<label class="span-2" for="confirm-password">Confirm password</label>
						<input type="password" id="confirm-password" name="confirm-password" minlength="4" value="${ user.password }">
					</p>
					<p>
						<input type="submit" value="Save changes">
					</p>
				</form>
			</div>
			<div class="span-8 prepend-1">
                <p>Enabled: ${ user.enabled }</p>
                <p>${ user.partner.name }</p>
                <h4>Roles</h4>
                <ul>
                <c:forEach items="${ user.authorities}" var="authority">
                    <li>${ authority.rolename }</li>
                </c:forEach>
                </ul>
                <h4>Committees</h4>
                <ul>
                <c:forEach items="${ user.committees}" var="committee">
                    <li><a href="/tac/committees/${committee.id}">${ committee.name }</a></li>
                </c:forEach>
                </ul>
            </div>
			<hr/>
			<script type="text/javascript">
				$(function() {
					$('#me a').button();
					$('input:submit').button();
					$('#me').validate({
                        rules: {
                            password: "required",
                            confirm-password: {
                              equalTo: "#password"
                            }
                        }
					});
				});
			</script>
<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
