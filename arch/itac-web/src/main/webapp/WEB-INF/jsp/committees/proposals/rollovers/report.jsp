<%@ include file="../../../fragments/header.jspf" %>

<%-- security attributes --%>
<c:set var="isAdmin" value="false"/>
<%-- Admin --%>
<security:authorize access="hasRole('ROLE_SECRETARY') or hasRole('ROLE_ADMIN')">
    <c:set var="isAdmin" value="true"/>
</security:authorize>

<div class="span-18 colborder prepend-1">
    <form name="rolloverset" id="rolloverset" method="POST" action="./RolloverSets">
        <table id="obs_list">
            <caption class="loud">Rollovers for Gemini ${siteName} - ${report.id}</caption>
            <thead>
                <tr>
                    <c:if test="${isAdmin}">
                    <th>Select</th>
                    </c:if>
                    <th>ID</th>
                    <th>RA</th>
                    <th>Dec</th>
                    <th>IQ</th>
                    <th>SB</th>
                    <th>WV</th>
                    <th>CC</th>
                    <th>Time</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="rolloverObservation" items="${report.observations}">
                <tr id="obs_${rolloverObservation.observationId}" class="observation">
                    <c:if test="${isAdmin}">
                    <td>
                        <input type="checkbox" name="observation" value="${rolloverObservation.observationId}"
                               id="includeObservation_${rolloverObservation.observationId}"
                               checked="true" class="rolloverObservationSelect"/>
                    </td>
                    </c:if>
                    <td>${rolloverObservation.observationId}</td>
                    <td>${rolloverObservation.target.coordinates.ra}</td>
                    <td>${rolloverObservation.target.coordinates.dec}</td>
                    <td>${rolloverObservation.siteQuality.imageQuality.percentage}</td>
                    <td>${rolloverObservation.siteQuality.skyBackground.percentage}</td>
                    <td>${rolloverObservation.siteQuality.waterVapor.percentage}</td>
                    <td>${rolloverObservation.siteQuality.cloudCover.percentage}</td>

                    <c:if test="${isAdmin}">
                    <td>
                        <input type="text" name="time" size="4"
                               class="required number"
                               value="${rolloverObservation.observationTime.value}"/>
                    </td>
                    </c:if>
                    <c:if test="${not isAdmin}">
                        <td>${rolloverObservation.observationTime.value}</td>
                    </c:if>
            </c:forEach>
            </tbody>
        </table>

        <c:if test="${isAdmin}">
            Save selected set as: <input type="text" class="required" name="setName">

            <p>
                <button id="submit" type="submit" value="Save">Save</button>
            </p>
        </c:if>
    </form>

    <%@ include file="../../../fragments/sidebar.jspf" %>
    <%@ include file="../../../fragments/footer.jspf" %>

    <script type="text/javascript">
        $('#obs_list').dataTable({
            "bPaginate": false,
            "bFilter": true,
            "bSort": true,
            "bInfo": false,
            "aaSorting": [[ 1, "asc" ]],
            "sDom": '<"top"f>t',
            "aoColumnDefs": [
              { "bSortable": true, "sType": "html", "aTargets": [ 1, 2, 3 ] },
              { "bSortable": true, "sType": "numeric", "aTargets": [ 4, 5, 6, 7 ] },
              { "bSortable": false, "aTargets": [ 0, 8 ] }
            ]
        });



        function pad(number, length) {
            var str = '' + number;
            while (str.length < length)
                str = '0' + str;
            return str;
        }


        function parse_program_id(stringId){
            try{
                var strings = stringId.split("-"),
                    i = 0,
                    retval = "";
                for(i = 0; i < strings.length; i += 1){
                    var string = strings[i];
                    var asNum = parseInt(string);
                    if(! isNaN(asNum)){
                        retval += pad(asNum, 6);
                    }else{
                        retval += string;
                    }
                }
                return retval;
            }catch(x){
                return stringId;
            }

        }

        function program_id_compare(a, b) {
            var a_id = parse_program_id($(a).attr('id'));
            var b_id = parse_program_id($(b).attr('id'));
            return (a_id > b_id ? 1 : (a_id < b_id) ? -1 : 0);
        }


        $(function() {
            $("#rolloverset").validate();
            $("#submit").button();

            $('.rolloverObservationSelect').click(function(){
                var checkboxValue = $(this).attr('checked');
                var selectedRow = $(this).closest('tr');
                selectedRow.find('td input.required.number').each(function() {
                    $(this).attr('disabled', (checkboxValue) ? false : true);
                });
            });

            var obs = $(".observation");
            obs.sort(program_id_compare);
            obs.each(function () {
                $(this).detach();
                $("#obs_list").append($(this));
            });
        });

    </script>