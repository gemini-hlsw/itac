<%@ include file="../../fragments/header.jspf" %>

<div class="span-18 colborder ">
    <div class="span-18">
        <h2 style="display: inline">Reassign queue programs<span style="float: right">Queue - ${queue.name} (${queue.detailsString})</span></h2></br>
    </div>

    </hr>
    <div style="clear: both">&nbsp;</div>

    <p/>
    Manually reassign programs to alternate bands (drag &amp; drop).  If you are manually adding a currently un-queued
    proposal to a specific band, click on the add proposal tab, begin typing either the PI&apos;s surname or the proposal id,
    click in the drop down on one of the identified proposals to select it.  After the selection, drag the proposal into
    the desired band.
    <p/>

    <div id="queue-band-tabs">
        <ul>
            <li class="droppable"><a href="#tabs-1">Band 1</a></li>
            <li class="droppable"><a href="#tabs-2">Band 2</a></li>
            <li class="droppable"><a href="#tabs-3">Band 3</a></li>
            <li class="droppable"><a href="#tabs-4">PW</a></li>
            <li class="droppable"><a href="#tabs-5">Classical</a></li>
            <li class="droppable"><a href="#tabs-7">Exchange</a></li>
            <li class="droppable"><a href="#tabs-6">Un-Queued</a></li>
        </ul>
        <div id="tabs-1" i class="band-1">
            <c:set var="band" value="1"/>
            <%@ include file="./fragments/band-add.jspf" %>
        </div>
        <div id="tabs-2" class="band-2">
            <c:set var="band" value="2"/>
            <%@ include file="./fragments/band-add.jspf" %>
        </div>
        <div id="tabs-3" class="band-3">
            <c:set var="band" value="3"/>
            <%@ include file="./fragments/band-add.jspf" %>
        </div>
        <div id="tabs-4" class="band-4">
            <c:set var="band" value="4"/>
            <%@ include file="./fragments/band-add.jspf" %>
        </div>
           <div id="tabs-5" class="band-5 classical manual">
            <%@ include file="./fragments/classical-add.jspf" %>
        </div>
        <div id="tabs-7" class="band-6 exchange manual">
            <%@ include file="./fragments/exchange-add.jspf"%>
        </div>
        <div id="tabs-6" class="add remove manual">
            <%@ include file="./fragments/force-add.jspf" %>
        </div>
    </div>
</div>
<script type="text/javascript">
    $(function() {
        page.helpContent = 'Manually reassign programs to alternate bands (drag &amp; drop).';

        var resetDraggables = function() {
            $("li.draggable").draggable({ revert: "invalid" }).disableSelection();
            $("li.draggable").each(function() {
                var centerHeight = $(this).height() / 2;
                var centerWidth = $(this).width() / 2;
                $(this).draggable("option", "cursorAt", { top: centerHeight, left: centerWidth  });
            });
        }();


        var tab_items = $("#queue-band-tabs ul li.droppable").droppable({
                    hoverClass: "ui-state-hover",
                    drop: function(event, ui) {
                        var tab = $(this);
                        var tabContentDiv = $(tab.find('a').attr('href'));
                        var list = tabContentDiv.find('table.band-list');
                        var draggedItem = ui.draggable;

                        var tabClass = tabContentDiv.attr('class');
                        var bandRegex = /.*band-(\d+).*/;
                        var removeRegex = /.*remove.*/;
                        var re_results = bandRegex.exec(tabClass);
                        var remove_re_results = removeRegex.exec(tabClass);
                        var removeOp = (remove_re_results != null);
                        var rebandOp = (re_results != null);
                        var forceAddOp = (re_results == null && remove_re_results == null);

                        if (forceAddOp) {
                            location.replace("./bandings/" + band + "/" + proposalId);
                        } else {
                            var id = draggedItem.attr('banding-id');  // draggedItem.find('.banding-id').html();
                            var bandingSource = (id != null);
                            var classicalOrExchange = (id == null);
                            var targetUrl = null;

                            if (removeOp) {
                                if (bandingSource) {
                                    location.replace('/tac/committees/${ committee.id }/queues/${ queue.id }/manual/banding/' + id + '/remove');
                                } else {
                                    var proposalId = draggedItem.attr('id');
                                    location.replace('/tac/committees/${ committee.id }/queues/${ queue.id }/manual/proposal/' + proposalId + '/remove');
                                }
                            } else if (rebandOp) {
                                var band = re_results[1];
                                if (bandingSource) {
                                    location.replace('/tac/committees/${ committee.id }/queues/${ queue.id }/manual/banding/' + id + '/band/' + band);
                                } else {
                                    //It's either a Classical or Exchange (non-banded) proposal
                                    var proposalId = draggedItem.attr('id');
                                    location.replace('/tac/committees/${ committee.id}/queues/${ queue.id }/manual/band/' + band + '/proposals/' + proposalId);
                                }
                            }
                        }
                    }


                });

        var tabs = $("#queue-band-tabs").tabs(); // Note: tabifying must come after the center calculations have been made or else all tabs other than the first will be display: none resulting in 0,0's

    tabs.tabs('select', ${ targetTab });

    });



</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
