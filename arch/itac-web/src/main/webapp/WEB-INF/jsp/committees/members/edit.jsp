<%@ include file="../../fragments/header.jspf" %>
	<style type="text/css">
	#sortable1 li, #sortable2 li { margin: 0 5px 5px 5px; padding: 5px; font-size: 1.2em; width: 120px; }
	</style>
			
			<div class="span-18 colborder ">
				<h2>Edit committee membership</h2>
				
				<div id="committee-member-tabs">
					<ul>
						<li><a href="#tabs-1">Members</a></li>
						<li><a href="#tabs-2">Everybody</a></li>
					</ul>
					<div id="tabs-1">
						<ul id="sortable1" class="connectedSortable ui-helper-reset">
							<li class="ui-state-default">Sandy</li>
							<li class="ui-state-default">Rosemary</li>
							<li class="ui-state-default">Bryan</li>
							<li class="ui-state-default">Andy</li>
						</ul>
					</div>
					<div id="tabs-2">
						<ul id="sortable2" class="connectedSortable ui-helper-reset">
							<li class="ui-state-highlight">Devin</li>
							<li class="ui-state-highlight">Vasu</li>
							<li class="ui-state-highlight">Shane</li>
						</ul>
					</div>
				</div>
				
			</div>
			<script type="text/javascript">
				$(function() {
					$("#sortable1, #sortable2").sortable().disableSelection();
			
					var $tabs = $("#committee-member-tabs").tabs();
			
					var $tab_items = $("ul:first li",$tabs).droppable({
						accept: ".connectedSortable li",
						hoverClass: "ui-state-hover",
						drop: function(ev, ui) {
							var $item = $(this);
							var $list = $($item.find('a').attr('href')).find('.connectedSortable');
			
							ui.draggable.hide('slow', function() {
								$tabs.tabs('select', $tab_items.index($item));
								$(this).appendTo($list).show('slow');
							});
						}
					});
				});
			</script>		
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
