<%@ include file="../../fragments/header.jspf" %>


			<div class="span-19 colborder">
                <h2 style="display: inline">Reports for committee ${committee.name}</h2>

                <!-- deal with errors and empty reports-->
                <%@ include file="../../fragments/_report_errors.jspf" %>

                <div class="span-18 box">
                    <h3 class="box-header"><a href="#" name="miscellaneous-reports">Reports for Gemini North</a></h3>
                    <ul>
                        <li><a href="/tac/committees/${ committee.id }/reports/raDistributionByInstrument.pdf?site=North">RA Distribution by instrument</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/observationTimeByInstrument.pdf?site=North">Observation time by instrument</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/duplicateTargets.pdf?site=North">Identical targets from multiple proposals</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/schedulingConstraints.pdf?site=North">Scheduling constraints</a></li>
                    </ul>
                    <ul>
                        <li>All instrument configurations <a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?site=North">(PDF)</a>, <a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.csv?site=North">(CSV)</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=GMOS_N">GMOS North instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=GNIRS">GNIRS instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=NIRI">NIRI instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=NIFS">NIFS instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=MICHELLE">Michelle instrument configurations</a></li>
                    </ul>
                </div>
                <div class="span-18 box">
                    <h3 class="box-header"><a href="#" name="miscellaneous-reports">Reports for Gemini South</a></h3>
                    <ul>
                        <li><a href="/tac/committees/${ committee.id }/reports/raDistributionByInstrument.pdf?site=South">RA Distribution by instrument</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/observationTimeByInstrument.pdf?site=South">Observation time by instrument</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/duplicateTargets.pdf?site=South">Identical targets from multiple proposals</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/schedulingConstraints.pdf?site=South">Scheduling constraints</a></li>
                    </ul>
                    <ul>
                        <li>All instrument configurations <a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?site=South">(PDF)</a>, <a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.csv?site=South">(CSV)</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=GMOS_S">GMOS South instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=GSAOI">GSAOI instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=NICI">NICI instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=TRECS">T-ReCS instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?instrument=FLAMINGOS2">Flamingos-2 instrument configurations</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/niciRestrictedTargets.pdf">NICI restricted targets</a></li>
                    </ul>
                </div>
                <div class="span-18 box">
                    <h3 class="box-header"><a href="#" name="miscellaneous-reports">Reports for Partners</a></h3>
                    <table>
                        <tr>
                            <th>Partner</th>
                            <th>Instrument configuration report</th>
                            <th>Partner submissions</th>
                        </tr>
                        <c:forEach items="${ allPartners }" var="partner">
                        <tr>
                            <td>
                                ${ partner.name }
                                <span class="flagMe right"><span style="display: none">${partner.abbreviation}</span></span>
                            </td>
                            <td>
                                <a href="/tac/committees/${ committee.id }/reports/instrumentConfiguration.pdf?partner=${partner.abbreviation}">(PDF)</a>
                            </td>
                            <td>
                                <a href="/tac/committees/${ committee.id }/reports/proposalSubmissions.pdf?partner=${partner.abbreviation}">(PDF)</a>, <a href="/tac/committees/${ committee.id }/reports/proposalSubmissions.csv?partner=${ partner.abbreviation }">(CSV)</a>
                            </td>
                        </tr>
                        </c:forEach>
                    </table>
                   <br/>
                </div>

                <div class="span-18 box">
                    <h3 class="box-header"><a href="#" name="exchange-reports">Reports for Exchanges</a></h3>
                    <ul>
                        <li><a href="/tac/committees/${ committee.id }/reports/raDistributionByInstrument.pdf?site=Subaru">RA Distribution by instrument for Subaru</a></li>
                        <li><a href="/tac/committees/${ committee.id }/reports/raDistributionByInstrument.pdf?site=Keck">RA Distribution by instrument for Keck</a></li>
                    </ul>
                </div>


			</div>
	<script type="text/javascript">

		$(function() {
            page.helpContent = 'A list of all reports available for this committee.  As an aside, rather than using ' +
            'the canned links above you can compose basic reports with the following filters: ' +
            'site -> [North, South, Subaru, Keck], partner [AU, GeminiStaff, Keck, Subaru, etc...], ' +
            'instrument -> [SUBARU, KECK, GMOS_N, GMOS_S, GNIRS, MICHELLE, NICI, NIFS, NIRI, PHOENIX, FLAMINGOS2, TRECS, GSAOI].';
		});
	</script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
