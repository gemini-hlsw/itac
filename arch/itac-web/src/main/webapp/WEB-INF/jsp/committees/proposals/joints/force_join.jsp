<%--
  Created by IntelliJ IDEA.
  User: lobrien
  Date: 1/25/11
  Time: 12:07 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Create Joint Proposal</title></head>
<body>
<h1>Create Joint Proposal</h1>
<form name="create_joint" method="POST" target="/" >
   <p>Primary / Master Proposal ID : <input type="text" name="masterProposalId"/></p>
   <p>Secondary Proposal ID: <input type="text" name="secondaryProposalId"/></p>
   <input type="submit" name="submit"/>
</form>
</body>
</html>