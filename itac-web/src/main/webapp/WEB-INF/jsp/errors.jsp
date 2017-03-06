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
        <link rel="stylesheet" href="/static/css/smoothness/jquery-ui-1.8.1.custom.css" type="text/css">
    </head>
    <body>
        <a href="/tac/committees/">Back to work</a>
        <h1>Errors</h1>

        <div>
            <div>
                    <ol>
                <c:forEach items="${ errors }" var="error">
                         <li>${ error }</li>
                </c:forEach>
                    </ol>
            </div>
        </div>
    </body>
</html>