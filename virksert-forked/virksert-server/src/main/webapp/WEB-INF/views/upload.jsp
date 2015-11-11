<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="row">
    <div class="col-lg-offset-2 col-lg-8">

        <h1>Last opp sertifikat</h1>

        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><strong>Validation error:</strong> <c:out value="${error}" /></div>
        </c:if>

        <div class="well well-lg">
            <form:form method="POST" commandName="uploadForm" enctype="multipart/form-data">
                        <div class="input-group">
                            <input type="file" name="file" class="form-control input-lg">
                    <span class="input-group-btn">
                        <button type="submit" class="btn btn-primary btn-lg">Last opp</button>
                    </span>
                </div>
            </form:form>
        </div>


        <h2>Verifisert opplasting</h2>

        <p class="lead">Sertifikater som lastes opp blir validert mot gjeldende regler for virksomhetssertifikater før sertifikatet blir gjort tilgjengelig. Når et sertifikat først er lastet opp vil det ikke bli fjernet før det utgår eller tilbakekalles (revokeres).</p>

    </div>
</div>