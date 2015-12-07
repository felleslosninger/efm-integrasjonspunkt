---
title: Overordnet arkitektur
id: overordnetarkitetktur
layout: default
description: Overordnede skisser hvordan Deploy Manager fungerer.
isHome: false
---
[Deployment Manager](#deploymanager)

## Arkitektur
Overordnede skisser hvordan Deploy Manager fungerer.

Når Deploy Manager starter, vil den aktivere alle prosessene for å sikre at siste versjon av underliggende applikasjoner kjører.

### Scheduler
Scheduler kjører tre prosesser for å holde monitorerte applikasjoner oppdatert og samtidig ikke belaste systemet det kjører på mer enn nødvendig. Listen over monitorerte applikasjoner er lokal, men er planlagt å flyttes til en sentral administrasjonsmodul.
![](https://github.com/difi/difi-deploy-manager/blob/master/doc/DMScheduler.png)

### CheckVersion
Check version skal sjekke listen over kjørende applikasjoner og versjoner mot et sentralt repository. Dersom nye applikasjoner eller versjoner av eksisterende applikasjoner blir funnet, blir de lagt til i listen over applikasjoner som skal lastes ned.
![](https://github.com/difi/difi-deploy-manager/blob/master/doc/DMFlowchartCheckVersion.png)

### Download
Download henter listen over applikasjoner som skal lastes ned med versjonsnummer, søker etter de i sentralt repository og laster de ned. Når nedlastingen er ferdig, blir listen over applikasjoner som skal restartes, oppdattert.
![](https://github.com/difi/difi-deploy-manager/blob/master/doc/DMFlowchartDownload.png)

##Restart
Restart henter listen over applikasjoner som er lastet ned og klar til å bli startet på nytt. Den gamle versonen av applikasjonen vil først bli stoppet før ny versjon startes. Dersom en prosess er i gang (for eksempel en melding er under behandling), vil restart vente til prosessen i applikasjonen som skal startes på nytt, er ferdig før faktisk omstart av applikasjonen gjennomføres.
![](https://github.com/difi/difi-deploy-manager/blob/master/doc/DMFlowchartRestart.png)