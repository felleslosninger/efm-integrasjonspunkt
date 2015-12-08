---
title: Hvordan komme i gang
id: hvordankommeigang
layout: default
description: En liten oppstart for å komme i gang.
isHome: false
---


### Dette trenger du å finne ut av
Driftsalternativer. Kan din driftsorganisasjon forvalte integrasjonspunktet?

* Url til arkivsystemet
* Arkivsystem type
* Organisasjonsnummeret til virksomheten
* Brukernavn og passord til Altinn formidlingstjeneste
* Java Keystore lokasjon (filen hvor sertifikatene ligger. _F. eks. crt/mykeystore.jks_)
* Java Keystore passord (passordet for å lese sertifikatene)
* Java Keystore nøkkelord (brukes for å finne riktig sertifikat i keystore)
* Hvilken port Integrasjonspunktet kan bruke
* Hvilken port Deploy Manageren kan bruke

* URL til MSH (Dette gjelder kun dersom du bruker BestEdu fra tidligere og kommuniserer med mottakere som ikke er del av piloten)



### Installere Java runtime environment (JRE)

Integrasjonspunktet og DeployMangager er Java applikasjoner og krever derfor at man har Java kjøremiljø installert på maskinen dette skal kjøre. 
For å verifisere om java er installert, og versjon kan du i et komandolinje vindu bruke komandoen

```
java -version
```

Meldingsformiddlingsapplikasjonene kriver minimu versjon 1.7.0

Dersom Java ikke er installert, eller versjonen er for gammel, kan ny versjon lastes ned [her](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html) og installeres.

### Installere Java Cryptography Extension (JCE)
Dersom det er en ny versjon av Java, så my JCE installeres. Last ned JCE fra [Oracles sider](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html).

Det er ikke noen enkel måte å sjekke om Java Cryptography Extension er installert. Ofte kan det enkleste være å bare laste ned og installere JCE, men om du ønsker å sjekke, kan du gå til mappen ```$JAVA_HOME/jre/lib/security``` og sjekke om filene ```US_export_policy.jar``` og ```local_policy.jar``` har nyere dato enn øvrige filer. Hvis datoen er lik, må du installere JCE.

### Opprette en bruker til Altinn formidlingstjeneste
_Prosessen for oppretting av brukere til Altinn Formidlingstjeneste er under arbeid_

### Sette opp sertifikat i Java Key Store (JKS)
Under piloten vil det kjøre med sertifkatkjede der Difi er Trusted Root. Deltagere vil derfor få tildelt sertifikater etterhvert som de blir en del av piloten.

Når du har fått sertifikatet, må det legges inn på maskinen. Noter deg lokasjonen for sertifikatet, samt brukernavn og passord. Dette legges inn som en del av [konfigurasjonen] (#konfigurasjon)


### Laste opp public virksomhetssertifikat til virksomhetssertifikatserveren
Sertifikatet kan lastes opp til [staging-/demo-server her](http://beta-meldingsutveksling.difi.no:9998)

### Laste ned og klargjøre Deploy Manager
Installering og oppsett av Deploy Manager er beskrevet i [installasjonsguiden](#deploymanager).


### Verifisere at alt er oppe og snurrer
Etter at Deploy Manageren er startet, vil den hente siste versjon av Integrasjonspunktet og starte det. Dette kan ta et par minutter.

Når Integrasjonspunktet er klart, kan du åpne en nettleser og taste 

```http://localhost:9000/api/running```. 

Da vil du få en oversikt over applikasjoner som Deploy Manager kjører. 

```http://localhost:<port-til-integrasjonspunkt>/noarkExchange?wsdl``` 

gir response i form av en wsdl når Integrasjonspunktet har startet.

### Kjøre integrasjonspunktet
Når DeployMangager er startet vil denne automatisk laste ned og starte siste versjon av integrasjonspunktet. 

### Konfigurere sak-/arkivsystem til å bruke Integrsjonspunktet


## Dersom du ønsker at Integrasjonspunktet skal overvåkes sentralt
Sentral overvåking innebærer at status samt en del statistikk sendes sentralt. Man vil tidligere få melding om feil, og man kan i enkelte tilfeller rette feilen før den påvirker produksjonen (før en feilet melding blir savnet).

**Parametre for aktivering av sentral overvåking**
1. spring.boot.admin.url
2. spring.boot.admin.client.name
3. spring.boot.admin.autoDeregistration
4. spring.boot.admin.client.serviceUrl

## Feilsøking
Dersom du får feilmeldingen av typen
`javax.xml.ws.WebServiceException: Failed to access the WSDL at: https://at06.altinn.basefarm.net/ServiceEngineExternal/BrokerServiceExternalBasic.svc?wsdl. It failed with: 
    sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target.`

Må sertifikates installeres i JKS fila _cacerts_ som ligger i _$JAVA_HOME/jre/lib/security_. For dette formålet kan du bruke verktøyet **Portecle** etter guiden [Connecting to ssl services](https://confluence.atlassian.com/jira/connecting-to-ssl-services-117455.html). Da tar man inspect SSL certificate mot URL i feilmeldingen. I eksempelet over er det at06.altinn.basefarm.net

