<%@ include file="../../fragments/header.jspf" %>
<%-- Admin --%>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
</security:authorize>

<div class="span-19 colborder ">
   <c:set var="proposalCount" value="0"/>
   <c:forEach items="${proposalIssuesByCategory}" var="kv">
      <c:set var="proposalCount" value="${proposalCount + fn:length(kv.value)}"/>
   </c:forEach>
   <h2 style="display: inline" class="span-6">Proposals With Issues (<span id="proposalsShowing">${proposalCount}</span>)
   </h2>

   <hr/>
   <div id="categories" style="display:none">
      <c:forEach items="${ProposalIssueCategoryValues}" var="category">
         <c:forEach items="${proposalIssuesByCategory}" var="kv">
            <c:if test="${kv.key eq category}">
               <c:set var="categoryProposals" value="${kv.value}"/>
               <div id="category-div-${category}" class="category-div">
               <h3 class='category-count'>${category} (<span
                                                         class="visible-count">${fn:length(categoryProposals)}</span>
                  showing <span class="some-hidden" style="display:none">; <a href="#" class="reveal">reveal <span
                                                                                                               class="hidden-count">0</span>
                     hidden</a></span>)</h3>

               <div id="accordion-${category}" class="accordion">
                  <c:forEach items="${categoryProposals}" var="proposal" varStatus="status">
                     <%@ include file="../../fragments/_accordion_proposal.jspf" %>
                  </c:forEach>
               </div>
            </c:if>
         </c:forEach>
         </div>
      </c:forEach>
   </div>
</div>

<script type="text/javascript">

   function putAccordionAtTop(accordionCategoryEl, proposalId) {
      var headerEl = $(accordionCategoryEl).find("#accordion_header_" + proposalId);
      var bodyEl = $(accordionCategoryEl).find("#accordion_body_" + proposalId);
      if (headerEl != null && bodyEl != null) {
         headerEl.detach();
         bodyEl.detach();

         $(accordionCategoryEl).append(headerEl);
         $(accordionCategoryEl).append(bodyEl);

      }
   }

   function sort_category(accordionCategoryEl) {
      try {
         var proposalsBySortKey = new Array();
         var keySort = new Array();
         //Get all proposal ids
         var buttons = $(accordionCategoryEl).find(".hide-button");
         $(buttons).each(function(ix, button) {
            var proposalId = button.attributes['name'].value;
            var headerEl = $(accordionCategoryEl).find("#accordion_header_" + proposalId);

            var sortKey = headerEl.find(".partner-abbreviation").text() + headerEl.find(".pi-last-name").text();
            proposalsBySortKey[sortKey] = proposalId;
            keySort.push(sortKey);
         });
         //Now go through them in order, pulling them and appending them (resulting in a sorted list)
         keySort = keySort.sort();
         for (var i = 0; i < keySort.length; i++) {
            var key = keySort[i];
            var proposalId = proposalsBySortKey[key];
            putAccordionAtTop(accordionCategoryEl, proposalId);
         }
      } catch(x) {
         alert(x);
      }
   }

   function proposal_visible(categoryEl, proposalId, makeVisible) {
      var headerEl = $(categoryEl).find("#accordion_header_" + proposalId + "");
      var bodyEl = $(categoryEl).find("#accordion_body_" + proposalId);
      if(headerEl == null || bodyEl == null || headerEl.length == 0 || bodyEl.length == 0){
         //Elements not found, which is perfectly legitimate
         return;
      }
      if (makeVisible) {
         headerEl.show();
         bodyEl.show();
         headerEl.accordion( "option", "active", false );
      } else {
         headerEl.hide();
         bodyEl.hide();
      }

      //Modify display
      var countEl = headerEl.parents('.category-div').children('.category-count');
      var visibleCountEl = countEl.children('.visible-count');

      var oldVisibleCount = parseInt(visibleCountEl.text());

      var newVisibleCount = oldVisibleCount + (makeVisible ? 1 : -1);

      visibleCountEl.text(newVisibleCount);
   }


   $(function() {
      page.helpContent = 'Lorem ipsum';

      $("#tabs").tabs();
   });

   $(document).ready(function() {
      $(".accordion").accordion({
         event: "click",
         clearStyle: true,
         collapsible: true,
         active: false
      });

      $('#categories').fadeIn(3000);

      $(".accordion").each(function(ix, value) {
         sort_category(value);
      });

      $(".bypass-checks").button();
      $(".bypass-checks").click(function () {
         var key = "edu.gemini.tac.proposals.hidden." + $(this).parents(".accordion").attr('id');
         var categoryEl = $(this).parents(".accordion");

         var proposalId = $(this).attr('name');

         postProposalEditWithProposalId('checksBypassed', 'true', 'Proposal', '', proposalId);
         proposal_visible(categoryEl, proposalId, false);
      });
   });
</script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
