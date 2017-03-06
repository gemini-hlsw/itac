<%@ include file="../fragments/header.jspf" %>


			<div class="prepend-1 span-17 colborder ">
				<p>Committee id: ${ committee.id }</p>
				<p>Committee name: ${ committee.name }</p>
				<p>Committee semester: ${ committee.semester.displayName }</p>
				<c:if test="${ not empty lastGeneratedQueue }"><p><a href="/tac/committees/${ committee.id }/queues/${ lastGeneratedQueue.id }">Last generated queue - ${ lastGeneratedQueue.name }</a><p></c:if>
				<c:if test="${ not empty finalizedQueues }">
				    <c:forEach items="${ finalizedQueues }" var="queue">
				    <p><a href="/tac/committees/${ committee.id }/queues/${ queue.id}">Final - ${ queue.site.displayName } - ${ queue.name }</a></p>
				    </c:forEach>
				</c:if>
                <c:if test="${ committee.active }"><p>Active</p></c:if>
                <c:if test="${ not committee.active }"><p>Not Active</p></c:if>

			</div>

		    <script type="text/javascript">
                $(document).ready(function() {
                    page.helpContent = 'Displays basic information about the committee as well as providing a couple of' +
                        ' deep dive links to specific common destinations.';
                });
            </script>

<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
