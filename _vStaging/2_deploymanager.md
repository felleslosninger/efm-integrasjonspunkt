---
title: DeployManager
id: deploymanager
layout: default
description: Informasjon om Deploy Manager
isHome: false
---

+ [Overordnet arkitektur](../resources/overordnet-arkitektur.md)
+ [monitorApps.json](../resources/monitorApps.json)


### Installering og oppsett av Deploy Manager

Deploy Manager er en applikasjon som kan installeres lokalt for å holde integrasjonspunktet
og andre applikasjoner den skal overvåke og oppdatere. 

Ved å installere Deploy Manageren vil man sikre at siste fungerende versjon av Integrasjonspunktet alltid kjører.

Deploy Manageren holder underordnede applikasjoner oppdatert ved å sjekke om ny versjon er tilgjengelig, laste ned og restarte applikasjoner den overvåker.

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