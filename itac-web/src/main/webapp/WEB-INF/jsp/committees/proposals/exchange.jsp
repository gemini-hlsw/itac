<%@ include file="../../fragments/header.jspf" %>
			
			<div class="span-18 colborder ">
				<h4 style="display: inline">Exchange programs</h4>
				<div id="tabs">
					<ul>
						<span style="float: right">
							<li><a href="#tabs-1">for partner telescopes</a></li>
							<li><a href="#tabs-2">from partner telescopes</a></li>
							<li><a href="#tabs-3">by telescope and site</a></li>
							<li><a href="#tabs-4">by site</a></li>
							<li><a href="#tabs-5">by partner</a></li>
						</span>
					</ul>
					
					<div style="clear: both">&nbsp;</div>
					<div id="tabs-1">
					<table id="gemini-exchange-table-summary">
						<thead>
							<caption><a href="#" name="for-partner">Gemini proposals for exchange time</a></caption>
							<tr>
								<td></td>
								<th scope="col">Requested time</th>
								<th scope="col">Telescope</th>
								<th scope="col">Partner</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach items="${ forKeckProposals }" var="proposal" varStatus ="status">
							<c:set var="recommendedTime" value="${ proposal.phaseIProposal.totalRecommendedTime }"/>
							<tr class="error">
								<th scope="row"><a href="/tac/committees/${ committee.id }/proposals/${ proposal.id}">${ proposal.phaseIProposal.title }</a></th>
								<td><fmt:formatNumber value="${ recommendedTime.value }" type="number" maxFractionDigits="1" minFractionDigits="1"/> ${ recommendedTime.units }</td>
								<td>Keck</td>
								<td>${ proposal.partner.abbreviation }</td>
							</tr>
							</c:forEach>
							<c:forEach items="${ forSubaruProposals }" var="proposal" varStatus ="status">
							<c:set var="recommendedTime" value="${ proposal.phaseIProposal.totalRecommendedTime  }"/>
							<tr class="error">
								<th scope="row"><a href="/tac/committees/${ committee.id }/proposals/${ proposal.id}">${ proposal.phaseIProposal.title }</a></th>
								<td><fmt:formatNumber value="${ recommendedTime.value }" type="number" maxFractionDigits="1" minFractionDigits="1"/> ${ recommendedTime.units }</td>
								<td>Subaru</td>
								<td>${ proposal.partner.abbreviation }</td>
							</tr>
							</c:forEach>
						</tbody>
					</table>
					</div>

					<div id="tabs-2">
					<table id="partner-exchange-table-summary">
						<thead>
							<caption>
								<a href="#" name="from-partner">Observatory proposals for exchange time</a>
								<span style="float: right">Keck[<fmt:formatNumber value="${ exchangeStatistics.keckHoursAtSouth + exchangeStatistics.keckHoursAtNorth }" type="number" maxFractionDigits="1" minFractionDigits="1"/>] - Subaru[<fmt:formatNumber value="${ exchangeStatistics.subaruHoursAtNorth + exchangeStatistics.subaruHoursAtSouth}" type="number" maxFractionDigits="1" minFractionDigits="1"/>]</span>
							</caption>
							<tr>
								<td></td>
								<th scope="col">Requested time</th>
								<th scope="col">Site</th>
								<th scope="col">Partner</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach items="${ fromKeckProposals }" var="proposal" varStatus="status">
							<c:set var="recommendedTime" value="${ proposal.phaseIProposal.totalRecommendedTime  }"/>
							<tr class="error">
								<th scope="row"><a href="/tac/committees/${ committee.id }/proposals/${ proposal.id}">${ proposal.phaseIProposal.title }</a></th>
								<td><fmt:formatNumber value="${ recommendedTime.doubleValue }" type="number" maxFractionDigits="1" minFractionDigits="1"/> ${ recommendedTime.units.display }</td>
								<td>${ proposal.site.displayName }</td>
								<td>Keck</td>
							</tr>
							</c:forEach>
							<c:forEach items="${ fromSubaruProposals }" var="proposal" varStatus="status">
                            <c:set var="recommendedTime" value="${ proposal.phaseIProposal.totalRecommendedTime  }"/>
							<tr class="error">
								<th scope="row"><a href="/tac/committees/${ committee.id }/proposals/${ proposal.id}">${ proposal.phaseIProposal.title }</a></th>
								<td><fmt:formatNumber value="${ recommendedTime.doubleValue }" type="number" maxFractionDigits="1" minFractionDigits="1"/> ${ recommendedTime.units.display }</td>
								<td>${ proposal.site.displayName }</td>
								<td>Subaru</td>
							</tr>
							</c:forEach>
						</tbody>
					</table>
					</div>

					<div id="tabs-3" class="span-17">
					<table id="exchange-table-partner">
						<caption><a href="#" name="by-telescope-and-site">Exchange time requested by telescope</a></caption>
						<thead>
							<tr>
								<td></td>
								<th>Requested time</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<th scope="row">Keck for Gemini South</th>
								<td><fmt:formatNumber value="${ exchangeStatistics.keckHoursAtSouth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>

							</tr>
							<tr>
								<th scope="row">Keck for Gemini North</th>
								<td><fmt:formatNumber value="${ exchangeStatistics.keckHoursAtNorth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>
							</tr>
							<tr>
								<th scope="row">Subaru for Gemini South</th>
								<td><fmt:formatNumber value="${ exchangeStatistics.subaruHoursAtSouth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>
							</tr>
							<tr>
								<th scope="row">Subaru for Gemini North</th>
								<td><fmt:formatNumber value="${ exchangeStatistics.subaruHoursAtNorth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>
							</tr>
						</tbody>
					</table>
					</div>

					<div id="tabs-4">
						<table id="exchange-table-site">
							<caption>Exchange time requested by site</caption>
							<thead>
								<tr>
									<td></td>
									<th>Requested time</th>
								</tr>
							</thead>
							<tbody>
								<tr>
									<th scope="row">North</th>
									<td><fmt:formatNumber value="${ exchangeStatistics.totalHoursAtNorth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>
								</tr>
								<tr>
									<th scope="row">South</th>
									<td><fmt:formatNumber value="${ exchangeStatistics.totalHoursAtSouth }" minFractionDigits="1" maxFractionDigits="1" type="number"/></td>
								</tr>
							</tbody>
						</table>
					</div>

					<div id="tabs-5">
						<table id="exchange-table-site-keck">
							<caption><a href="#" name="by-partner">Exchange time requested by partners for Keck</a></caption>
							<thead>
								<tr>
									<td></td>
									<th>Requested time</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${ exchangeStatistics.partnerRequestKeck }" var="valuePair">
								<tr>
									<th scope="row">${ fn:toUpperCase(valuePair.key.partnerCountryKey)  }</th>
									<td>${ valuePair.value  }</td>
								</tr>
								</c:forEach>
							</tbody>
						</table>
						<table id="exchange-table-site-subaru">
							<caption><a href="#" name="by-partner">Exchange time requested by partners for Subaru</a></caption>
							<thead>
								<tr>
									<td></td>
									<th>Requested time</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${ exchangeStatistics.partnerRequestSubaru }" var="valuePair">
								<tr>
									<th scope="row">${ fn:toUpperCase(valuePair.key.partnerCountryKey)  }</th>
									<td>${ valuePair.value  }</td>
								</tr>
								</c:forEach>
							</tbody>
						</table>
					</div>
				</div>
			</div>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

<script type="text/javascript">
	$(document).ready(function() {
		$('#exchange-table-partner').visualize({
			type: 'pie', 
			height: 500,
			pieMargin: 50
		});
		$('#exchange-table-site').visualize({
		    type: 'pie',
		    height: 500,
		    pieMargin: 50
		});
		$("#tabs").tabs();
	});
</script>
