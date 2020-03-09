<%@ taglib prefix='c' uri='http://java.sun.com/jstl/core_rt' %>

<html>
<head>
    <title>Gemini International Time Allocation Committee</title>
    <style type="text/css">
        body {
            color : #665555;
            margin : 0 0 auto auto;
        }

        .lifted {
            background : #bbbbaa;
            padding : 0.25em;
            -moz-border-radius : 8px;
            border-radius : 8px;
        }

        .label {
            border-radius : 2px;
            -moz-border-radius : 2px;
            text-align : right;
        }

        #bg_img {
            width: 45em;
            height: 60em;
            padding: 0px;
            margin: 0px;
        }

        #content {
            position: relative;
            top : -56em;
            left: 2em;
            width: 25em;
            z-index : 1;
        }

    </style>
    <script type="text/javascript" src="/static/javascript/jquery-1.4.4.min.js"></script>

</head>

<body>
<img id="bg_img" src="/static/images/analemma_ayiomamitis_big.jpg"/>

<div id="content" class="lifted">

    <h1><span class="lifted">Gemini Observatory ITAC</span></h1>

    <c:if test="${not empty param.login_error}">
        <p style="text-color:red">
            Your login attempt was not successful, try again.<br/><br/>
            Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>.
        </p>
    </c:if>

     <form name="f" action="<c:url value='/j_spring_security_check'/>" method="POST">
        <table class="lifted">
            <tr>
                <td align="right"><span>User:</span></td>
                <td><input type='text' name='j_username' autofocus="autofocus   "
                           value='<c:if test="${not empty param.login_error}"><c:out value="${SPRING_SECURITY_LAST_USERNAME}"/></c:if>'/>
                </td>
            </tr>
            <tr>
                <td align="right"><span >Password:</span></td>
                <td><input type='password' name='j_password'></td>
            </tr>

            <tr>
                <td colspan='2' align="center"><input name="submit" type="submit" value="Login to ITAC"></td>
            </tr>
        </table>
    </form>
</div>
</body>
<script type="text/javascript">
        $(document).ready(function() {
            sessionStorage.setItem("partnerFilter", "true");
        });
</script>
</html>