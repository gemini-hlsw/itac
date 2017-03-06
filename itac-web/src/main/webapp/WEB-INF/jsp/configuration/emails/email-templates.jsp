<%@ include file="../../fragments/header.jspf" %>

			
			<div class="span-18 colborder ">

				<div id="tabs">
					<ul>
    				    <c:forEach items="${templates}" var="template" varStatus="status">
	    					<li><a href="#tabs-${status.count}" class="template-tab">${template.description}</a></li>
	    				</c:forEach>
					</ul>

					<c:forEach items="${templates}" var="template" varStatus="status">
					    <div id="tabs-${status.count}" >
    						<form method="post" action="email-templates" enctype="multipart/form-data">
                                <div class="controls" style="float: right">
                                    <input type="button" name="edit" value="edit" class="edit"/>
                                    <input type="submit" name="submit" value="save" class="save"/>
                                    <input type="submit" name="submit" value="cancel" class="cancel"/>
                                    <!-- hidden input field to let server side know, which template is affected -->
    	    						<input type="text" name="index" value="${status.count}" class="index"/>
                                </div>
                                <div class="editfields">
	    						    <textarea class="low" name="subject" disabled>${template.subject}</textarea>
	    						    <textarea name="template" disabled>${template.template}</textarea>
	    						</div>
                            </form>
					    </div>
					</c:forEach>

				</div>
			</div>

			<script type="text/javascript">
			    <!-- keep track of editing mode -->
			    var editing = false;

				$(function() {
					$("#tabs").tabs();
					$("div.controls input").button();
					$("div.controls input.save").hide();
					$("div.controls input.cancel").hide();
					$("div.controls input.index").hide();

					$("#tabs").tabs({
                        select: function(event, ui) {
                            if (editing == true) {
                                <!-- do not select another tab if there might be pending changes -->
                                alert("Please save or discard any pending changes first.");
                                return false;
                            }
                        }
					});

					$("div.controls input.edit").click(function() {
					    editing = true;
						$(this).siblings("input.save").show();
						$(this).siblings("input.cancel").show();
						$(this).hide();
						$(this).closest(".ui-tabs-panel").find("textarea").attr("disabled", "");
					});

                    <!-- help -->
                    page.helpContent =
                        'Edit subject and content of the different email templates. ' +
                        'The following variables are available int the template texts:</br>' +
                        '<ul>' +
                        '<li><b>@COUNTRY@</b> Partner Country</li>' +
                        '<li><b>@GEMINI_COMMENT@</b> Comment added by Gemini</li>' +
                        '<li><b>@GEMINI_CONTACT_EMAIL@</b> Gemini Contact Scientist email (for successful proposals)</li>' +
                        '<li><b>@GEMINI_ID@</b> Gemini Science Program ID (for successful proposals)</li>' +
                        '<li><b>@ITAC_COMMENTS@</b> Comments added by ITAC</li>' +
                        '<li><b>@JOINT_INFO@</b> Listing of all parts of a Joint Proposal</li>' +
                        '<li><b>@JOINT_TIME_CONTRIBS@</b> Listing of partner/time for all parts of a Joint Proposal</li>' +
                        '<li><b>@NTAC_COMMENT@</b> Partner TAC Comment</li>' +
                        '<li><b>@NTAC_RANKING@</b> Partner Ranking</li>' +
                        '<li><b>@NTAC_RECOMMENDED@</b> </li>' +
                        '<li><b>@NTAC_REF_NUMBER@</b> Partner Proposal Reference Number</li>' +
                        '<li><b>@NTAC_SUPPORT_EMAIL@</b> Partner Support Email</li>' +
                        '<li><b>@PI_MAIL@</b> Mail address of PI</li>' +
                        '<li><b>@PI_NAME@</b> Name of PI</li>' +
                        '<li><b>@PROG_ID@</b> Program ID</li>' +
                        '<li><b>@PROG_TITLE@</b> Proposal Title</li>' +
                        '<li><b>@PROG_KEY@</b> Science Program password (for successful proposals)</li>' +
                        '<li><b>@QUEUE_BAND@</b> Science Band Number (1, 2, 3, or 4 for successful proposals)</li>' +
                        '<li><b>@TIME_AWARDED@</b> (for successful proposals)</li>' +
                        '</ul>'

					
				});
			</script>
					
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
