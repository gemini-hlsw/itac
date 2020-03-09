<%@ include file="../../fragments/header.jspf" %>
<%-- security attributes --%>
<c:set var="isAdmin" value="false"/>
<%-- Admin --%>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
</security:authorize>

<div class="span-19 colborder ">
    <h2 id='instrument_unavailability'>Site Shutdowns</h2>
    <ul>
        <c:forEach var="kv" items="${shutdownsBySite}">
            <c:set var="site" value="${kv.key}"/>
            <li class="site siteQualityRadio proposals" name="${site}">${site}
                <!-- Show existing unavailable dates -->
                <c:set var="shutdowns" value="${kv.value}"/>
                <c:forEach var="shutdown" items="${shutdowns}">
                    <span id="${shutdown.id}" class="shutdown">
                        <p>
                            <c:if test="${isAdmin}">
                                <a href="#" class="delete_shutdown">
                                    <img
                                            src="/static/images/button_cancel.png" class="shutdown-icon"></a> </c:if>&nbsp;Unavailable
                            from <span
                                class="startDate">${shutdown.dateRangePersister.dateRange.startDate}</span>
                            through <span
                                class="endDate">${shutdown.dateRangePersister.dateRange.endDate}</span>

                        </p>
                    </span>
                </c:forEach>

                <!-- Show '+' icon for new area -->
                <p>
                    <c:if test="${isAdmin}">
                        <a href="#new_${site}_shutdown" name="new_${site}_shutdown"
                           id="new_${site}_shutdown" class="new_shutdown">
                            <img src="/static/images/edit_add.png" class="shutdown-icon">
                        </a>
                    </c:if>
                </p>
            </li>
        </c:forEach>
        <!-- Hidden span for date picking -->
        <span id="shutdown_dates" style="display:none">
            <h2>Site Unavailability Period</h2>

            <p>First day unavailable</p>
            <input id="startDatePicker" type="text"/>
            <p>Final day unavailable</p>
            <input id="endDatePicker" type="text"/>
            <p>
                <button type="submit" name="submit" value="submit" id="add_shutdown">Add</button>
            </p>
        </span>
        <!-- Hidden template for shutdown appearance -->
        <span id="shutdown_template" class="shutdown" style="display:none">
            <p><a href="#" id="delete_shutdown" class="delete_shutdown"><img
                    src="/static/images/button_cancel.png" align="absmiddle" class="shutdown-icon"></a>&nbsp;Unavailable
                from <span
                        class="startDate">replaceMe</span> through <span class="endDate">replaceMe</span></p>
        </span>
    </ul>
</div>
<script type="text/javascript">

    $(function() {
        //Create the datepickers
        $('#startDatePicker').datepicker();
        $('#endDatePicker').datepicker();

        //Handle the "+" button: show the date-picking span
        $(".new_shutdown").click(function () {
            var id = $(this).attr("id");

            //Show and move the date picking span
            var b = $("#shutdown_dates");
            $("#shutdown_dates").show();
            b.detach();
            $(this).parent().append(b);

            //Hide all plus buttons
            $(".new_shutdown").hide();
        });


        //Grab the data from the data-picking span, hide the span, post the data, and handle the callback
        $("#add_shutdown").click(function () {
            var firstDate = $("#startDatePicker").datepicker('getDate');
            var secondDate = $("#endDatePicker").datepicker('getDate');
            if (firstDate.length == 0 || secondDate.length == 0) {
                return;
            }
            var startDate = $.datepicker.formatDate('yy-mm-dd', firstDate);
            var endDate = $.datepicker.formatDate('yy-mm-dd', secondDate);
            //Find the owner
            var owningEl = $(this).closest("li");
            //Hide specificier span
            $("#shutdown_dates").hide();
            //Create a new shutdown
            $('body').css('cursor', 'wait');
            $.post("shutdowns",
                    { 'siteName' : owningEl.attr('name'), 'startDate' : startDate, 'endDate' : endDate},
                    //Callback
                    function (responseData) {
                        try {
                            //Clone the template, set it's values with JSON data, and place it
                            var jsObj = $.parseJSON(responseData);
                            var shutdownId = jsObj['shutdownId'];
                            var shutdownClone = $("#shutdown_template").clone();
                            shutdownClone.attr('id', shutdownId);
                            shutdownClone.find(".startDate").text(jsObj['startDate']);
                            shutdownClone.find(".endDate").text(jsObj['endDate']);
                            owningEl.append(shutdownClone);
                            shutdownClone.show();
                        } catch(x) {
                            alert(x);
                        }
                        //Move 'new' button to bottom
                        var pb = owningEl.find(".new_shutdown");
                        pb.detach();
                        owningEl.append(pb);
                        //Bring the 'new' buttons back
                        $(".new_shutdown").show();
                        $('body').css('cursor', 'auto');
                    });
        });

        //Set up the delete handler for all delete buttons, even those created dynamically (live() method)
        $(".delete_shutdown").live('click', function() {
            var id = $(this).closest("span").attr('id');
            $.ajax({
                type: "DELETE",
                url: "shutdowns/shutdown/" + id,
                success : function (deleteResponse) {
                    $("#" + id).remove();
                },
                error : function (failedDelete) {
                    alert("Could not delete shutdown. Manually delete shutdown id-> '" + id + "' ");
                }
            });
        });
    });
</script>
<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
