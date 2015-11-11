<%@ page session="false" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<h1 class="sr-only">Hjem</h1>

<div class="row">
    <div class="col-md-6">
        <div style="text-align: center;">
            <a href="<s:url value="/cert" />">
                <div class="well well-lg" style="font-size: 30pt;">
                    <i class="glyphicon glyphicon-search" style="font-size: 60pt;"></i><br />
                    Finn sertifikat
                </div>
            </a>
        </div>
    </div>
    <div class="col-md-6">
        <div style="text-align: center;">
            <a href="<s:url value="/upload" />">
                <div class="well well-lg" style="font-size: 30pt;">
                    <i class="glyphicon glyphicon-cloud-upload" style="font-size: 60pt;"></i><br />
                    Last opp sertifikat
                </div>
            </a>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-lg-offset-2 col-lg-8">

        <h2>Digitale sertifikater er mest verdt når de er delt</h2>

        <p class="lead">Digitale sertifikater som ikke deles blir ikke brukt. Denne tjenesten gjør det enkelt å dele norske virksomhetssertifikater mellom virksomheter. Sertifikater som lastes opp blir tilgjengelig gjennom definerte grensesnitter for økt bruk.</p>

        <h2>Verdiøkende tjenester</h2>

        <p class="lead">Det er mulig å <a href="<s:url value="/feed" />">abonnere</a> på hendelser i tjenesten for å automatisere oppgaver relatert til bruk av virksomhetssertifikater i egne løsninger.</p>

        <h2>Vilkår for bruk</h2>

        <p class="lead">Tjenesten kan fritt brukes, men bruk som medfører uforholdsvis stor last eller bruk som kan medføre at tjenesten blir utilgjengelig vil bli sperret.</p>

    </div>
</div>
