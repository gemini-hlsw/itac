<%@ include file="../fragments/header.jspf" %>


            <div class="span-23 prepend-1">
                <h2>Committees</h2>
                <table id="committees">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Semester</th>
                            <th>${ user.partner.abbreviation }</th>
                            <th>Proposals</th>
                            <th>Queues</th>
                            <th>Members</th>
                            <th>Active?</th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${ committees }" var="committee">
                        <tr>
                        <c:if test="${committee.active}"><c:set var="activeDisplay" value="Active"/></c:if>
                        <c:if test="${!committee.active}"><c:set var="activeDisplay" value="Inactive"/></c:if>
                        <c:set var="partnerProposals" value="${ committeeToPartnerProposalsMap[committee] }"/>
                        <td><a href="/tac/committees/${ committee.id} " id="committee_link_${ committee.id }">${ committee.name }</a></td>
                        <td>${ committee.semester.displayName }</td>
                        <td><a href="/tac/committees/${ committee.id}/proposals/partner/${ user.partner.abbreviation }" id="committee_link_${ committee.id }">${ fn:length(partnerProposals) }</a></td>
                        <td><a href="/tac/committees/${ committee.id}/proposals" id="committee_link_${ committee.id }">${ fn:length(committee.proposals) }</a></td>
                        <td><a href="/tac/committees/${ committee.id}/queues" id="committee_link_${ committee.id }">${ fn:length(committee.queues) }</a></td>
                        <td><a href="/tac/committees/${ committee.id}/members" id="committee_link_${ committee.id }">${ fn:length(committee.members) }</a></td>
                        <td>${ activeDisplay }</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <script type="text/javascript">
                $(document).ready(function() {
                    page.helpContent = 'This page lists the committees to which you belong.  Drill down for more details by clicking on one of the linked committees.' +
                        'If you belong to no committees, or do not see the committee you expect, please contact the ITAC secretary.';
                    $('#committees').dataTable({
                        "bPaginate": false,
                        "bFilter": true,
                        "bSort": true,
                        "bInfo": false,
                        "aaSorting": [[ 6, "asc" ], [1, "asc"]],
                        "sDom": '<"top"f>t',
                        "aoColumns": [
                          { "sType": "html" },
                          { "sType": "html" },
                          { "sType": "num-html" },
                          { "sType": "num-html" },
                          { "sType": "num-html" },
                          { "sType": "num-html" },
                          { "sType": "html" }
                        ]
                    });
                });
            </script>
<%@ include file="../fragments/sidebar.jspf" %>
<%@ include file="../fragments/footer.jspf" %>
