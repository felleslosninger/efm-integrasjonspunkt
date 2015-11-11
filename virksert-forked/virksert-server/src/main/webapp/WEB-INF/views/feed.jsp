<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="row">
    <div class="col-lg-offset-2 col-lg-8">

        <h1>Abonnere på hendelser</h1>

        <h2>Utløpende sertifikater</h2>

        <p class="lead">Inneholder oversikt over sertifikater i tjenesten som vil løpe ut i løpet av de neste 7 dagene.</p>

        <p><a href="<s:url value="/feed/expiring.json" />" class="btn btn-default btn-lg">JSON</a></p>

        <h2>Tilbakekalte sertifikater</h2>

        <p class="lead">Inneholder oversikt over sertifikater i tjenesten som er oppdaget å være tilbakekalt de siste 7 dagene.</p>

        <p><a href="<s:url value="/feed/revoked.json" />" class="btn btn-default btn-lg">JSON</a></p>

        <h2>Oppdaterte sertifikater</h2>

        <p class="lead">Inneholder oversikt over sertifikater i tjenesten er som er oppdatert eller gjort tilgjengelig de siste 7 dagene.</p>

        <p><a href="<s:url value="/feed/updated.json" />" class="btn btn-default btn-lg">JSON</a></p>

        <h2>Innhold i oversikter</h2>

        <p class="lead">Oversiktene inneholder følgende data for sertifikatene:</p>

        <dl class="lead dl-horizontal">
            <dt>identifier</dt>
            <dd>Organisasjonernummeret knyttet til sertifikatet.</dd>

            <dt>serialnumber</dt>
            <dd>Serienummeret til sertifikatet, må ikke forveksles med tilfeller hvor feltet "SERIALNUMBER" er brukt for organisasjonsnummer.</dd>

            <dt>expiration</dt>
            <dd>Tidspunkt for utløp av sertifikatet angitt i millisekunder (epoch time).</dd>

            <dt>updated</dt>
            <dd>Tidspunkt for sise oppdatering av sertifikatet angitt i millisekunder (epoch time).</dd>
        </dl>

    </div>
</div>
