<%@ include file="../../../fragments/header.jspf" %>


<div class="span-18 colborder ">
    <h4>Rollover Set ${set.name} contains ${fn:length(set.observations)} observations.</h4>

        <table id="obs_list">
            <caption>Rollover Set ${set.name} contains ${fn:length(set.observations)} observations.</caption>
            <thead>
                <tr>
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
            <c:forEach var="rolloverObservation" items="${set.observations}">
                <tr id="obs_${rolloverObservation.observationId}" class="observation">
                    <td>${rolloverObservation.observationId}</td>
                    <td>${rolloverObservation.target.coordinates.ra}</td>
                    <td>${rolloverObservation.target.coordinates.dec}</td>
                    <td>${rolloverObservation.siteQuality.imageQuality.percentage}</td>
                    <td>${rolloverObservation.siteQuality.skyBackground.percentage}</td>
                    <td>${rolloverObservation.siteQuality.waterVapor.percentage}</td>
                    <td>${rolloverObservation.siteQuality.cloudCover.percentage}</td>
                    <td>${rolloverObservation.observationTime.prettyString}</td>
            </c:forEach>
            </tbody>
        </table>



    <%@ include file="../../../fragments/sidebar.jspf" %>
    <%@ include file="../../../fragments/footer.jspf" %>

    <script type="text/javascript">
        $('#obs_list').dataTable({
            "bPaginate": false,
            "bFilter": true,
            "bSort": true,
            "bInfo": false,
            "aaSorting": [[ 0, "asc" ]],
            "sDom": '<"top"f>t',
            "aoColumnDefs": [
              { "bSortable": true, "sType": "html", "aTargets": [ 0, 1, 2 ] },
              { "bSortable": true, "sType": "numeric", "aTargets": [ 3, 4, 5, 6 ] },
              { "bSortable": false, "aTargets": [ 7 ] }
            ]
        });
    </script>