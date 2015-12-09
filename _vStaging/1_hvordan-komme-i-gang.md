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
* URL til MSH (Dette gjelder kun dersom du bruker BestEdu fra tidligere og kommuniserer med mottakere som ikke deltar i piloten)



### Installere Java runtime environment (JRE)

Integrasjonspunktet og DeployMangager er Java applikasjoner og krever derfor at man har Java kjøremiljø installert på maskinen dette skal kjøre. 
For å verifisere om java er installert og hvilken versjon kan du i et komandolinje vindu bruke komandoen

```
java -version
```

Meldingsformidlingsapplikasjonen krever minimum versjon 1.7.0

Dersom Java ikke er installert eller versjonen er for gammel, kan ny versjon lastes ned [her](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html) og installeres.

### Installere Java Cryptography Extension (JCE)
Bruker du ny versjon av Java, må ny JCE installeres. Last ned JCE fra [Oracles sider](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html).

Det er ikke noen enkel måte å sjekke om Java Cryptography Extension er installert. Ofte kan det enkleste være å bare laste ned og installere JCE, men om du ønsker å sjekke, kan du gå til mappen ```$JAVA_HOME/jre/lib/security``` og sjekke om filene ```US_export_policy.jar``` og ```local_policy.jar``` har nyere dato enn øvrige filer. Hvis datoen er lik, må du installere JCE.

### Opprette en bruker til Altinn formidlingstjeneste

Integrasjonspunktet kjører som et sluttbrukersystem mot AltInns meldingsformidler. Integrsjonspunktet må registeres som et [sluttbrukersystem](https://www.altinn.no/no/Portalhjelp/Datasystemer/Sende-fra-sluttbrukersystem-datasystem/) i AltInns portal
Informasjon om hvordan du logger på AltInn portal finner du [her](https://www.altinn.no/no/Portalhjelp/Innlogging-og-rapportering/).

### Sette opp sertifikat i Java Key Store (JKS)
Under piloten vil det kjøre med sertifikatkjede der Difi er Trusted Root. Deltagere vil derfor få tildelt sertifikater etterhvert som de blir en del av piloten.

Når du har fått sertifikatet, må det legges inn på maskinen. Noter deg lokasjonen for sertifikatet, samt brukernavn og passord. Dette legges inn som en del av [Konfigurasjonen](#konfigurasjon)

### Laste opp public virksomhetssertifikat
Sertifikatet kan lastes opp til [virksomhetssertifikatserveren](http://virksert.herokuapp.com/)

### Konfigurere sak-/arkivsystem til å bruke Integrsjonspunktet

Oppsett for Acos, ePhorte, [P360](../resources/Oppsett360.docx)

### Sentral kontroll på integrasjonspunkt
Sentral overvåking innebærer at status samt en del statistikk sendes sentralt. Man vil tidligere få melding om feil, og man kan i enkelte tilfeller rette feilen før den påvirker produksjonen (før en feilet melding blir savnet).


** Parametre for aktivering av sentral overvåking **
* spring.boot.admin.url
* spring.boot.admin.client.name
* spring.boot.admin.autoDeregistration
* spring.boot.admin.client.serviceUrl

### Kjøre integrasjonspunktet
Når DeployMangager er startet vil denne automatisk laste ned og starte siste versjon av integrasjonspunktet.



