<%@ include file="../../fragments/header.jspf" %>


            <div class="span-18 colborder ">
                <form action="#">
                    <table>
                        <thead>
                            <tr>
                                <td>Name</td>
                                <td>Site</td>
                                <td>Semester</td>
                                <td>RA Bin size</td>
                                <td>Dec bin size</td>
                                <td>Smoothing length</td>
                                <td>Actions</td>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><input  type="text" size="12" disabled value="Restriction name"/></td>
                                <td><select name="site"><option value="">Choose</option><option value="north" selected>North</option><option value="south">South</option></select></td>
                                <td><select name="semester"><option value="">Choose</option><option value="a" selected>A</option><option value="b">B</option></select></td>
                                <td><input class="bin"  type="text" size="7" disabled value="10	:00"/></td>
                                <td><input class="bin" type="text" size="7" disabled value="00:30"/></td>
                                <td><input  type="text" size="3" disabled value="5"/></td>
                                <td>Frozen</td>
                            </tr>
                            <tr>
                                <td><input  type="text" size="12" disabled value="Restriction name"/></td>
                                <td><select name="site"><option value="">Choose</option><option value="north" selected>North</option><option value="south"/>South</option></select></td>
                                <td><select name="semester"><option value="">Choose</option><option value="a">A</option><option value="b" selected>B</option></select></td>
                                <td><input class="bin"  type="text" size="7" disabled value="10	:00"/></td>
                                <td><input class="bin" type="text" size="7" disabled value="00:30"/></td>
                                <td><input  type="text" size="3" disabled value="5"/></td>
                                <td>Frozen</td>
                            </tr>
                            <tr>
                                <td><input  type="text" size="12" disabled value="Restriction name"/></td>
                                <td><select name="site"><option value="">Choose</option><option value="north">North</option><option value="south" selected>South</option></select></td>
                                <td><select name="semester"><option value="">Choose</option><option value="a" selected>A</option><option value="b">B</option></select></td>
                                <td><input class="bin"  type="text" size="7" disabled value="10	:00"/></td>
                                <td><input class="bin" type="text" size="7" disabled value="00:30"/></td>
                                <td><input  type="text" size="3" disabled value="5"/></td>
                                <td><a href="#" class="enable-edit">Edit</a></td>
                            </tr>
                            <tr>
                                <td><input  type="text" size="12" disabled value="Restriction name"/></td>
                                <td><select name="site"><option value="">Choose</option><option value="north">North</option><option value="south" selected>South</option></select></td>
                                <td><select name="semester"><option value="">Choose</option><option value="a">A</option><option value="b" selected>B</option></select></td>
                                <td><input class="bin"  type="text" size="7" disabled value="10	:00"/></td>
                                <td><input class="bin" type="text" size="7" disabled value="00:30"/></td>
                                <td><input  type="text" size="3" disabled value="5"/></td>
                                <td><a href="#" class="enable-edit">Edit</a></td>
                            </tr>
                            <tr>
                                <td colspan="7">
                                    <a id="add-restriction">Add restriction</a>
                                </td>
                            </tr>
                        </tbody>
                    </table
                </form>
            </div>

            <script type="text/javascript">
                $(function() {
                    $.mask.definitions['p']='[+-]';
                    $.mask.definitions['h']='[012]';
                    $.mask.definitions['m']='[012345]';
                    $(".ra").mask("h9:m9");
                    $(".dec").mask("p99:m9");
                    $(".bin").mask("99:99");
                    $(".enable-edit").button();
                    $("#add-restriction").button({
                        icons: {
                            primary: 'ui-icon-plusthick'
                        }
                    })
                    $("#add-restriction").click(function() {
                        var emptyRow = '<tr class="notice"> <td><input  type="text" size="12" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><input  type="text" size="7" value=""/></td> <td><a href="#" class="save">Save</a></td> </tr>';

                        var newRow = $(this).closest('tr').before(emptyRow);
                        $(".save").each(function() {
                            $(this).button();
                            $(this).click(function() {
                                $(this).parents('tr').addClass('success');
                            });
                        });
                    })
                    $(".enable-edit").click(function() {
                        var row = $(this).closest("tr");
                        row.find("td input").each(function() {
                            $(this).removeAttr("disabled");
                            row.addClass("notice");
                        });
                    });
                    $('.restriction h3 a').toggle(function() {
                        var targetDiv = $(this).closest("li").children("div");
                        targetDiv.slideDown();
                    }, function() {
                        var targetDiv = $(this).closest("li").children("div");
                        targetDiv.slideUp();
                    });
                    $(".restriction div").hide();
                });
            </script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>



