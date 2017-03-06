<%@ include file="../../fragments/header.jspf" %>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
</security:authorize>
			<div class="span-18 colborder ">

                <div class="span-11">
                    <h2>Queue Log - ${queue.name}</h2></br>
                    <h3>${queue.detailsString} - <span id="proposalsShowing">${fn:length(queue.bandings)}</span></h3>
                    <h3>Created - <fmt:formatDate value="${ queue.createdTimestamp }" type="both" dateStyle="short" timeStyle="long" /></span>
                </div>
				<div class="span-6">
                    <div id="configure-filters" class="span-6 last">
                        <form action="#" id="tag-filter" style="float: right">
                            <fieldset>
                            <legend>filters <i>(freeform search)</i></legend>
                            <input type="text" id="search">
                            </fieldset>
                        </form>
				    </div>
				</div>
                <div class="span-17">
                    <h4>
                        <table>
                            <tr>
                                <td><a href="#queue-time-table">Queue Programs</a></td>
                                <td><a href="#queue-decision-log-b12">Decision log Band 1/2</a></td>
                                <td><a href="#queue-decision-log-b3">Decision log Band 3</a></td>
                                <td><a href="#queue-decision-log-pw">Decision log PW</a></td>
                            </tr>
                        </table>
                    </h4>
                </div>


                <hr/>

				<div style="clear: both">&nbsp;</div>
				<div id="log">
                    <ul id="log-items">
                        <c:forEach items="${ logEntries }" var="logEntry">
                            <%@ include file="../../fragments/_log_entry.jspf" %>
                        </c:forEach>
                    </ul>
				</div>
			</div>

<script type="text/javascript">
    $(document).ready(function() {
        page.helpContent = '<p>This page lists events relevant to the queue.  Timestamps of when queues are generated, proposals edited, etc.' +
            'You can select only specific events by using the various filters.</p>' +
            '<p>A comment like ""Bands1&2 - Accepted - US rank 67.0. Partner: 45.2% (118.30 / 262.00 hours). Overall: 28.1% (402.35 / 1432.00 hours)." ' +
            'can be interpreted in a more verbose form as saying that the proposal was accepted in Band 1 or 2 (i.e., using the normal observing ' +
            'conditions, not Band 3 conditions), and was the US&apos;s 67th ranked proposal. After it was added, 45.2% of the total ' +
            'available time for US proposals had been allocated (118.3 out of 262 hours) and 28.1% of queue time overall had been ' +
            'allocated (402.35 out of 1432 hours)</p>' +
            '<p>Partners have an additional filter available to them: the ability to toggle between a view of only decisions related to proposals' +
            'that they are individual or primary partner on, or all queue decisions.</p>';

        $('input#search').quicksearch('ul#log-items li.PROPOSAL_QUEUE_GENERATED div form table tbody tr');

/*
        In the queue program area, the sort by index or cumulative doesnt seem to work, the  sequence get broken, I think by joints. Also, sorting on partner or references breaks because top level entries for joints, with blank entries for these values, are put to top. We may have to disable sorting on the queue programs for now, unless you see an easy fix.
        $('#queue-time-table').dataTable({
                "bPaginate": false,
                "bFilter": true,
                "bSort": true,
                "bInfo": false,
                "aaSorting": [[ 0, "asc" ]],
                "sDom": '<"top"f>t',
                "aoColumns": [
                  { "sType": "num-html" },
                  { "sType": "num-html" },
                  { "sType": "num-html" },
                  { "sType": "html" },
                  { "sType": "html" },
                  { "sType": "html" },
                  { "sType": "num-html" },
                  { "sType": "html" },
                  { "sType": "html" }
                ]
            });
*/

        $('#queue-decision-log-b12, #queue-decision-log-b3, #queue-decision-log-pw').each(function() {
                $(this).dataTable({
                    "bPaginate": false,
                    "bFilter": true,
                    "bSort": true,
                    "bInfo": false,
                    "aaSorting": [[ 1, "asc" ]],
                    "sDom": '<"top"f>t',
                    "aoColumnDefs": [
                      { "bSortable": true, "sType": "html", "aTargets": [ 1, 3, 4, 5 ] },
                      { "bSortable": true, "sType": "num-html", "aTargets": [ 0, 2 ] },
                      { "bSortable": false, "aTargets": [ 6 ] }
                    ]
                });
            });

        var otherPartnerClasses = [
        <c:forEach items="${ partners }" var="partner" varStatus="status">
            <c:set var="comma" value="${ (!status.last) ? ',' : ''}"/>
            <c:if test="${ user.partner ne partner }">
                <%-- Queue Engine does not seem to know anything about partner country keys nor have consistent abbreviations. --%>
                <c:set var="partnerAbbreviation" value="${ (partner.abbreviation eq 'GeminiStaff') ? 'GS' : partner.abbreviation }"/>
                "tbody tr.partner-${partnerAbbreviation}"${comma}
            </c:if>
        </c:forEach>
        ];

        $('#qtt-partner-toggle, #qdl-partner-toggle').button().addClass('right').find('span').addClass('loud').css('font-size','1.2em');
        $('#qtt-partner-toggle, #qdl-partner-toggle').toggle(function() {
                var parentElement = '';
                if ($(this).attr('id') == 'qtt-partner-toggle')
                    parentElement = $('#queue-time-table');
                else
                    parentElement = $('#queue-decision-log-b12, #queue-decision-log-b3, #queue-decision-log-pw')
                parentElement.each(function() {
                    $(this).find(otherPartnerClasses.join(', ')).hide();
                });
            }, function() {
                var parentElement = '';
                if ($(this).attr('id') == 'qtt-partner-toggle')
                    parentElement = $('#queue-time-table');
                else
                    parentElement = $('#queue-decision-log-b12, #queue-decision-log-b3, #queue-decision-log-pw')
                parentElement.each(function() {
                    $(this).find(otherPartnerClasses.join(', ')).show();
                });
            });
        <c:if test="${ !isAdmin }">
            $('#qtt-partner-toggle, #qdl-partner-toggle').each(function() { $(this).click(); });
        </c:if>
        <c:if test="${ isAdmin }">
            $('#qtt-partner-toggle, #qdl-partner-toggle').each(function() { $(this).hide(); });
        </c:if>

    });
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript" src="/static/javascript/itac/log_filter.js"></script>
