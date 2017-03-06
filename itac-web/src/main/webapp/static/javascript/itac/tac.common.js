function validateNotNull(value, varName) {
    if(value == null){
        var msg = "Error on page: Invalid null value for variable '" + varName + "'. Reload page and repeat operation. Report if symptoms persist.";
        alert(msg);
        throw msg;
    }
}

//Handles Site Quality Constraint Edits
function postProposalEditWithProposalId(targetField, newValue, targetClass, naturalId, proposalId) {
    validateNotNull(targetField, "targetField");
    validateNotNull(newValue, "newValue");
    validateNotNull(targetClass, "targetClass");
    validateNotNull(naturalId, "naturalId");
    var encodedNewValue = encodeURIComponent(newValue);
    var encodedTargetClass = encodeURIComponent(targetClass);
    var encodedNaturalId = encodeURIComponent(naturalId);
    validateNotNull(encodedNewValue, "encodedNewValue");
    validateNotNull(encodedTargetClass, "encodedTargetClass");
    validateNotNull(encodedNaturalId, "encodedNaturalId");

    //POST
    $.ajax({
        type: "POST",
        url: './' + proposalId,
        data: "field=" + targetField + "&value=" + encodedNewValue + "&class=" + encodedTargetClass + "&naturalId=" + encodedNaturalId,
        success: function(msg) {
            var messageBox = $('#message_box');
            var messageBoxMessage = $('#message_box_message');
            messageBoxMessage.text(targetField + ' successfully changed to ' + newValue + '.');
            messageBoxMessage.addClass('success');
            messageBox.fadeIn().delay(5000).fadeOut();
        },
        error: function(msg) {
            var messageBox = $('#message_box');
            var messageBoxMessage = $('#message_box_message');
            messageBoxMessage.text('Change to ' + targetField + ' failed.  ' + msg);
            messageBoxMessage.addClass('error');
            messageBox.fadeIn().delay(5000).fadeOut();
        }
    });
}

function postProposalEdit(targetField, newValue, targetClass, naturalId) {
}

function stripHtml(html) {
    var tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent||tmp.innerText;
}

function isNumeric(value) {
    if (value == null || !value.toString().match(/^[-]?\d*\.?\d*$/)) return false;
    return true;
}

function trimWhitespace(value) {
    return value.replace(/^\s+|\s+$/g, "")
}

function isEmpty(value) {
    return value.length == 0;
}

function isEmail(allEmails) {
    var splitEmails = allEmails.split(",");
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    var email = true;
    for (i = 0; i < splitEmails.length; i++) {
        var splitIsEmail = re.test(trimWhitespace(splitEmails[i]));
        if (!splitIsEmail)
            email = false;
    }

    return email;
}

// Strip everything that isn't capital.
function onlyCapitals(string) {
    var output = '';
    for (i = 0; i < string.length; i++) {
        var character = string.charAt(i)
        if (character == character.toUpperCase())
            output += character;
    }

    return output;
}

// Return only the portion of the string parameter that before the first marker character.
function onlyBefore(string, marker) {
    var output = '';
    var markerFound = false;
    for (i = 0; i < string.length; i++) {
        var character = string.charAt(i)
        if (character == marker)
            markerFound = true;

        if (!markerFound)
            output += character;
        else
            break;
    }

    return output;
}

// Return only the portion of the string parameter that occurs after the last marker character.
function onlyAfterLast(string, marker) {
    var output = '';
    var markerFound = false;
    for (i = 0; i < string.length; i++) {
        var character = string.charAt(i)
        if (character == marker)
            output = '';
        else
            output += character;
    }

    return output;
}

// used to validate numerical input
function validate(newValue, element) {
    if (element.hasClass('numeric')) {
        if (!isNumeric(newValue)) {
            element.addClass('error');
            return false;
        } else {
            element.removeClass('error');
        }
    }
    if (element.hasClass('email')) {
        if (!isEmpty(newValue) && !isEmail(newValue)) {
            element.addClass('error');
            return false;
        } else {
            element.removeClass('error');
        }
    }

    return true;
}

// Used when breadcrumbing the header buttons.
var makeButtonLabelStrong = function(element) {
    var label = element.button( "option", "label");
    element.button( "option", "label", "<strong>" + label + "</strong>");
};