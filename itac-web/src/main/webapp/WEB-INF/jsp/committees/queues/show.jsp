<%@ include file="../../fragments/header.jspf" %>

            <div class="span-18 colborder ">
                <div class="span-8">
                    <h2 class="inline">${queue.name}</h2></br>
                    <h3 class="inline">Queue Proposals - ${queue.detailsString}</h3>
                    <h3 style="display: inline" class="span-6">Queue Proposals - <span id="proposalsShowing">${fn:length(queue.bandings)}</span></h3>
                    <h3 style="display: inline" class="span-8">Created - <fmt:formatDate value="${ queue.createdTimestamp }" type="both" dateStyle="short" timeStyle="long" /></span>
                </div>

                <div class="span-9">
                    <p style="float: right;" id="filter-header" class="inline span-4 last filter-button"><a href="#">Excluded: None</a></p>
                    <p style="float: right;" id="sort-header" class="inline span-4 colborder sort-button"><a href="#">Sort: Partner/Partner ID</a></p>
                </div>

                </hr>

                <c:set var="show_banding" value="true"/>
                <%@ include file="../../fragments/_proposal_sort.jspf" %>
                <%@ include file="../../fragments/_proposal_filter.jspf" %>
                <hr/>

                <div id="all-accordion">

                    <%-- show all banded proposals (suppress components) --%>
                    <c:forEach items="${ queue.bandings }" var="banding">
                        <c:choose>
                            <c:when test="${ !banding.jointComponent }">
                                <c:set var="proposal" value="${ banding.proposal }"/>
                                <%@ include file="../../fragments/_accordion_proposal.jspf" %>
                            </c:when>
                        </c:choose>
                    </c:forEach>

                    <%-- show all classical proposals (suppress components) --%>
                    <c:forEach items="${ queue.copyOfClassicalProposalsSorted }" var="proposal">
                        <c:choose>
                            <c:when test="${ !proposal.jointComponent }">
                                <%@ include file="../../fragments/_accordion_proposal.jspf" %>
                            </c:when>
                        </c:choose>
                    </c:forEach>

                    <%-- show all exchange proposals (suppress components) --%>
                    <c:forEach items="${ queue.exchangeProposals }" var="proposal">
                        <c:choose>
                            <c:when test="${ !proposal.jointComponent }">
                                <%@ include file="../../fragments/_accordion_proposal.jspf" %>
                            </c:when>
                        </c:choose>
                    </c:forEach>
                </div>
            </div>
            <script type="text/javascript">
                $(function() {
                    $("#all-accordion").accordion({
                            collapsible: true,
                            active: false
                    });
                });
            </script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript" src="/static/javascript/itac/proposal_filter.js"></script>
<script type="text/javascript" src="/static/javascript/itac/proposal_sort.js"></script>

<%@ include file="../../fragments/_proposal_sort_generators.jspf"%>
<%@ include file="../../fragments/_band_sort_generators.jspf"%>
