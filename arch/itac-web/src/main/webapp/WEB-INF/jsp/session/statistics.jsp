<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="security" %>
<%@ page import="edu.gemini.tac.itac.web.UrlFor" %>
<%@ page import="edu.gemini.tac.itac.web.Link" %>


<html lang="en">

<head><title>Hibernate Statistics</title></head>
<body>
<h1>Hibernate Statistics</h1>
<h2>Slowest query</h2>
${slowestQuery}
<p>
Time: ${slowestQueryTime}

<h2>Most called</h2>
${maxQueryQuery}
<p>
Called: ${maxQueryCount}

<h2>Queries whose executionCount * avgTime > 1000ms</h2>
<c:forEach items="${map}" var="entry" varStatus ="status">
<h2>${entry.key}</h2>
<ul>
    <li>CacheHitCount ${entry.value.cacheHitCount}</li>
    <li>CacheMissCount ${entry.value.cacheMissCount}</li>
    <li>ExecutionCount ${entry.value.executionCount}</li>
    <li>ExecutionAvgTime ${entry.value.executionAvgTime}</li>
</ul>

</c:forEach>
</body>
</html>