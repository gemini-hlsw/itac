<%@ include file="../../fragments/header.jspf" %>

<div class="span-19 colborder">
<%-- security attributes --%>
<c:set var="isAdmin" value="false"/>
<c:set var="canSeeDetails" value="false"/>
<c:set var="canSeeComments" value="false"/>
<c:set var="isPrimaryPartner" value="false"/>
<%-- Admin --%>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
</security:authorize>
<security:authorize access="hasRole('ROLE_COMMITTEE_MEMBER') or hasRole('ROLE_ADMIN')">
    <c:set var="canSeeComments" value="true"/>
</security:authorize>
<%-- Involved in proposal can see details, can edit their own fields, and can edit ITAC comment--%>
<c:if test="${ proposal.joint}">
<c:forEach items="${proposal.phaseIProposal.submissions}" var="submission">
    <c:if test="${submission.partner.partnerCountryKey eq user.partner.partnerCountryKey}">
        <c:set var="canSeeDetails" value="true"/>
    </c:if>
</c:forEach>
</c:if>
<%-- Primary partner can edit additional fields --%>
<c:if test="${user.partner.partnerCountryKey eq proposal.partner.partnerCountryKey}">
    <c:set var="canSeeDetails" value="true"/>
    <c:set var="isPrimaryPartner" value="true"/>
</c:if>

<%-- Override editing functions if this is a static export --%>
<c:if test="${ !empty cssString }">
    <c:set var="isAdmin" value="false"/>
    <c:set var="isPrimaryPartner" value="false"/>
</c:if>

<!-- Issues -->
<c:if test="${!empty issues}">
    <h4>Issues:
        <c:if test="${proposal.checksBypassed}">
            ${fn:length(proposal.issues)} issues hidden.
        </c:if>
        <c:if test="${ isAdmin }">
        <form style="display:inline;">
            <span class="right" id="toggle-checks-bypassed">
                <input type="radio" id="toggle-checks-bypassed-bypass" name="toggle-checks-bypassed" <c:if test="${ !proposal.checksBypassed }">checked="checked"</c:if>/><label class="small" for="toggle-checks-bypassed-bypass">bypass checks</label>
                <input type="radio" id="toggle-checks-bypassed-restore" name="toggle-checks-bypassed" <c:if test="${ proposal.checksBypassed }">checked="checked"</c:if> /><label class="small" for="toggle-checks-bypassed-restore">restore checks</label>
            </span>
        </form>
        </c:if>
    </h4>
    <div id="issues-list" <c:if test="${proposal.checksBypassed}">style="display:none;"</c:if>>
    <ol class="prepend-1 last">
        <c:forEach items="${ issues }" var="issue" varStatus="status">
            <c:choose>
                <c:when test="${issue.error}">
                    <c:set var="listClass" value="ui-state-error ui-corner-all"/>
                    <c:set var="iconClass" value="left ui-icon ui-icon-alert"/>
                </c:when>
                <c:when test="${issue.warning}">
                    <c:set var="listClass" value="ui-state-highlight ui-corner-all"/>
                    <c:set var="iconClass" value="left ui-icon ui-icon-info"/>
                </c:when>
                <c:otherwise>
                    <c:set var="listClass" value=""/>
                    <c:set var="iconClass" value=""/>
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${ issue.category == 'TimeAllocation' }">
                    <c:set var="issueLink" value="#submissionDetails"/>
                </c:when>
                <c:when test="${ issue.category == 'ObservingConstraint' }">
                    <c:set var="issueLink" value="#observationDetails"/>
                </c:when>
                <c:otherwise>
                    <c:set var="issueLink" value=""/>
                </c:otherwise>
            </c:choose>

            <li class="${ listClass }">
                <span class="${ iconClass }"></span>
                <c:if test="${ !empty issueLink }"><a href="${issueLink}"></c:if>
                ${issue.message}
                <c:if test="${ !empty issueLink }"></a></c:if>
            </li>
            </br>
        </c:forEach>
    </ol>
    </div>
</c:if>

<!-- prolog -->
<a name="prolog"></a> <!-- phase1Document -->
<a name="phase1Document"></a> <!-- summary -->
<!-- proposalContents -->
<a name="proposalContents"></a>
<hr/>

<h2 style="clear: both">Proposal Contents</h2>
<ul id="proposal-header-list" class="proposal-header-list">
<li class="large" style="padding: 0 0.2em 0 0.0em"><a href="#investigators">Investigators<span><img src="/static/images/fromOCS2PIT/user-icon.png" alt="investigators"/></a></li>
<li class="large"><a href="#submissionDetails">Times&Ranks<span><img src="/static/images/fromOCS2PIT/waiting.gif" alt="timesranks"/></a></li>
<li class="large"><a href="#observationDetails">Observations<span><img src="/static/images/fromOCS2PIT/clock.png" alt="observations"/></a></li>
<li class="large"><a href="#resources">Resources<span><img src="/static/images/fromOCS2PIT/device.png" alt="resources"/></a></li>
<li class="large"><a href="#observingConditions">Conditions<span><img src="/static/images/fromOCS2PIT/conds.png" alt="conditions"/></a></li>
<li class="large"><a href="#allocationCommitteeComments">Comments<span><img src="/static/images/fromOCS2PIT/segment_edit.gif" alt="comments"/></a></li>
<li class="large"><a href="#pdfAttachment">PDF<span><img src="/static/images/fromOCS2PIT/attach.png" alt="pdfAttachment"/></a></li>
</ul>

<a name="summary"></a>
<table width="100%" cellspacing="10" border="0">
    <tr>
        <td align="left"><img alt="[GEMINI]" src="/static/images/geminiLogo.gif"/></td>

        <td align="center"><span class="c1"><b>GEMINI OBSERVATORY</b></span><br/>
            <i><span class="c2">observing time request (HTML summary)</span></i></td>
    </tr>
</table>
<table>
    <tr>
        <td colspan="2"><b>Semester:</b><br/> ${proposal.committee.semester.displayName}</td>

        <td colspan="2"><b>Observing Mode:</b><br/> ${proposal.phaseIProposal.observingMode}</td>

        <c:if test="proposal.phaseIProposal.primary.partnerLead != null">
            <td><b>Partner Lead Scientist: ${proposal.phaseIProposal.primary.partnerLead}</b><br/>
                <c:set var="partnerLeadEmail" value="${ proposal.phaseIProposal.primary.partnerLead.email }"/>
                <a href="mailto:${partnerLeadEmail}"><span class="ui-icon ui-icon-mail-closed" style="float: right"></span>${partnerLeadEmail}</a>
            </td>
        </c:if>
    </tr>

    <tr>
        <td colspan="2"><b>Instruments:</b><br/>
            <str:join items="${ instruments }" separator=", "/>
        </td>

        <td colspan="2"><b>Gemini Reference:</b><br/>
            ${geminiReference}</td>

        <td><b>Partner:</b><br/>
            ${proposal.partner.name}<span class="flagMe"><span class="hide">${proposal.partner.abbreviation}</span></span></td>
    </tr>

    <tr>
        <td colspan="2"><b>Time Awarded:</b><br/>
            <c:choose>
                <c:when test="${!empty proposal.itac.accept}">
                    ${proposal.itac.accept.award.prettyString}
                </c:when>
                <c:otherwise>
                    Proposal not yet accepted into a queue.
                </c:otherwise>
            </c:choose>
        </td>

        <td colspan="2"><b>Thesis:</b><br/>
            ${proposal.phaseIProposal.investigators.pi.status}</td>

        <td><b>Partner Reference:</b><br/>
            <span id="partnerReference">${proposal.phaseIProposal.primary.receipt.receiptId}</span></td>
    </tr>

    <tr>
        <td colspan="2" class="band3Instructions"><b>Band 3
        <c:choose>
        <c:when test="${ proposal.band3 }">
        <span id="band3Acceptable">Acceptable:</span></b><br/>
        </c:when>
        <c:otherwise>
        <span id="band3Acceptable">not Acceptable:</span></b><br/>
        </c:otherwise>
        </c:choose>
            <span id="band3toggleable">
                <c:if test="${(isAdmin or isPrimaryPartner) and !(proposal.phaseIProposal.exchange or proposal.phaseIProposal.classical)}">
                    <div class="small">
                        <input type="checkbox" id="band3eligible">
                        <label for="band3eligible">switch between only Band 1/2 and only Band 3</label>
                    </div>
                </c:if>
            </span>
        </td>

        <td colspan="2"><b>Poor Weather Flag:</b>
            <span id="poorWeatherText">
            <c:if test="${poorWeather}">Yes</c:if>
            <c:if test="${!poorWeather}">No</c:if>
            <c:if test="${(isAdmin or isPrimaryPartner)}">
            <a href="#allocationCommitteeComments" class="small right">change</a>
            </c:if>
            </span>
        </td>

        <td colspan="2"><b>Rollover eligible:</b>
            <span id="rolloverEligible">
            <c:choose>
            <c:when test="${empty proposal.itac.accept}">Not yet accepted by ITAC</c:when>
            <c:when test="${proposal.itac.accept.rollover}">Yes</c:when>
            <c:when test="${!proposal.itac.accept.rollover}">No</c:when>
            </c:choose>
            </span>
        </td>
    </tr>
</table>
<!-- investigators -->
<hr/>

<table cellspacing="5" width="100%" height="167" border="0">
    <tr>
        <td class="label"><b>Title:</b></td>
        <td><b>${proposal.phaseIProposal.title}</b></td>
    </tr>
    <tr>
        <td class="label">
            <b>Principal Investigator:</b>

        </td>
        <td>
            <b>${pi}</b>
            <span id="investigators" class="right"><img src="/static/images/fromOCS2PIT/user-icon.png" alt="investigator"/></span>
        </td>
    </tr>
    <tr>
        <td class="label"><b>PI institution:</b></td>
        <td>${pinst}</td>
    </tr>
    <tr>
        <td class="label"><b>PI status:</b></td>
        <td>${piStatus}</td>
    </tr>
    <tr>
        <td class="label"><b>PI phone / fax / e-mail:</b></td>
        <td><a href="tel:${piPhone}">${piPhone}</a> / ${piFax} / <a href="mailto:${piEmail}"><span class="ui-icon ui-icon-mail-closed" style="float: right"></span>${piEmail}</a></td>
    </tr>
    <tr>
        <td class="label"><b>Co-investigators: </b></td>
        <td>
            <c:forEach items="${proposal.phaseIProposal.investigators.coi}" var="coi">
                ${coi.firstName} ${coi.lastName}: ${coi.institution},
                <a href="mailto:${coi.email}">${coi.email}
                    <span id="investigators" class="right"><img src="/static/images/fromOCS2PIT/user-icon.png" alt="investigator"/></span>
                    <span class="ui-icon ui-icon-mail-closed" style="float: right"></span>
                </a><br/>
            </c:forEach>
        </td>
    </tr>
</table>
<c:if test="${ proposal.joint }">
<table>
    <caption>Components</caption>
    <thead>
        <tr>
            <th></th>
            <th>Partner</th>
            <th>PI</th>
            <th>Proposal</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach items="${ proposal.proposals }" var="component">
    <tr>
        <td>
            <c:if test="${ proposal.primaryProposal eq component }"><span class="ui-icon ui-icon-star" style="float: right"></span></c:if>
            <c:if test="${ proposal.primaryProposal ne component }"><span class="ui-icon ui-icon-minus" style="float: right"></span></c:if>
        </td>
        <td><span class="flagMe">${ component.partner.abbreviation }</span></td>
        <td><a href="mailto:${ component.phaseIProposal.investigators.pi.email }">${ component.phaseIProposal.investigators.pi.email }<span class="ui-icon ui-icon-mail-closed" style="float: right"></span></a></td>
        <td><a href="/tac/committees/${proposal.committee.id}/proposals/${component.id}">${ component.phaseIProposal.primary.receipt.receiptId }</a></td>
    </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<c:if test="${ proposal.jointComponent }">
<h3>
    Member of <a href="/tac/committees/${proposal.committee.id}/proposals/${proposal.jointProposal.id}">joint
    <span class="ui-icon ui-icon-circle-triangle-e conditionLink" style="float: right"></span>
    </a>
</h3>
</c:if>


<!-- submission details -->
<h3 id="submissionDetails">Submission details<span id="investigators" class="right"><img src="/static/images/fromOCS2PIT/waiting.gif" alt="times-ranks"/></span></h3>
<hr/>
<table>
    <caption>NTAC (<i>recommended</i>)</caption>
    <thead>
    <tr>
        <th>Partner</th>
        <th><span class="ui-icon ui-icon-clock" style="float: left"></span></th>
        <th>Minimum <span class="ui-icon ui-icon-clock" style="float: left"></span></th>
        <th>Rank</th>
        <th>Email</th>
    </tr>
    </thead>
    <tbody>
    <tr class="hostRow ntacComment">
        <%-- Set editability of the host partner NTAC comment --%>
        <c:set var="tacFieldClass" value="tacField"/>
        <c:if test="${isAdmin or isPrimaryPartner}">
            <c:set var="tacFieldClass" value="tacEditField"/>
        </c:if>
        <!-- Single or master proposal -->
        <c:set var="primarySubmission" value="${proposal.phaseIProposal.primary}" />
        <td name="partnerName" data-id="${primarySubmission.partner.partnerCountryKey}" class="flagMe">
            ${primarySubmission.partner.abbreviation}
         </td>

        <td>
            <span class="${tacFieldClass} numeric" name="partnerTime" id="host_partnerTime"><fmt:formatNumber value="${ primarySubmission.accept.recommend.valueInHours }" minFractionDigits="1" maxFractionDigits="2" type="number"/>
            </span> hours
        </td>

        <td>
            <span class="${tacFieldClass} numeric" name="partnerMinTime" id="host_partnerMinTime">
                <fmt:formatNumber value="${ primarySubmission.accept.minRecommend.valueInHours }" minFractionDigits="1" maxFractionDigits="2" type="number"/>
            </span> hours
        </td>

        <td>
            <span class="${tacFieldClass} numeric" name="partnerRanking" id="host_rank">
                <fmt:formatNumber value="${ primarySubmission.accept.ranking }" maxFractionDigits="2" type="number"/>
            </span>
        </td>
        <td>
            <span class="${tacFieldClass} email" name="partnerContact" id="host_contact">&nbsp;${primarySubmission.accept.email}&nbsp;</span>
        </td>
    </tr>
    <%-- only display recommendations is user is admin or from partner country (ITAC-196) --%>
    <c:forEach items="${proposal.phaseIProposal.submissions}" var="submission">
        <c:if test="${submission != primarySubmission}">
            <c:choose>
            <c:when test="${submission.partner.partnerCountryKey eq user.partner.partnerCountryKey}">
                <c:set var="myCountry" value="true"/>
            </c:when>
            <c:otherwise>
                <c:set var="myCountry" value="false"/>
            </c:otherwise>
            </c:choose>
            <c:choose>
            <c:when test="${ myCountry or isAdmin }">
                <c:set var="tacFieldClass" value="tacEditField"/>
            </c:when>
            <c:otherwise>
                <c:set var="tacFieldClass" value=""/>
            </c:otherwise>
            </c:choose>

            <tr class="ntacComment">
                <td name="partnerName" data-id="${submission.partner.partnerCountryKey}"  class="flagMe">${submission.partner.abbreviation}</td>
                <c:choose>
                <c:when test="${!empty submission.accept}">
                <td>
                     <span class="${ tacFieldClass } numeric" name="partnerTime">
                        <fmt:formatNumber value="${ submission.accept.recommend.valueInHours }" minFractionDigits="1" maxFractionDigits="2" type="number"/>
                     </span> hours
                </td>
                <td>
                     <span class="${ tacFieldClass } numeric" name="partnerMinTime">
                        <fmt:formatNumber value="${ submission.accept.minRecommend.valueInHours }" minFractionDigits="1" maxFractionDigits="2" type="number"/>
                     </span> hours
                </td>
                <td>
                    <span class="${ tacFieldClass }  numeric" name="partnerRanking">
                        <fmt:formatNumber value="${ submission.accept.ranking }" maxFractionDigits="2" type="number"/>
                    </span>
                </td>
                <td>
                    <span class="${ tacFieldClass } email" name="partnerContact">&nbsp;${submission.accept.email}&nbsp;</span>
                </td>
                </c:when>
                <c:otherwise>
                <td colspan="4">Partner has not accepted</td>
                </c:otherwise>
                </c:choose>
            </tr>
        </c:if>
    </c:forEach>
        <tr>
            <td>Total</td>
            <td>${proposal.totalRecommendedTime.prettyString}</td>
            <td>${proposal.totalMinRecommendTime.prettyString}</td>
            <td></td>
            <td></td>
        </tr>
    </tbody>
</table>

<table>
    <caption>Partner Submission Details</b> (<i>requested</i>)</caption>
    <thead>
        <tr>
            <th>Partner</th>
            <th><span class="ui-icon ui-icon-clock" style="float: left"></span></th>
            <th>Minimum <span class="ui-icon ui-icon-clock" style="float: left"></span></th>
            <th>Reference number</th>
        </tr>
    </thead>
    <tr class="c4 hostRow">
        <!-- Single or master proposal -->
        <c:set var="primarySubmission" value="${proposal.phaseIProposal.primary}" />
        <td class="flagMe">${primarySubmission.partner.abbreviation}</td>
        <td>${primarySubmission.request.time.prettyString}</td>
        <td>${primarySubmission.request.minTime.prettyString}</td>
        <td>${primarySubmission.receipt.receiptId}</td>
    </tr>
    <!-- Component proposals -->
    <c:forEach items="${proposal.phaseIProposal.submissions}" var="submission">
        <c:if test="${submission != primarySubmission}">
        <tr>
            <td class="flagMe">${submission.partner.abbreviation}</td>
            <td>${submission.request.time.prettyString}</td>
            <td>${submission.request.minTime.prettyString}</td>
            <td>${submission.receipt.receiptId}</td>
        </tr>
        </c:if>
    </c:forEach>
    <tr>
        <td>Total</td>
        <td>${proposal.totalRequestedTime.prettyString}</td>
        <td>${proposal.totalMinRequestedTime.prettyString}</td>
        <td></td>
    </tr>
</table>
<c:if test="${canSeeDetails or isAdmin}">
    <div id="band_3_submission_request">
        <c:choose>
        <c:when test="${band_3_request}">
        <table>
            <caption>Band 3</caption>
            <thead>
                <tr>
                    <th>Requested<span class="ui-icon ui-icon-clock" style="float: left"></span></th>
                    <th>Minimum usable requested <span class="ui-icon ui-icon-clock" style="float: left"></span></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>${band_3_requested_time}</td>
                    <td>${band_3_minimum_usable_time}</td>
                </tr>
            </tbody>
         </table>
        </c:when>
        <c:otherwise>
        No band 3 request made.
        </c:otherwise>
        </c:choose>
        <div style="clear:both">&nbsp;</div>
    </div>
</c:if>


<h3 id="abstract">Abstract</h3>
<hr/>
<p>${abstract}</p>

<h3 id="observationDetails">Observation Details<span class="right"><img src="/static/images/fromOCS2PIT/clock.png" alt="observations"/></span></h3>
<%@ include file="../../fragments/_observations.jspf" %>

<h3 id="resources">Resources<span class="right"><img src="/static/images/fromOCS2PIT/device.png" alt="resources"/></span></h3>
<div>
    <ul>
        <c:forEach items="${proposal.phaseIProposal.blueprints}" var="blueprint">
        <li><div id="blueprint_${blueprint.blueprintId}"><b>${blueprint.instrument.displayName} (${blueprint.instrument.site.displayName})</b></div>
            <ul>
                <li>${blueprint.display}
                    <ul>
                        <c:forEach items="${blueprint.resourcesByCategory}" var="resourceType">
                            <c:if test="${ !empty resourceType.value }">
                                    <li>${resourceType.value}</li>
                            </c:if>
                        </c:forEach>
                    </ul>
                </li>
            </ul>
        </li>
        </c:forEach>
    </ul>
</div>

<h3 id="observingConditions">Observing Conditions <span class="right"><img src="/static/images/fromOCS2PIT/conds.png" alt="conditions"/></span></h3>
<c:choose>
<c:when test="${!empty proposal.conditions}">
<table width="100%" border="1">
    <thead>
        <tr class="c5">
            <th>Name</th>
            <c:forEach items="${proposal.conditions[0].conditionOrdering}" var="conditionType">
            <th>${conditionType.qualityDisplayName}</th>
            </c:forEach>
        </tr>
    </thead>

    <%-- READ-ONLY version --%>
    <c:if test="${isAdmin eq false and isPrimaryPartner eq false}">
        <c:forEach items="${proposal.conditions}" var="condition">
            <tr class="observationConstraint" name="constraint_${condition.id}">
                <td><div id="condition_${condition.id}" style="text-decoration: none">${condition.name}</div></td>
                <c:forEach items="${condition.conditionOrdering}" var="conditionType">
                <td>${conditionType.displayName}</td>
                </c:forEach>
            </tr>
        </c:forEach>
    </c:if>
    <c:if test="${isAdmin or isPrimaryPartner}">
        <c:forEach items="${proposal.conditions}" var="condition">
            <%-- Do not change this name from "constraint_*" without changing JS, which strips out ID to know which constraint to edit --%>
            <tr class="observationConstraint" id="constraint_${condition.id}">
                <td><div id="condition_${condition.id}" class="condition-display-${condition.id}">${condition.activeDisplay}</div></td>
                <c:forEach items="${condition.conditionOrdering}" var="conditionType">
                <td>
                    <select id="obs${conditionType.class.simpleName}_${condition.id}">
                        <c:forEach items="${conditionType.values}" var="value">
                            <option value="${value.key}" class="${conditionType.class.simpleName}">${value.displayName}</option>
                        </c:forEach>
                    </select>
                </td>
                </c:forEach>
            </tr>
        </c:forEach>
    </c:if>
</table>
</c:when>
<c:otherwise>
No conditions.
</c:otherwise>
</c:choose>

<!-- ITAC-196 -->
<h3 id="allocationCommitteeComments">Allocation Committees<span class="right"><img src="/static/images/fromOCS2PIT/segment_edit.gif" alt="comments"/></span></h3>
<table>
    <caption>NTAC comments</caption>
    <thead>
        <tr>
            <th>Committee</th>
            <th>Comment</th>
            <th>PW</th>
        </tr>
    </thead>
    <tbody>
    <!-- Partner Comments -->
    <c:if test="${isAdmin or isPrimaryPartner}">
        <c:set var="primaryProposalPartner" value="${proposal.phaseIProposal.primary.partner}"/>
        <tr class="ntacComment">
            <td>
                <span name="partnerName" data-id="${primaryProposalPartner.partnerCountryKey}"  class="flagMe">
                    <span class="ui-icon ui-icon-star left"></span>
                    ${primaryProposalPartner.abbreviation}
                </span>
            </td>

            <td>
                <span class="tacCommentEditField" name="ntacComment" id="ntacComment"><c:if test="${ empty proposal.phaseIProposal.primary.comment}">Add comment</c:if>${proposal.phaseIProposal.primary.comment}</span>
            </td>

            <td>
                <input type="checkbox" class="tacPW" id="tacPW_${proposal.phaseIProposal.primary.partner.partnerCountryKey}"/>
                <span id="tacPWText_${proposal.phaseIProposal.primary.partner.partnerCountryKey}">${proposal.phaseIProposal.primary.accept.poorWeather}</span>
            </td>
        </tr>
    </c:if>

    <c:forEach items="${proposal.phaseIProposal.submissions}" var="submission">
        <c:if test="${submission.partner != proposal.phaseIProposal.primary.partner}">
            <c:set var="myCountry" value="false"/>
            <c:set var="partnerKey" value="${submission.partner.partnerCountryKey}"/>
            <c:if test="${partnerKey eq user.partner.partnerCountryKey}">
                <c:set var="myCountry" value="true"/>
            </c:if>
            <c:if test="${ myCountry or isAdmin }">
            <tr class="ntacComment">
                <td><span name="partnerName" data-id="${submission.partner.partnerCountryKey}" class="flagMe">${submission.partner.abbreviation}</span></td>

                <td>
                   <span class="tacCommentEditField" name="partnerComment"><c:if test="${ empty submission.comment}">Add comment</c:if>${submission.comment}</span>
                </td>

                <td>
                <c:if test="${ not empty submission.accept}">
                    <input type="checkbox" class="tacPW" id="ntacPW_${partnerKey}"/>
                    <span id="ntacPWText_${partnerKey}">${proposal.phaseIProposal.primary.accept.poorWeather}</span>
                </c:if>
                </td>
            </tr>
            </c:if>
        </c:if>
    </c:forEach>
    </tbody>
</table>
<table>
    <caption>ITAC/Gemini comments</caption>
    <thead>
        <tr>
            <th>Committee</th>
            <th>Comment</th>
            <th>Contact</th>
        </tr>
    </thead>
    <tbody>
<c:if test="${isAdmin or canSeeDetails or canSeeComments}">
        <tr>
            <td>ITAC</td>
            <td><span class="itacEditField" name="itacComment">${proposal.itac.comment}</span></td>
            <td>&nbsp;</td>
        </tr>
</c:if>
<c:if test="${isAdmin or canSeeComments}">
        <tr>
            <td><a name="geminiComment"></a>Gemini</td>

            <td>
                <span class="itacEditField" name="geminiComment"><c:if test="${ empty proposal.itac.geminiComment}">Add comment</c:if>&nbsp;${proposal.itac.geminiComment}&nbsp;</span>
            </td>

            <td>
                <c:choose>
                <c:when test="${ not empty proposal.itac.accept }">
                    <span class="itacEditField email" name="contactScientistEmail">${proposal.itac.accept.contact}</span>
                </c:when>
                <c:otherwise>
                Proposal not accepted.
                </c:otherwise>
                </c:choose>
            </td>
        </tr>
</c:if>
    </tbody>
</table>
<!-- additionalInformation -->
<a name="additionalInformation"></a>
<hr/>

<h3 id="keywords">Keywords</h3>
<ul>
    <c:forEach items="${keywords}" var="keyword">
        <li>${keyword}</li>
    </c:forEach>
</ul>

<h3 id="pdfAttachment">PDF Attachment
    <a href="${proposalId}/pdf"><span style="display: inline" class="small quiet">[download]</span>
    <span class="right"><img src="/static/images/fromOCS2PIT/attach.png" alt="pdf"/></span></a>
</h3>

<p/>


<!-- coda -->
<a name="coda"></a>
</div>


<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
<script type="text/javascript">

    function sortObservationsByBand(){
        var spans = $('.has-observation-band-data');
        var sorted = spans.sort(function(a, b) {
            var isLess = $(a).data("band") + $(a).data("targetName") < $(b).data("band") + $(b).data("targetName");
            var isMore = $(a).data("band") + $(a).data("targetName") > $(b).data("band") + $(b).data("targetName");
            return isLess ? -1 : isMore ? 1 : 0;
        });
        $.each(sorted, function(idx, item) {
            $("#observation-table").append($(item));
            var sSelector = "#observation-conditions-" + $(item).data("id");
            $(item).after($(sSelector));
        });
    }
    
    $(document).ready(function() {
        $('.Too').each(function() {
            $(this).prepend('<img src="/static/images/fromOCS2PIT/too.png"</img>&nbsp;');
        });

        $('.Nonsidereal').each(function() {
            $(this).prepend('<img src="/static/images/fromOCS2PIT/nonsidereal.png"</img>&nbsp;');
        });

        $('.Sidereal').each(function() {
            $(this).prepend('<img src="/static/images/fromOCS2PIT/sidereal.png"/>&nbsp;');
        });

        sortObservationsByBand();

        <c:if test="${isAdmin or isPrimaryPartner}">
        $('span.qualityEditField, span.proposalPartnerTacEditField, span.band3EditField, span.observationEditField').editableText({
            newlinesEnabled: false
        });
        </c:if>

        <c:if test="${isAdmin or canSeeDetails or isPrimaryPartner}">
        $('span.itacEditField').editableText({
            newlinesEnabled: false
        });
        </c:if>

        <!-- highlight fields as theyre edited -->
        $('span.qualityEditField, span.itacEditField, span.proposalPartnerTacEditField,.tacEditField, span.band3EditField, span.tacCommentEditField').bind('onStartEditing', function() {
            $(this).css('background-color', '#ffc');
            $(this).css('border', '1px dotted');
            $(this).css('font-size', '16px');
            $(this).css('padding', '4px');
        });
        <!-- Clear highlights -->
        $('span.qualityEditField, span.itacEditField, span.proposalPartnerTacEditField,.tacEditField, span.band3EditField, span.tacCommentEditField').bind('onStopEditing', function() {
            $(this).css('background-color', '');
            $(this).css('border', '');
            $(this).css('font-size', '');
            $(this).css('padding', '');
        });

        <!-- tacEditField only written inside security check, so okay for this call to assume their editability -->
        $('span.tacEditField, span.tacCommentEditField').editableText({
            newlinesEnabled: false
        });

        //TODO: Collapse editing copy/pasta

        //Handles NTAC Edits
        $('.tacEditField').change(function() {
            var targetField = $(this).attr('name');
            var targetClass = "TacExtension";
            var naturalId = $(this).parents(".ntacComment").find("[name='partnerName']").attr('data-id');
            var newValue = newValue = trimWhitespace(stripHtml($(this).html()));
            var valid = validate(newValue, $(this));
            if (!valid)
                return false;
            else
                postProposalEdit(targetField, newValue, targetClass, naturalId);
        });

        //Handles NTAC Comment Edits
        $('.tacCommentEditField').change(function() {
            var targetField = $(this).attr('name');
            var targetClass = "TacExtension";
            var parentRow = $(this).closest(".ntacComment");
            var naturalId = parentRow.find("td span[name='partnerName']").attr('data-id');
            var newValue = newValue = trimWhitespace(stripHtml($(this).html()));
            var valid = validate(newValue, $(this));
            if (!valid)
                return false;
            else
                postProposalEdit(targetField, newValue, targetClass, naturalId);
        });

        //Handles ITAC Comment Edits
        $('.itacEditField').change(function() {
            var targetField = $(this).attr('name');
            var targetClass = "ITacExtension";
            var naturalId = "Default";
            var newValue = newValue = trimWhitespace(stripHtml($(this).html()));
            var valid = validate(newValue, $(this));
            if (!valid)
                return false;
            else
                postProposalEdit(targetField, newValue, targetClass, naturalId);
        });

        //Handles band3 Edits
        $('.band3EditField').change(function() {
            var targetField = $(this).attr('name');
            var targetClass = "GeminiBand3Extension";
            var naturalId = "Default";
            var newValue = newValue = trimWhitespace(stripHtml($(this).html()));
            var valid = validate(newValue, $(this));
            if (!valid)
                return false;
            else
                postProposalEdit(targetField, newValue, targetClass, naturalId);
        });

        //Handles observation time edits
        $('.observationEditField').change(function() {
            var targetField = $(this).attr('name');
            var targetClass = "Observation";
            var parentRow = $(this).closest("tr.observation");
            var rowId = parentRow.attr('id');
            naturalId = onlyAfterLast(rowId, '-');

            var newValue = trimWhitespace(stripHtml($(this).html()));
            var valid = validate(newValue, $(this));
            if (!valid)
                return false;
            else
                postProposalEdit(targetField, newValue, targetClass, naturalId);
        });

        //Reads value, strips it if necessary, and posts it
        function registerDropDownChangeListener(selector, fieldName, naturalId, targetClass, textUpdateClass) {
            $(selector).change(function() {
                if (textUpdateClass != null) { // Not well generalized.
                    var conditionRow = $(this).closest('tr.observationConstraint');
                    $(textUpdateClass).each(function(){
                        var activeSiteQualities = joinActiveSiteQualities(conditionRow);
                        $(this).text(activeSiteQualities);
                    });
                }
                var initVal = $(selector).val();
                postProposalEdit(fieldName, trimWhitespace(initVal), targetClass, naturalId);
            });
        };

        function refreshPoorWeatherFlag() {
            var pwCheckboxes = $(".tacPW"), isPw = false;
            pwCheckboxes.each(function() {
                var isChecked = $(this).attr('checked');
                if (isChecked) {
                    isPw = true;
                }
            });
            $("#poorWeatherText").html((isPw) ? 'Yes <a href="#allocationCommitteeComments" class="small right">change</a>' : 'No <a href="#allocationCommitteeComments" class="small right">change</a>');
        }

        //Binds a checkbox and text field (value) to a postProposalEdit() handler (e.g., Band3, PW flag, etc.).
        // @param selector: jquery to find the checkbox under examination.
        // @param initialValue: the value to initialize that checkbox to
        // @param textSelector: selector for accompanying text.
        // @param targetField - human readble designation of exactly what kind of thing is being edited.
        // @param targetClass - similar to natural id concept, rough grouping of what kind of things are being edited
        // @param naturalId - refers to an element id that has sense in the domain
        // @param changeSelector - selector for a span to update
        // @param changeContent - array containing text to update the changeSelector, first element if checked, second element if not
        // @param collectionSelector - selector for a collection of items to update
        // @param collectionContent - array containing text to update the collectionSelector, first element if checked, second element if not
        function setEditCheckbox(selector, initialValue, textSelector, targetField, targetClass, naturalId, changeSelector, changeContent, collectionSelector, collectionContent) {
            $(selector).attr('checked', initialValue);
            $(selector).change(function() {
                var newVal = $(this).attr('checked');
                if (textSelector != null) {
                    $(textSelector).html(newVal.toString());
                }
                postProposalEdit(targetField, newVal, targetClass, naturalId);
                if (changeSelector != null) {
                    var newText = (newVal) ? changeContent[0] : changeContent[1];
                    $(changeSelector).text(newText);
                }
                if (collectionSelector != null) {
                    var newCollectionText = (newVal) ? collectionContent[0] : collectionContent[1];
                    $(collectionSelector).each(function() { $(this).text(newCollectionText); });
                }
                refreshPoorWeatherFlag();
            });
        }

        //Band 3 eligible checkbox
        <c:choose>
        <c:when test="${ !proposal.phaseIProposal.mixedObservationBands}">
        $('#band3eligible').button();
        setEditCheckbox("#band3eligible", ${band3}, "#band3Text", "band3eligible", "GeminiBand3Extension", "Default", "#band3Acceptable", ["acceptable", "not acceptable"], ".observationBand", ["BAND_3", "BAND_1"]);
        </c:when>
        <c:otherwise>
        $('#band3toggleable').replaceWith("<div>Proposal contains both Band 1/2 and Band 3 observations.</div>");
        </c:otherwise>
        </c:choose>

        //Poor weather flag
        setEditCheckbox("#tacPW_${proposal.phaseIProposal.primary.partner.partnerCountryKey}", ${proposal.phaseIProposal.primary.accept.poorWeather }, "#tacPWText_${proposal.phaseIProposal.primary.partner.partnerCountryKey}", "poorWeather", "TacExtension", "${proposal.phaseIProposal.primary.partner.partnerCountryKey}", null, null, null, null);

        <c:forEach var="submission" items="${proposal.phaseIProposal.submissions}">
        <c:if test="${not empty submission.accept}">
        setEditCheckbox("#ntacPW_${submission.partner.partnerCountryKey}", ${submission.accept.poorWeather}, "#ntacPWText_${submission.partner.partnerCountryKey}", "poorWeather", "TacExtension", "${submission.partner.partnerCountryKey}", null, null, null, null);
        </c:if>
        </c:forEach>

        function postProposalEdit(targetField, newValue, targetClass, naturalId) {
            postProposalEditWithProposalId(targetField, newValue, targetClass, naturalId, '${proposalId}');
        }

        function joinActiveSiteQualities(conditionRow) {
            var activeSiteQualities = [];
            var i = 0;
            var element = $(conditionRow);
            var options = element.find('option:selected');
            options.each(function() {
                var optionClass=$(this).attr('class');
                var capitals = onlyCapitals(optionClass);
                var component = capitals + $(this).val();
                activeSiteQualities[i++] = onlyBefore(component, '/');
            });

            return activeSiteQualities.join();
        }

        $('#toggle-checks-bypassed').buttonset();
        $('#toggle-checks-bypassed-bypass').click(function() {
            $('#issues-list').hide();
            postProposalEdit('checksBypassed', 'true', 'Proposal', '');

        });
        $('#toggle-checks-bypassed-restore').click(function() {
            $('#issues-list').show();
            postProposalEdit('checksBypassed', 'false', 'Proposal', '');
        });

        //Conditions edit list
        <c:forEach var="condition" items="${proposal.conditions}">
            <c:forEach items="${condition.conditionOrdering}" var="siteQuality">
        $("#obs${siteQuality.class.simpleName}_${condition.id}").val('${siteQuality.key}');
        registerDropDownChangeListener('#obs${siteQuality.class.simpleName}_${condition.id}', '${siteQuality.class.simpleName}', '${condition.id}', 'GeminiPart', '.condition-display-${condition.id}');
             </c:forEach>
        </c:forEach>

        //Observations edit lists (condition)
        <c:forEach var="observation" items="${proposal.allObservations}">
        registerDropDownChangeListener('#observation_condition_selecter_${observation.id}', 'condition', '${observation.id}', 'Observation', null);
        </c:forEach>

        //Observations edit lists (blueprint)
        <c:forEach var="observation" items="${proposal.allObservations}">
        registerDropDownChangeListener('#observation_blueprint_selecter_${observation.id}', 'blueprint', '${observation.id}', 'Observation', null);
        </c:forEach>

        //Observations edit lists (blueprint)
        <c:forEach var="observation" items="${proposal.allObservations}">
        registerDropDownChangeListener('#observation_band_selecter_${observation.id}', 'band', '${observation.id}', 'Observation', null);
        </c:forEach>


        <c:forEach var="observation" items="${proposal.allObservations}">
        setEditCheckbox("#observation-${observation.id}-toggle", ${observation.active}, null, "observationActive", "Observation", "${observation.id}", null, null, null, null);
        </c:forEach>

        $(".conditionLink").qtip({
            content: 'Jump to observing condition',
            show: 'mouseover',
            hide: 'mouseout',
            position: {
                corner: {
                    target: 'bottomLeft',
                    tooltip: 'topRight'
                }
            }
        });

        $(".blueprintLink").qtip({
            content: 'Jump to blueprint description',
            show: 'mouseover',
            hide: 'mouseout',
            position: {
                corner: {
                    target: 'bottomLeft',
                    tooltip: 'topRight'
                }
            }
        });

        $(".band3Instructions").qtip({
            content: 'If a proposal contains both band 1/2 and Band 3 observations it is considered finalised; a proposal that does not contain both flavors of observations can be toggled between an only-Band-1/2 and an only-Band-3 proposal.',
            show: 'mouseover',
            hide: 'mouseout',
            position: {
                corner: {
                    target: 'bottomRight',
                    tooltip: 'topRight'
                }
            }
        });

        $('.position').qtip({ style: { name: 'cream', tip: true } })
        $('.brightness').qtip({ style: { name: 'cream', tip: true } })
    });
</script>
