<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="date" class="java.util.Date" />
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Error</title>
        <meta charset="utf-8"/>
        <link rel="shortcut icon" href="/static/images/favicon.ico" type="image/x-icon"/>

        <link rel="stylesheet" href="/static/css/blueprint/screen.css" type="text/css" media="screen, projection">
        <link rel="stylesheet" href="/static/css/blueprint/print.css" type="text/css" media="print">
        <!--[if lt IE 8]>
        <link rel="stylesheet" href="/static/css/blueprint/ie.css" type="text/css" media="screen, projection"><![endif]-->
        <script type="text/javascript" src="/static/javascript/jquery-1.4.4.min.js"></script>
    </head>
    <body>
        <a href="/tac/committees/">Back to work</a>
        <h1>Error</h1>
        <p>Something has gone wrong.  Below are the error details.</p>
        <h2>Details</h2>
        <ul>
            <li>Timestamp: <fmt:formatDate value="${date}" type="both" dateStyle="long" timeStyle="long" /></li>
            <li>Action: <c:out value="${requestScope['javax.servlet.forward.request_uri']}" />     </li>
            <li>Exception: <c:out value="${requestScope['javax.servlet.error.exception']}" /> </li>
            <li>Message: <c:out value="${requestScope['javax.servlet.error.message']}" /></li>
            <li>Status code: <c:out value="${requestScope['javax.servlet.error.status_code']}" /></li>
            <li>User agent: <c:out value="${header['user-agent']}" /></li>
            <li>Parameters: <c:out value="${request.parameterMap}" /></li>
            <li>Version: ${ version } </li>
        </ul>

        <a href="#" id="showException">Show exception</a>
        <a href="#" id="showErrors">Show errors</a>
        <a href="#" id="showErrorMap">Show error map</a>

        <div>
            <div id="exception" class="box">
                <span class="large"><strong>Exception:</strong>${ exception }      </span>
            </div>
            <div id="errors" class="box">
                <span class="large"><strong>Errors:</strong>${ errors }     </span>
            </div>
            <div id="errorMap" class="box">
                <span class="large"><strong>Error Map:</strong>${ errorMap }</span>
            </div>
        </div>

        <script type="text/javascript">
            $(document).ready(function () {
                $('#exception, #errors, #errorMap').each(function() { $(this).hide(); });

                $("#showException").click(function() { $('#exception').show(); });
                $("#showErrors").click(function() { $('#errors').show(); });
                $("#showErrorMap").click(function() { $('#errorMap').show(); });
            });
        </script>
    </body>
</html>