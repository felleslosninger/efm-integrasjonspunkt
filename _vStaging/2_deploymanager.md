---
title: DeployManager
id: deploymanager
layout: default
description: Informasjon om Deploy Manager
isHome: false
---

### Installering og oppsett av Deploy Manager

Deploy Manager er en applikasjon som kan installeres lokalt for å holde integrasjonspunktet
og andre applikasjoner den skal overvåke oppdatert. 

Ved å installere Deploy Manageren vil man sikre at siste fungerende versjon av Integrasjonspunktet alltid kjører.

Deploy Manageren holder underordnede applikasjoner oppdatert ved å sjekke om ny versjon er tilgjengelig, laste ned og restarte applikasjoner. 

### Installasjon/start
Deploy Manager kan startes ved å laste ned jar-filen, og startes fra kommandolinjen og startes med kommandoen ```
java -jar deploy-manager[versjon].jar
```


Når den starter, vil den lete lokalt etter filen `data/monitorApps.json`. monitorApps.json inneholder informasjon om hvilke applikasjoner som skal vedlikeholdes av Deployment Manager. 

For å legge til applikasjoner som skal vedlikeholdes, må de legges manuelt inn i monitorApps.json. [Struktur på monitorApps.json](../resources/monitorApps.json)

**Parameter**                      |**Verdi**  		|**Beskrivelse**
-----------------------------------|----------------|---------------------------------------------------------------------
application.runtime.environment    |enum     		|test, staging, production
spring.boot.admin.url              |url:port 		|URL til server hvor monitoreringsapplikasjonen er installert
spring.boot.admin.client.serviceUrl|url:port 		|URL monitor-appen kan kalle inn til DM for metrics m.m.
spring.application.name            |String   		|Navnet applikasjonen skal registrere seg på monitoreringsappen som
application.runtime.status         |test, production|Modus applikasjonen skal kjøre i, hvilket nexus repo som skal benyttes


### Verifisere at alt er oppe og snurrer
Etter at Deploy Manageren er startet, vil den hente siste versjon av Integrasjonspunktet og starte det. Dette kan ta et par minutter.

Når Integrasjonspunktet er klart, kan du åpne en nettleser og taste 

```http://localhost:9000/api/running```. 

Da vil du få en oversikt over applikasjoner som Deploy Manager kjører. 

```http://localhost:<port-til-integrasjonspunkt>/noarkExchange?wsdl``` 

gir response i form av en wsdl når Integrasjonspunktet har startet.