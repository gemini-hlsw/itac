<%@ include file="../../fragments/header.jspf" %>


			<div class="span-18 colborder ">
                <h3>Copying <a href="/tac/configuration/condition-bins/${conditionSet.id}">${conditionSet.name}</a></h3>
                <div>
                    <form action="/tac/configuration/condition-bins/${conditionSet.id}/copy" method="post">
                        <input type="text" size="30" name="name" value="${conditionSet.name}"/></td>
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
                                    <td><input class="percent" type="text" size="4" value="${condition.availablePercentage}" name="condition-${condition.id}"/></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                            <input type="submit"/>
                        </table>
                    </form>
                </div>
			</div>

			<script type="text/javascript">
				$(function() {
				});
			</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>




