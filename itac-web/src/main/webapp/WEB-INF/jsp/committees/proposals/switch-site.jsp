<%@ include file="../../fragments/header.jspf" %>


<div class="span-18 colborder ">
    <h2>Proposal "${ proposal.phaseIProposal.title }" currently intended for Gemini ${ proposal.site.displayName }</h2>

    <c:catch var="exception">

        <ul>
            <c:forEach items="${ proposal.phaseIProposal.blueprints }" var="blueprint" varStatus="resourceStatus">
                <li>
                    Instead of ${ blueprint.display }, moved proposal will use ${ blueprint.complementaryInstrumentBlueprint.display }
                </li>
            </c:forEach>
        </ul>
    </c:catch>
    <c:if test="${exception!=null}">
        <h1>
            <c:out value="${exception.message}"/>
            <c:out value="${exception.stackTrace}"/>
        </h1>
    </c:if>

    <c:if test="${exception == null}">
        <c:set var="newSite" value="${ proposal.site.displayName == 'North' ? 'South' : 'North' }"/>
        <form action="switch-site" method="POST" name="confirmForm"/>
            <a class="button" id="confirm" href="#"
               onClick="document.confirmForm.submit(); return false;" method="POST">
                Confirm move to Gemini ${ newSite }
            </a>
        </c:if>
    <a class="button" href=".">Cancel</a>

</div>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

<script type="text/javascript">
		$(function() {
			$("a.button").button();
            var label = $("a#confirm").button( "option", "label");
			$("a#confirm").button( "option", "label", "<strong>" + label + "</strong>");
		});
	</script>
