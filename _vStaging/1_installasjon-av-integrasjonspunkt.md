---
title: Installasjon av integrasjonspunkt
pageid: installasjonavintegrasjonspunkt
layout: default
description: En liten oppstart for å komme i gang.
isHome: false
---

### Krav til kjøremiljø 

+ Java 8 med JCE installert
+ 3x tilgjengelig minne i forhold til størte meldinger ønsket sendt
+ Nødvendige brannmursåpninger
+ BestEdu ekspederingskanal skrudd på i sak-/arkivsystem


### Installere Java runtime environment (JRE)

Integrasjonspunktet og DeployMangager er Java applikasjoner og krever derfor at man har Java kjøremiljø installert på maskinen dette skal kjøre. 
For å verifisere om java er installert og hvilken versjon kan du i et komandolinje vindu bruke komandoen

```
java -version
```

Meldingsformidlingsapplikasjonen krever minimum versjon 1.8.0

Dersom Java ikke er installert eller versjonen er for gammel, kan ny versjon lastes ned [her](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) og installeres.

### Installere Java Cryptography Extension (JCE)
Bruker du ny versjon av Java, må ny JCE installeres. Last ned JCE fra [Oracles sider](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).

Det er ikke noen enkel måte å sjekke om Java Cryptography Extension er installert. Ofte kan det enkleste være å bare laste ned og installere JCE, men om du ønsker å sjekke, kan du gå til mappen ```$JAVA_HOME/jre/lib/security``` og sjekke om filene ```US_export_policy.jar``` og ```local_policy.jar``` har nyere dato enn øvrige filer. Hvis datoen er lik, må du installere JCE.
Dersom JCE mangler vil integrasjonspunket stoppe under oppstart og skrive logmelding om manglende JCE.

### Opprette en bruker til Altinn formidlingstjeneste

Integrasjonspunktet kjører som et sluttbrukersystem mot AltInn's meldingsformidler. Integrsjonspunktet må registeres som et [sluttbrukersystem](https://www.altinn.no/no/Portalhjelp/Datasystemer/Sende-fra-sluttbrukersystem-datasystem/) i AltInn's portal.
Informasjon om hvordan du logger på AltInn portal finner du [her](https://www.altinn.no/no/Portalhjelp/Innlogging-og-rapportering/).

Under opprettelse av sluttbrukersystemet vil du sette passord og få tildelt brukerid, disse skal senere brukes i properties filen som beskrives lenger ned

### Sette opp sertifikat i Java Key Store (JKS)
Under piloten vil det kjøre med sertifikatkjede der Difi er Trusted Root. Deltagere vil derfor få tildelt sertifikater etterhvert som de blir en del av piloten.

Når du har fått sertifikatet, må det legges inn på serveren du kjører integrasjonspunket. Noter deg lokasjonen for sertifikatet, samt brukernavn og passord. 
Dette legges senere inn som propertiene, keystorelocation, privatekeypassword, privatekeyalias

### Laste opp public virksomhetssertifikat
Sertifikatet kan lastes opp til [virksomhetssertifikatserveren](https://beta-meldingsutveksling.difi.no/virksomhetssertifikat/)

### Brannmursåpninger

+ 93.94.10.249:443
+ 93.94.10.18:8300


### Oppsett

Start med å opprette en mappe med navn integrajsonspunkt på c:\ 

Last deretter ned integrasjonspunktet fra artifactory (se link i top av dokument) og legg den i overnevnte mappe
Opprett filen integrasjonspunkt-local.properties på området

### integrasjonspunkt-local.properties

Følgende verdier settest i integrasjonspunkt-local.properties

**Propertie**              			|**Beskrivelse**														|**Eksempel**
------------------------------------|-----------------------------------------------------------------------|-----------------
difi.move.org.number               	|Organisasjonsnummer til din organisasjon (9 siffer)					|123456789
server.port							|Portnummer integrasjonspunktet skal kjøre på (default 9093) 			| 9093		  
									|																		|
difi.move.noarkSystem.endpointURL 	|URL integrasjonspunktet finner sak-/arkivsystemets BestEdu tjenester 	| 
difi.move.noarkSystem.type        	|Sak/-arkivsystem type 													|P360/Acos/ePhorte																	
difi.move.noarkSystem.username\*   	|Brukernavn for autentisering mot sakarkivsystem						|svc_sakark
difi.move.noarkSystem.password\*   	|Passord for autentisering mot sakarkivsystem							|
difi.move.noarkSystem.domain\*     	|Domene sakarkivsystemet kjører på										|
									|																		|
difi.move.msh.endpointURL\*\*		|Path til MSH 															|
									|																		|
difi.move.org.keystore.path			|Path til .jks fil	 													|
difi.move.org.keystore.password    	|Passord til keystore 													|
difi.move.org.keystore.alias		|Alieas til virksomhetssertifikatet som brukes i integrasjonspunktet 	| 
									|																		|
difi.move.altinn.username         	|Brukernavnet du fikk når du opprettet AltInn systembruker				|
difi.move.altinn.password         	|Passord du satte når du opprettet AltInn systembruker					|



\* Autentisering mot sakarkivsystem benyttes av P360

\*\* Denne brukes bare dersom du allerede har BestEdu og ønsker å sende filer via gammel MSH til deltakere som ikke er en del av piloten. Integrasjonspunktet vil da opptre som en proxy.

Last ned eksempel for [P360](../resources/integrasjonspunkt-local.properties_360), Acos, [ephorte](../resources/integrasjonspunkt-local.properties_ephorte)
Lagre filen på området c:\integrajsonspunkt og endre navnet til integrasjonspunkt-local.properties


Når du er ferdig skal strukturen på området se slik ut:

```
c:/
|-- integrasjonspunkt/
	|-- integrasjonspunkt-local.properties
```


### Konfigurere sak-/arkivsystem til å bruke Integrsjonspunktet

Oppsett for Acos, ePhorte, [P360](../resources/Oppsett360.docx)


### Start Integrasjonspunktet
Integrasjonspunktet startes fra kommandolinjen med kommandoen (Kjør som admindistrator)

```
java -jar -Dspring.profiles.active=staging  integrasjonspunktet[versjon].jar
```


kommandoen
```
http://localhost:<port-til-integrasjonspunkt>/noarkExchange?wsdl
``` 

gir response i form av en wsdl når Integrasjonspunktet har startet.




