<%@ include file="../../fragments/header.jspf" %>

			
			<div id="reports" class="span-18 colborder ">
                <div class="span-17">
                    <h2 style="display: inline">${queue.name}</h2></br>
                   <h3 style="display: inline">Queue Reports - ${queue.detailsString}</h3>
                 </div>

                </hr>
                <div style="clear: both">&nbsp;</div>

                <%@ include file="../../fragments/_report_errors.jspf" %>

				<div class="span-17 box">
					<h3 class="box-header"><a href="#" name="miscellaneous-reports">Queue Reports</a></h3>

					<ul>
						<li>Proposals <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/proposals.pdf">(PDF)</a>, <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/proposals.csv">(CSV)</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/raDistributionByInstrument.pdf">RA Distribution by instrument</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/raDistributionByBand.pdf">RA Distribution by band</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observationTimeByInstrument.pdf">Observation time by instrument</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observationTimeByBand.pdf">Observation time by band</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observationTimeByPartner.pdf">Observation time by partner</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observingConditions.pdf">Observing conditions by band</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observingConditionsForBand1.pdf">Observing conditions for band 1</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observingConditionsForBand2.pdf">Observing conditions for band 2</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observingConditionsForBand3.pdf">Observing conditions for band 3</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/observingConditionsForBand4.pdf">Observing conditions for band 4 (poor weather)</a></li>
						<li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/duplicateTargets.pdf">Identical targets from multiple proposals</a></li>
                        <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/schedulingConstraints.pdf">Scheduling constraints</a></li>
				    </ul>
				    <c:choose>
                        <c:when test="${queue.site.displayName == 'North'}">
                            <ul>
                                <li>All instrument configurations <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf">(PDF)</a>, <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.csv?site=North">(CSV)</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=GMOS_N">GMOS North instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=GNIRS">GNIRS instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=NIRI">NIRI instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=NIFS">NIFS instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=MICHELLE">Michelle instrument configurations</a></li>
                            </ul>
                        </c:when>
                        <c:otherwise>
                            <ul>
                                <li>All instrument configurations <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf">(PDF)</a>, <a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.csv?site=South">(CSV)</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=GMOS_S">GMOS South instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=NICI">NICI instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=TRECS">T-ReCS instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/instrumentConfiguration.pdf?instrument=FLAMINGOS2">Flamingos2 instrument configurations</a></li>
                                <li><a href="/tac/committees/${ committee.id }/queues/${ queue.id }/reports/niciRestrictedTargets.pdf">NICI restricted targets</a></li>
                            </ul>
                        </c:otherwise>
                    </c:choose>
				</div>
				
			</div>
			<script type="text/javascript">
	
			</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
