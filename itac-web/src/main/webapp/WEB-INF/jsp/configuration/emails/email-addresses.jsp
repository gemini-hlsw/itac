<%@ include file="../../fragments/header.jspf" %>


<div class="span-18 colborder ">
    <H1>NGO Contact Emails</H1>
    <c:forEach items="${partnerList}" var="partner" varStatus="status">
        <h2>${ partner.name}</h2>

        <p><span class="email" data-partner="${ partner.name }">${ partner.ngoFeedbackEmail }</span></p>
    </c:forEach>
    <hr/>
    <p>Enter multiple email addresses using ';' as a separator. </p>
</div>

<script type="text/javascript">
    //Accepts string, returns boolean
    function validate(email) {
        //Absurd and, apparently, not even truly in compliance with RFC822... But catches all common mistakes...
        var regex = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return regex.test(email);
    }

    function postNewEmailAddress(partner, newContactEmail){
         $.ajax({
                type: "POST",
                url: "./email-addresses",
                data: "partnerName=" + partner + "&newEmailAddress=" + newContactEmail,
                success: function(msg) {
                    var messageBox = $('#message_box');
                    var messageBoxMessage = $('#message_box_message');
                    messageBoxMessage.text('Email successfully changed to ' + newValue + '.');
                    messageBoxMessage.addClass('success');
                    messageBox.fadeIn().delay(5000).fadeOut();
                },
                error: function(msg) {
                    var messageBox = $('#message_box');
                    var messageBoxMessage = $('#message_box_message');
                    messageBoxMessage.text('Change to email failed.  ' + msg);
                    messageBoxMessage.addClass('error');
                    messageBox.fadeIn().delay(5000).fadeOut();
                }
            });
    }

    $(function() {
        var oldValue = ""; //Used to hold the previous value of an email (restored if invalid email entered)
        //Editable text for emails
        $('span.email').editableText({
            newLinesEnabled: false
        });

        //On change, validate and then POST the change
        $('span.email').change(function() {
            var newValue = $(this).html().trim();
            var valid = true; //In the end, *DON'T* validate it, because we want flexibility, e.g., "foo@bar.com;bat@baz.com"
            //var valid = validate(newValue);
            if (valid == true) {
                postNewEmailAddress($(this).attr('data-partner'), newValue);
            } else {
                //Restore old value
                alert("'" + newValue + "' not recognized as valid email. Try again.");
                $(this).text(oldValue);
            }
        });

        <!-- highlight field on edit -->
        $('span.email').bind('onStartEditing', function() {
            $(this).css('background-color', '#ffc');
            $(this).css('border', '1px dotted');
            $(this).css('font-size', '16px');
            $(this).css('padding', '4px');
            oldValue = $(this).html();
        });
        <!-- Clear highlights -->
        $('span.email').bind('onStopEditing', function() {
            $(this).css('background-color', '');
            $(this).css('border', '');
            $(this).css('font-size', '');
            $(this).css('padding', '');
        });


        <!-- help -->
        page.helpContent =
                'Edit contact addresses. ';


    });
</script>

<%@ include file="../../fragments/sidebar.jspf" %>
<%@ include file="../../fragments/footer.jspf" %>
