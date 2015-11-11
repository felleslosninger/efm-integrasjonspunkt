<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="well well-lg">
    <s:url value="/cert" var="certUrl" />
    <form:form method="GET" action="${certUrl}">
        <div class="input-group">
            <input type="text" name="identifier" class="form-control input-lg" value="<c:out value="${identifier}" />" placeholder="Organisasjonsnummer">
            <span class="input-group-btn">
                <button type="submit" class="btn btn-primary btn-lg">Finn</button>
            </span>
        </div>
    </form:form>
</div>