---
title: Installasjon av integrasjonspunkt
id: installasjonavintegrasjonspunkt
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

Dersom Java ikke er installert eller versjonen er for gammel, kan ny versjon lastes ned [her](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) og installeres.

### Installere Java Cryptography Extension (JCE)
Bruker du ny versjon av Java, må ny JCE installeres. Last ned JCE fra [Oracles sider](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).

Det er ikke noen enkel måte å sjekke om Java Cryptography Extension er installert. Ofte kan det enkleste være å bare laste ned og installere JCE, men om du ønsker å sjekke, kan du gå til mappen ```$JAVA_HOME/jre/lib/security``` og sjekke om filene ```US_export_policy.jar``` og ```local_policy.jar``` har nyere dato enn øvrige filer. Hvis datoen er lik, må du installere JCE.

### Opprette en bruker til Altinn formidlingstjeneste

Integrasjonspunktet kjører som et sluttbrukersystem mot AltInn's meldingsformidler. Integrsjonspunktet må registeres som et [sluttbrukersystem](https://www.altinn.no/no/Portalhjelp/Datasystemer/Sende-fra-sluttbrukersystem-datasystem/) i AltInn's portal.
Informasjon om hvordan du logger på AltInn portal finner du [her](https://www.altinn.no/no/Portalhjelp/Innlogging-og-rapportering/).

### Sette opp sertifikat i Java Key Store (JKS)
Under piloten vil det kjøre med sertifikatkjede der Difi er Trusted Root. Deltagere vil derfor få tildelt sertifikater etterhvert som de blir en del av piloten.

Når du har fått sertifikatet, må det legges inn på maskinen. Noter deg lokasjonen for sertifikatet, samt brukernavn og passord. 
Dette legges inn som propertiene, keystorelocation og privatekeypassword

### Laste opp public virksomhetssertifikat
Sertifikatet kan lastes opp til [virksomhetssertifikatserveren](https://beta-meldingsutveksling.difi.no/virksomhetssertifikat/)

### Installering og oppsett av Deploy Manager

Deploy Manager er en applikasjon som kan installeres lokalt for å holde integrasjonspunktet
og andre applikasjoner den skal overvåke oppdatert. 

Ved å installere Deploy Manageren vil man sikre at siste fungerende versjon av Integrasjonspunktet alltid kjører.

Deploy Manageren holder underordnede applikasjoner oppdatert ved å sjekke om ny versjon er tilgjengelig, laste ned og restarte applikasjoner. 

#### Installasjon/start

Start med å opprette en mappe med navn deploymanager på c:\ 

Last deretter ned Deploymanager fra artifactory (se link i top av dokument) og legg den i overnevnte mappe

Når DeployManager starter, vil den lete lokalt etter filen `data/monitorApps.json`. monitorApps.json inneholder informasjon om hvilke applikasjoner som skal vedlikeholdes av Deployment Manager. 

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
      "environmentVariables": "-Dspring.profiles.active=staging",
      "mainClass": "my.application.start.Application"
    }]
}
```

Eksempel, med en del preutfylte verdier, for integrajsonspunktet finner du [her](../resources/monitorApps.json).
Filen navngis som monitorApps.json og legges under \data

#### integrasjonspunkt-local.properties

Følgende verdier settest i integrasjonspunkt-local.properties

**Propertie**              	|**Beskrivelse**														|**Eksempel**
----------------------------|-----------------------------------------------------------------------|-----------------
noarksystem.endpointURL 	| URL integrasjonspunktet finner sak-/arkivsystemets BestEdu tjenester 	| 
noarksystem.type        	| Sak/-arkivsystem type 												|P360/Acos/ePhorte																	
noarksystem.userName\*   	|brukernavn for autentisering mot sakarkivsystem						|svc_sakark
noarksystem.password\*   	|passord for autentisering mot sakarkivsystem							|
noarksystem.domain*     	|domene sakarkivsystemet kjører på										|
							|																		|
adresseregister.endPointURL	|url til adresseregister												|
orgnumber               	| Organisasjonsnummer til din organisasjon (9 siffer)					|123456789
server.port					| Portnummer integrasjonspunktet skal kjøre på (default 9093) 			| 9093		  
keystorelocation 			| path til .jks fil 													|
privatekeypassword      	| Passord til keystore 													|
privatekeyalias  			| alieas til virksomhetssertifikatet som brukes i  integrasjonspunktet 	| 
							|																		|
altinn.username         	|brukernavnet du fikk når du opprettet AltInn systembruker				|
altinn.password         	|passord du satte når du opprettet AltInn systembruker					|
msh.endpointURL\*\*			|url til msh															|

\* Autentisering mot sakarkivsystem benyttes av P360
\*\* Denne brukes bare dersom du allerede har BestEdu og ønsker å sende filer via gammel MSH til deltakere som ikke er en del av piloten. Integrasjonspunktet vil da opptre som en proxy.

Last ned eksempel for [P360](../resources/integrasjonspunkt-local.properties_360), Acos, [ephorte](../resources/integrasjonspunkt-local.properties_ephorte)
Lagre filen på området c:\deploymanager og endre navnet til integrasjonspunkt-local.properties


Når du er ferdig skal strukturen på området se slik ut:

```
c:/
|-- deploymanager/
	|-- integrasjonspunkt-local.properties
	|-- deploy-manager-x.x.xx--xxx.jar
	|-- data
		|-- monitorApps.json
```



#### Start DeployManager
Deploy Manager startes fra kommandolinjen med kommandoen 

```
java -jar -Dapplication.runtime.environment=staging  deploy-manager[versjon].jar

Fra versjon 1.0.1
java -jar -Dspring.profiles.active=staging  deploy-manager[versjon].jar
```

Etter at Deploy Manageren er startet, vil den hente siste versjon av Integrasjonspunktet og starte det. Dette kan ta et par minutter.

Når Integrasjonspunktet er klart, kan du åpne en nettleser og taste ```http://localhost:9000/api/running```. 
Da vil du få en oversikt over applikasjoner som Deploy Manager kjører. 

```http://localhost:<port-til-integrasjonspunkt>/noarkExchange?wsdl``` gir response i form av en wsdl når Integrasjonspunktet har startet.

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



