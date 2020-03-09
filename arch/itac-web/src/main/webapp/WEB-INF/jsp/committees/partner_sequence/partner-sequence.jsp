<%@ include file="../../fragments/header.jspf" %>

<c:set var="currentIsProportional" value="unchecked"/>
<c:set var="customSequenceName" value="ProportionalPartnerSequence"/>
<c:choose>
    <c:when test="${partnerSequenceName eq 'ProportionalPartnerSequence'}"><c:set var="currentIsProportional" value="checked"/></c:when>
    <c:otherwise><c:set var="customSequenceName" value="${partnerSequenceName}"/></c:otherwise>
</c:choose>

<div class="span-18 colborder ">
    <H1>Partner Sequence</H1>

    <form action="./partner_sequence" id="form" method="POST">
        <p>
            <input type="checkbox" id="proportional" name="proportional" ${currentIsProportional}>Use proportional partner sequencing</input>
        </p>
        <hr/>
        <h2>Or</h2>
        <p>
            <label class="span-2" for="sequence">Partner Sequence</label>
            <textarea id="csv" name="csv" disabled="disabled" rows="10" cols="80" minlength="2">${partnerSequence}</textarea>
        </p>
        <p id="proportions">(Initial sequence has: ${partnerSequenceProportions})</p>

        <p>
            <input id="repeat" type="radio" name="repeat" value="repeating" checked="true" disabled="disabled">Repeat
        </p>

        <p>
            <input id="onetime" type="radio" name="repeat" value="once" disabled="disabled">First cycle only
        </p>

        <p>
            <label class="span-2" for="name">Name</label>
            <input type="text" id="name" name="name" class="required" value="${customSequenceName}" minlength="1" disabled="disabled">
        </p>

        <p>
            <input type="submit" value="Save sequence">
        </p>
    </form>
    <hr/>
    <p>"Use proportional partner sequencing" will use a deterministic algorithm that produces a
    sequence of partners proportional to their partner share (it returns the partner whose percentage of appearances in
        the sequence-so-far is the greatest percentage shortfall from their desired rate of appearance).
    </p>
    <p>A custom partner sequence is a comma-separated sequence of partner country keys. The sequence can either be used for the entire
        the queue (Choose "Repeat") or just for initial picks (Choose "One time only"). The ratio of partner % to proportion in the sequence
        is the chief determinant of the quantum (e.g., if a partner is allocated 10% of the queue but only appears 5 times in a custom sequence of length 100,
        that partner's quantum will be ~6 hours).

    </p>

</div>

<script type="text/javascript">
    function enableMainForm(bool){
                var els = [$("#csv"), $("#repeat"), $("#onetime"), $("#name")];
                els.map(function(el) {
                    try{
                    if(bool){
                        el.removeAttr("disabled");
                        el.classes.addClass("required");
                    }else{
                        el.attr("disabled", "disabled");
                        el.removeClass("required");
                    }
                    }catch(ex){
                        //Swallow...May get thrown if el.classes is null above
                    }
                });
            }

    function calcAndShowPartnerPercentages(){
        try{
            var csv = $('#csv').val();
            var seq = csv.split(",")
            var counts = {};
            for(var i = 0; i < seq.length; i++){
                var key = seq[i];
                if(typeof(counts[key]) == "undefined"){
                    counts[key] = 0;
                }
                var count = counts[key];
                counts[key] = count + 1;
            }
            var proportions = {};
            for(var key in counts){
                proportions[key] = 100.0 * counts[key] / seq.length;
            }

            var description = "(Proportions shown: ";
            for(var key in proportions){
                description += key + ": " + proportions[key].toFixed(2) + "% ";
            }
            description += ")";
            $('#proportions').text(description);
        }catch(x){
            //Swallow it, becauser it's probably just a temporary formatting error
        }
    }

    $(function() {
        //Page-load set of values
        enableMainForm(! $('#proportional').is(':checked'));

        //Disable/Enable other formfields based on proportional checkbox
        $('#proportional').change(function() {
            var isChecked = $(this).is(':checked');
            enableMainForm(! isChecked);
        });

        calcAndShowPartnerPercentages();
        $('#csv').keyup(calcAndShowPartnerPercentages);
        $('#csv').change(calcAndShowPartnerPercentages);

        //Enable validation
        $('#form').validate();
    });


</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>

