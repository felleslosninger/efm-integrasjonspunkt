<%@ taglib prefix="t" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>

    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Virksomhetssertifikat</title>

    <link rel="stylesheet" href="<s:url value="/webjars/bootstrap/3.3.5/css/bootstrap.min.css" />">

</head>
<body>

<nav class="navbar navbar-default">
    <div class="container">
        <div class="navbar-header">
            <a class="navbar-brand" href="/">Virksomhetssertifikat</a>
        </div>
    </div>
</nav>

<div class="container">
    <t:insertAttribute name="content"/>
</div>

</body>
</html>