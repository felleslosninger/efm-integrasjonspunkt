<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="row">
    <div class="col-lg-offset-2 col-lg-8">

        <h1>Finn sertifikat</h1>

        <c:import url="_cert_search.jsp" />

        <h2>Sertifikat er utløpt</h2>

        <p class="lead">Sertifikatet du ser etter er utløpt og er derfor ikke lenger tilgjengelig.</p>


    </div>
</div>