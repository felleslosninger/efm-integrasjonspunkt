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
Deploy Manager kan startes ved å laste ned jar-filen, og startes fra kommandolinjen og startes med kommandoen 

```
java -jar -Dapplication.runtime.environment=staging  deploy-manager[versjon].jar
```


Når den starter, vil den lete lokalt etter filen `data/monitorApps.json`. monitorApps.json inneholder informasjon om hvilke applikasjoner som skal vedlikeholdes av Deployment Manager. 

For å legge til applikasjoner som skal vedlikeholdes, må de legges manuelt inn i monitorApps.json. Applikasjoner legges til som vist under. 

```json
{"artifacts": [
    {
      "name": "Name of application",
      "groupId": "my.app.group.id",
      "artifactId": "my.app.artifact.id",
      "activeVersion": "activeVersion",
      "artifactType": "JAR",
      "filename": "filename.jar",
      "vmOptions": "-Xms=512m -Xmx=512m",
      "environmentVariables": "-Dspring.boot.admin.url=http://localhost:9000 ",
      "mainClass": "my.application.start.Application"
    }]
}
```

Eksemple for integrajsonspunktet finner du [her](../resources/monitorApps.json).
Filen navngis som monitorApps.json og legges under \data

### Verifisere at alt er oppe og snurrer
Etter at Deploy Manageren er startet, vil den hente siste versjon av Integrasjonspunktet og starte det. Dette kan ta et par minutter.

Når Integrasjonspunktet er klart, kan du åpne en nettleser og taste 

```http://localhost:9000/api/running```. 

Da vil du få en oversikt over applikasjoner som Deploy Manager kjører. 

```http://localhost:<port-til-integrasjonspunkt>/noarkExchange?wsdl``` 

gir response i form av en wsdl når Integrasjonspunktet har startet.