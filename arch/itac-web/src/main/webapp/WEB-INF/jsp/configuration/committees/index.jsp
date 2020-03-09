<%@ include file="../../fragments/header.jspf" %>


			<div class="span-24 colborder " id='nonTableForm'>
                <table id="committees">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Semester</th>
                            <th>Active</th>
                        </tr>
                    </thead>

                    <tbody>
					<c:forEach items="${committees}" var="committee">
                        <tr id="committee-${committee.id}" class="committee">
                            <form action="/tac/configuration/committees/${ committee.id }/edit" class="editCommittee" method="POST">
                            <td><a href="/tac/committees/${ committee.id}">${committee.name}</a></td>
                            <td>${committee.semester.displayName}</td>
                            <td><input name="active" type="checkbox" <c:if test="${ committee.active }">checked</c:if>/></td>
                            </form>
                        </tr>
					</c:forEach>
					</tbody>
				</table>
				<hr/>
				<a href="#" id="create-new-committee">Create new committee</a>
				<div id="new-committee">
	    			<span class="small">Creating a committee adds all existing people to that committee.</span>
				    <form id="create-committee" action="/tac/configuration/committees/" method="POST" class="nonTableForm">
						<label for="committee-name">Committee name<span class="small">Please name you committee</span></label>
                        <input type="text" id="committee-name" name="committeeName" value="" class="required"></input>
                        <div class="clear">&nbsp;</div>

                        <label for="semester-name">Semester name<span class="small">A or B?</span></label>
                        <input type="text" id="semester-name" name="semesterName" value="" class="required"></input>
                        <div class="clear">&nbsp;</div>

                        <label for="semester-year">Semester year<span class="small">What year does it cover?</span></label>
                        <input type="text" id="semester-year" name="semesterYear" value="" class="required number"></input>
                        <div class="clear">&nbsp;</div>

                        <a href="#" id="create-button">Save new committee</a>
				    </form>
				</div>
			</div>

            <script type="text/javascript">

                $(function() {
                    page.helpContent = 'See all committees, activate, deactivate and create new committees.';

                    // Parses out semester years and names from committee name if it happens to be in the right format.
                    $('#committee-name').change(function() {
                        var name = ($(this).val());
                        var re = /(\d+)-?([AB])/
                        var components = re.exec(name);
                        if (components != null) {
                            $('#semester-name').val(components[2]);
                            $('#semester-year').val(components[1]);
                        }
                    });

                    // Submits form on checkbox click.
                    $('.committee td input').change(function() {
                        ($(this).closest('tr').find('form')).submit();
                    });

                    $('#create-new-committee').toggle(function() {
                        var targetDiv = $(this).next('div#new-committee');
                        targetDiv.slideDown();
                    }, function() {
                        var targetDiv = $(this).next('div#new-committee');
                        targetDiv.slideUp();
                    }).button();
                    $("div#new-committee").hide();

                    $('a#create-button').button();
                    $('a#create-button').click(function() {
                        $(this).parent('#create-committee').submit();
                    });
                    $('#create-committee').validate();

                    $('#committees').dataTable({
                        "bPaginate": false,
                        "bFilter": true,
                        "bInfo": false,
                        "aaSorting": [[1, "desc"]]
                    });

                });
            </script>

<%@ include file="../../fragments/footer.jspf" %>




