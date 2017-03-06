<%@ include file="../../fragments/header.jspf" %>


            <div class="span-19 colborder ">

                <table id="all-queues">
                    <caption><span class="large"><b>Queues (<span id="queuesShowing">${fn:length(orderedQueues)}</span>)</b></span></h2>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Site</th>
                            <th>Final</th>
                            <th style="text-align: center">Summary</th>
                            <th style="text-align: center">Log</th>
                            <th style="text-align: center">Timestamp</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${orderedQueues}" var="queue">
                        <tr>
                            <td class="queueDetailLink"><a href="/tac/committees/${committee.id}/queues/${queue.id}">${ queue.name }<span class="right ui-icon ui-icon-circle-triangle-e queueDetailsLink"></span></a></td>
                            <td>${ queue.site.displayName }</td>
                            <td>
                            <c:if test="${ queue.finalized }">Yes</c:if>
                            <c:if test="${ !queue.finalized }">No</c:if>
                            </td>
                            <td class="queueSummaryLink" style="text-align: center"><div id="toggle-${queue.id}" class="queueDetailsToggle"><small>show</small></div></td>
                            <td style="text-align: center" class="queueLogLink">
                                <a href="/tac/committees/${committee.id}/queues/${queue.id}/log"><img src="/static/images/fromOCS2PIT/book.png"></img></a>
                            </td>

                            <td style="text-align: center"><fmt:formatDate value="${ queue.createdTimestamp }" type="both" dateStyle="default" timeStyle="short" /></td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <c:forEach items="${orderedQueues}" var="queue">
                    <div id="queue-${queue.id}-dialog" class="queueDetailsDialog" title="${ queue.name } - ${ queue.detailsString } - ${fn:length(queue.bandings)} - <fmt:formatDate value="${ queue.createdTimestamp }" type="both" dateStyle="short" timeStyle="long" />">
                            <%@ include file="../../fragments/_partner_charges.jspf" %>

                            <hr/>
                            <div class="box span-6 right last" style="padding: 0px; margin: 0px;">
                                <ol style="list-style-type: none">
                                    <li><a href="/tac/committees/${committee.id}/queues/${queue.id}">List of queue proposals</a>
                                    <li><a href="/tac/committees/${committee.id}/queues/${queue.id}/log">Queue log</a></li>
                                </ol>
                            </div>
                            <c:if test="${showSubaru[queue.id]}">
                                <p class="span-10">Subaru Scheduling: ${subaruClassical[queue.id]} classical hours, ${subaruQueue[queue.id]} queue hours.</p>
                            </c:if>
                            <p class="span-10">Band cutoffs (1/2/3): ${queue.band1Cutoff}, ${queue.band2Cutoff}, ${queue.band3Cutoff}</p>
                            <p class="span-10">Band 3 conditions threshold: ${queue.band3ConditionsThreshold}</p>
                            <p class="span-10">Band 3 used after threshold crossed?: ${queue.useBand3AfterThresholdCrossed}</p>
                            <hr/>

                            <p>Band Restriction Rules</p>
                            <ul>
                                <c:forEach items="${queue.bandRestrictionRules}" var="bandRestrictionRule">
                                    <li>${ bandRestrictionRule.name }</li>
                                </c:forEach>
                            </ul>

                            <p><a href="/tac/configuration/radec">Bin configuration: ${queue.binConfiguration.name}</a></p>
                            <hr/>

                            <p>
                                Queue notes
                                <ul>
                                <c:forEach items="${queue.notes}" var="note">
                                    <li>${ note.note }</li>
                                </c:forEach>
                                </ul>
                            </p>

                            <p>
                                <c:choose>
                                <c:when test="${ not empty queue.rolloverSet }">
                                <a href="/tac/committees/${ committee.id }/rollovers/${ queue.site.displayName }/rolloverset/${ queue.rolloverSet.id}">Rollovers</a>
                                </c:when>
                                <c:otherwise>
                                No rollover information associated with queue.
                                </c:otherwise>
                                </c:choose>
                            </p>

                        </div>
                </c:forEach>
            </div>
            <script type="text/javascript">

                $(function() {
                    page.helpContent = 'Displays previously generated queues (if any) and allows the ITAC secretary to ' +
                        'generate new queues.  By clicking into the summary "show" button, you will be able to view a summary, and then ' +
                        'access additional details via the link associated with the queue name.';

                    $('#all-queues').dataTable({
                        "bPaginate": false,
                        "bFilter": true,
                        "bSort": true,
                        "bInfo": false,
                        "aaSorting": [[ 2, "desc" ], [5, "desc"]],
                        "sDom": '<"top"f>t',
                        "aoColumnDefs": [
                          { "bSortable": true, "sType": "html", "aTargets": [ 0, 1, 2 ] },
                          { "bSortable": false, "aTargets": [ 3, 4 ] },
                          { "bSortable": true, "sType": "date", "aTargets": [ 5 ] }
                        ]
                    });


                    $( ".queueDetailsDialog" ).dialog({ autoOpen: false, width: 700 });
                    $('.queueDetailsToggle').each(function() {
                        $(this).button();
                        var queueId = $(this).attr('id');
                        $(this).click(function() {
                            var id = onlyAfterLast(queueId, '-');
                            $('#queue-' + id + '-dialog').dialog('open');
                        });

                    });

                    $(".queueSummaryLink").each(function() {
                        $(this).qtip({
                            content: 'Click to view a <span class="large"><b>summary</b></span> of queue information including partner charges.',
                            show: 'mouseover',
                            hide: 'mouseout',
                            position: {
                                corner: {
                                    target: 'center',
                                    tooltip: 'topRight'
                                }
                            }
                        });
                    });

                    $(".queueDetailLink").each(function() {
                        $(this).qtip({
                            content: 'Click to view queue <span class="large"><b>details</b></span> including a list of proposals.',
                            show: 'mouseover',
                            hide: 'mouseout',
                            position: {
                                corner: {
                                    target: 'center',
                                    tooltip: 'topLeft'
                                }
                            }
                        });
                    });

                    $(".queueLogLink").each(function() {
                        $(this).qtip({
                            content: 'Click to view a <span class="large"><b>log</b></span> of actions on a queue including queueing decisions.',
                            show: 'mouseover',
                            hide: 'mouseout',
                            position: {
                                corner: {
                                    target: 'center',
                                    tooltip: 'topRight'
                                }
                            }
                        });
                    });
                });
            </script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
