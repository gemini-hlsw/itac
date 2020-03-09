<%@ include file="../../fragments/header.jspf" %>

<%-- security attributes --%>
<c:set var="isAdmin" value="false"/>
<%-- Admin --%>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
    <c:set var="jointProposalAdmin" value="true"/>
</security:authorize>

<div class="span-18 colborder ">
    <div id="tabs">
        <ul>
            <li><a href="#tabs-1">Joint Proposals (${fn:length(proposals)})</a></li>
            <c:if test="${isAdmin}">
                <li><a href="#tabs-2">Force Join <span class="small quiet"><i>(create new joint)</i></span></a></li>
                <li><a href="#tabs-3">Force Join <span class="small quiet"><i>(to existing)</i></span></a></li>
            </c:if>
        </ul>
        <div id="tabs-1">
            <ul id="jp_list">
                <c:forEach items="${ proposals }" var="proposalKV">
                    <c:set var="jointProposal" value="${proposalKV.key}"/>
                    <c:set var="primaryProposal" value="${proposalKV.value[0]}"/>
                    <c:set var="components" value="${proposalKV.value[1]}"/>
                    <c:set var="north_or_south" value="${sortKeys[proposalKV.key]['Location']}"/>
                    <c:set var="proposal_site_pi"
                           value="${north_or_south}_${jointProposal.phaseIProposal.investigators.pi.lastName}"/>
                    <li id="li_${proposal_site_pi}" name="li-${proposal_site_pi}">
                        <h5>
                            <a href="../proposals/${jointProposal.id}">${jointProposal.phaseIProposal.investigators.pi.lastName}: ${ jointProposal.phaseIProposal.title }
                                (@ ${north_or_south}) [
                                <c:forEach items="${ jointProposal.submissionsPartnerEntries }" var="entry">
                                <c:if test="${ !empty entry.value.receipt.receiptId }">
                                    ${ entry.value.receipt.receiptId }
                                </c:if>
                                </c:forEach>
                                ]</h5></a>
                        <ol>
                                <%-- First, the master proposal --%>
                            <li>
                                <a href="/tac/committees/${ primaryProposal.committee.id }/proposals/${ primaryProposal.id }">${primaryProposal.phaseIProposal.investigators.pi.lastName}&#39;s ${primaryProposal.partner.abbreviation}
                                    proposal</a>
                                <span style="float: right"><span class="ui-icon ui-icon-star" style="float: right"></span>Primary Proposal</span>
                                <c:if test="${isAdmin}">
                                <c:if test="${ jointProposalAdmin }">
                                <span style="float: right">
                                    <a href="#li-${proposal_site_pi}" id="dissolve_${jointProposal.id}"
                                       onclick="dissolveJoint(${jointProposal.committee.id}, ${jointProposal.id})">Dissolve
                                        joint</a>
                                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                </span>
                                </c:if>
                                </c:if>
                            </li>

                                <%-- Then, the component proposals --%>
                            <c:forEach items="${ components }" var="component">
                                <li>
                                    <a href="/tac/committees/${ component.committee.id }/proposals/${ component.id }">${component.phaseIProposal.investigators.pi.lastName}&#39;s ${component.partner.abbreviation}
                                        proposal </a>
                                    <c:if test="${isAdmin}">
                                    <c:if test="${ jointProposalAdmin }">
                                <span style="float: right">
                                    <a href="#li-${proposal_site_pi}"
                                       onclick="makePrimary(${jointProposal.committee.id}, ${jointProposal.id}, ${component.id})"><span class="ui-icon ui-icon-minus" style="float: right"></span>
                                       Make primary proposal</a>
                                </span>
                                <span style="float: right">
                                    <a href="#li-${proposal_site_pi}" id="remove_${component.id}"
                                       onclick="removeFromJoint(${jointProposal.committee.id}, ${jointProposal.id}, ${component.id})">Remove
                                        from join</a>
                                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                </span>
                                    </c:if>
                                    </c:if>
                                </li>
                            </c:forEach>
                        </ol>
                    </li>
                </c:forEach>
            </ul>
        </div>
        <c:if test="${isAdmin}">
            <div id="tabs-2">
                <div class="span-8">
                    <ol id="joint-prop-selectable" class="selectable">
                        <c:forEach items="${committeeProposals}" var="proposal" varStatus="status">
                        <c:if test="${ !proposal.fromExchangePartner and !proposal.joint}">
                            <li class="ui-widget-content ${ proposal.partner.abbreviation }" id="${proposal.id}"><span class="hide">${ proposal.id }</span>
                                <c:forEach items="${ proposal.submissionsPartnerEntries }" var="entry">
                                    <c:if test="${ !empty entry.value.receipt.receiptId }">
                                        ${ proposal.partner.abbreviation} - ${ entry.value.receipt.receiptId }
                                    </c:if>
                                </c:forEach>
                                ${ proposal.phaseIProposal.investigators.pi.lastName }
                            </li>
                        </c:if>
                        </c:forEach>
                    </ol>
                </div>
                <div id="joint-queue" style="float: right" class="span-8 last">
                    <form id="create" action="joint/create" method="post">
                        <button id="save_merge" style="display:none">Save Joint Proposal</button>
                        <input type="hidden" id="masterProposalId" name="masterProposalId"/>

                        <p/>
                        <input type="hidden" id="secondaryProposalIds" name="secondaryProposalIds"/>

                        <p/>
                    </form>
                    <h2>Primary Proposal</h2>
                    <ul id="primary-queue" class="droppable ui-widget-header">
                    </ul>
                    <h2>Secondary Proposals</h2>
                    <ul id="secondary-queue" class="selectable">
                    </ul>

                </div>
                <div style="clear: both">&nbsp;</div>
            </div>
            <div id="tabs-3">
                <div id="addToJoinButton"
                     style="position: absolute; top: 80px; left: 255; " >
                    Join
                </div>
                <div class="span-6">
                    <ol id="joint-prop-existing-selectable" class="selectable" style="list-style-type: none; margin: 0 0 0 0; padding-left: 0;">
                        <c:forEach items="${committeeProposals}" var="proposal" varStatus="status">
                        <c:if test="${ !proposal.exchange and !proposal.joint and !proposal.jointComponent}">
                        <li class="ui-widget-content ${ proposal.partner.abbreviation }" id="new-component-for-joint-${proposal.id}" title="${ proposal.phaseIProposal.title }">
                            <span><str:substring start="0" end="1">${ proposal.site.displayName }</str:substring> - ${ proposal.phaseIProposal.primary.partner.abbreviation } - ${ proposal.phaseIProposal.primary.receipt.receiptId }</span>
                            <span class="right">${ proposal.phaseIProposal.investigators.pi.lastName }</span>
                            <div class="clear"/>
                        </li>
                        </c:if>
                        </c:forEach>
                    </ol>
                </div>
                <div style="float: right" class="prepend-1 span-9 last">
                    <ul id="joint-queue-existing"  class="selectable" style="list-style-type: none; margin: 0 0 0 0; padding-left: 0;">
                    <c:forEach items="${ proposals }" var="proposalKV">
                        <c:set var="jointProposal" value="${proposalKV.key}"/>
                        <c:set var="primaryProposal" value="${proposalKV.value[0]}"/>
                        <c:set var="components" value="${proposalKV.value[1]}"/>
                        <c:if test="${ !primaryProposal.exchange }">
                        <li class="ui-widget-content ${ jointProposal.partner.abbreviation }" id="joint-proposal-${jointProposal.id}" title="${ jointProposal.phaseIProposal.title }">
                            <span class="ui-icon ui-icon-star" style="float: left"></span>
                            <span>
                                <str:substring start="0" end="1">${ jointProposal.site.displayName }</str:substring> - ${ jointProposal.partner.abbreviation} - ${ jointProposal.phaseIProposal.primary.receipt.receiptId }
                                <span class="loud right"><b>${ jointProposal.phaseIProposal.investigators.pi.lastName }</b></span>
                                <div class="components">
                                    <c:forEach items="${ components }" var="component">
                                        <c:if test="${ !empty component.phaseIProposal.primary.receipt.receiptId }">
                                            <span class="ui-icon ui-icon-minus" style="float: left"></span>${ component.phaseIProposal.primary.partner.abbreviation} - ${ component.phaseIProposal.primary.receipt.receiptId }<br/>
                                        </c:if>
                                    </c:forEach>
                                </div>
                            </span>
                        </c:if>
                        </li>
                    </c:forEach>
                    </ul>
                </div>
                <div style="clear: both">&nbsp;</div>
            </div>
        </c:if>
    </div>
</div>

<script type="text/javascript">


    /* Handle removing a component proposal from a joint */
    function removeFromJoint(committeeId, jointProposalId, componentId) {
        wait(true);
        $.ajax({
            type: "POST",
            url: "./joint/" + jointProposalId + "/remove/" + componentId,
            failure: function(msg) {
                alert(msg);
            },
            complete: function(jqHXR, textStatus) {
                wait(false);
                window.location.reload();
            }
        });
    }

    function dissolveJoint(committeeId, jointProposalId) {
        wait(true);
        $.ajax({
            type: "DELETE",
            url: "./joint/" + jointProposalId,
            failure: function(msg) {
                alert(msg);
            },
            complete: function(jqHXR, textStatus) {
                wait(false);
                window.location.reload();
            }
        })
    }

    function makePrimary(committeeId, jointProposalId, componentId) {
        wait(true);
        $.ajax({
            type: "POST",
            url: "./joint/" + jointProposalId + "/with_master/" + componentId,
            failure: function(msg) {
                alert(msg);
            },
            complete : function(jqHXR, textStatus) {
                wait(false);
                window.location.reload();
            }
        })
    }

    function addNewComponent(committeeId, jointProposalLi, newComponentLis) {
        wait(true);
        var newComponentIds = [];
        var newComponentProposalsSelected = $('#joint-prop-existing-selectable').children('li.ui-selected');
        newComponentProposalsSelected.each(function() {
             newComponentIds.push(onlyAfterLast($(this).attr('id'), '-'));
        });
        var jointProposalId =
            onlyAfterLast($(jointProposalLi).attr('id'), '-');

        $.ajax({
            type: "POST",
            url: "./joint/" + jointProposalId + "/new_components/",
            data: {"newComponentIds" : newComponentIds},
            dataType: 'json',
            success: function(msg) {
                var messageBox = $('#message_box');
                var messageBoxMessage = $('#message_box_message');
                messageBoxMessage.html(msg.message);
                messageBoxMessage.removeClass('error');
                messageBoxMessage.addClass('success');
                messageBox.fadeIn().delay(5000).fadeOut();

                var jointProposalComponentList = $(jointProposalLi).find('span div.components');
                var componentProposalsList = $(newComponentLis[0]).closest('ol');
                newComponentLis.each(function() {
                    componentProposalsList.remove($(this).attr('id'));
                    $(jointProposalComponentList[0]).append($(this));
                });

                wait(false);
            },
            error: function(msg) {
                var messageBox = $('#message_box');
                var messageBoxMessage = $('#message_box_message');
                var json = $.parseJSON(msg.response);
                messageBoxMessage.html(json.message);
                messageBoxMessage.removeClass('success');
                messageBoxMessage.addClass('error');
                messageBox.fadeIn().delay(5000).fadeOut();
                wait(false);
            }
        })
    }


    $(function() {
        page.helpContent = 'Allows secretary and admin to manipulate joint proposals.  There are two tabs: Joint ' +
            'proposals and Force Join.  <br/>' +
            'Joint proposals allows you to view all joint proposals, dissolve them back into ' +
            'their individual component proposals, remove individual component proposals from a join, or designate ' +
            'an alternative component proposal as the primary proposal for the joint.  The primary proposal controls ' +
            'the partner that is allowed to globally edit the proposal as well as which actual observations and ' +
            'conditions are being edited.  When editing a condition on the joint proposal, it also modifies that condition ' +
            'on the primary component proposal.<br/>' +
            'Force join allows you to create new joins.  First click a proposal to designate it the primary, then add ' +
            'additional component proposals by clicking them.  If you wish to change the primary, then click on the ' +
            'preferred component in the joint building list (shown to the right).  Finally, you must save the ' +
            'joint proposal or no changes will be made.';

        $("#wait").hide();

        $("#tabs").tabs();

        $("#secondary-queue").selectable({
            selected: function(evt, ui) {
                //Remove current primary
                var oldPrimary = $("#primary-queue li")[0];
                $(oldPrimary).remove();
                $(ui.selected).remove();

                $("#secondary-queue").append(oldPrimary);
                var el = $(ui.selected).clone(true);
                $("#primary-queue").append(el);
            }
        });

        $('#joint-prop-existing-selectable').selectable({
            selected: function(evt, ui) {
            }
        }).children('li').sort(function(a, b) {
            return $(a).text().toUpperCase().localeCompare($(b).text().toUpperCase());
        }).appendTo('#joint-prop-existing-selectable');

        $('#joint-queue-existing').bind("mousedown", function (e) {
           e.metaKey = false; // Disable multiple selection of joint proposals.
        }).selectable({
            selected: function(evt, ui) {
            }
        }).children('li').sort(function(a, b) {
            return $(a).text().toUpperCase().localeCompare($(b).text().toUpperCase());
        }).appendTo('#joint-queue-existing');

        // Keep add component to joint button in an accessible location as the window scrolls.
        $(window).scroll(function()
        {
            $('#addToJoinButton').animate({top:$(window).scrollTop()+125+"px" },{queue: false, duration: 150});
        });

        $('#addToJoinButton').button().click(function() {
            var jointProposalSelected = $('#joint-queue-existing').children('li.ui-selected')[0];
            var newComponentProposalsSelected = $('#joint-prop-existing-selectable').children('li.ui-selected');

            addNewComponent(${committeeId}, jointProposalSelected, newComponentProposalsSelected);
        });;


        $("#secondaryProposalIds").val("");
        $("#masterProposalId").val("");
        $("#joint-prop-selectable").selectable({
            /*
             * When a proposal is selected, add it to the list
             * of proposals that are to be merged. The very first
             * proposal defaults to be the "primary" proposal.
             *
             * Secondary proposals can be dragged into the primary position.
             *
             */
            selected: function(evt, ui) {
                var el = $(document.createElement('li'));
                var id = "drag_" + ui.selected.id;
                /* If exists as primary or secondary, do not re-add it */
                if ($("#secondary-queue").find("#" + id).length > 0
                        || $("#primary-queue").find("#" + id).length > 0
                        ) {
                    return;
                }
                $(el).attr("id", id);
                $(el).html(ui.selected.innerHTML);
                $(el).css("z-index", 20);
                $(el).css("top", "auto");
                $(el).css("list-style-type", "none");

                /* If the primary is empty, set it to selected */
                if ($("#primary-queue li").length == 0) {
                    $("#primary-queue").append(el);
                    $("#masterProposalId").val(ui.selected.id);
                } else {
                    $("#save_merge").show();
                    $("#secondary-queue").append(el);
                    if ($("#secondaryProposalIds").val().length == 0) {
                        $("#secondaryProposalIds").val(ui.selected.id);
                    } else {
                        var oldVal = $("#secondaryProposalIds").val();
                        $("#secondaryProposalIds").val(oldVal + "," + ui.selected.id);
                    }
                }
            }
        });

        //Sort Joint Proposals
        //   Collect 'em'
        var els = $("#jp_list>li");
        var els2 = els.sort(function(a, b) {
            return a.id < b.id ? -1 : (a.id > b.id) ? 1 : 0;
        });
        els.detach();
        els2.appendTo($("#jp_list"));

    });
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
