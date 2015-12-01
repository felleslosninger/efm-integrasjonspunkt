---
title: Konfigurasjon
id: konfigurasjon
layout: default
description: Hvordan sette verider i .properties filen
isHome: false
---

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
keystorelocation 			| path til '.jks fil 													|
privatekeypassword      	| Passord til keystore 													|
privatekeyalias  			| alieas til virksomhetssertifikatet som brukes i  integrasjonspunktet 	| 
							|																		|
altinn.external_service_code|																		|
altinn.external_service_edition_code|																|
altinn.username         	|brukernavnet du fikk når du opprettet AltInn systembruker				|
altinn.password         	|passord du satte når du opprettet AltInn systembruker					|
msh.endpointURL\*\*			|url til msh															|

\* Autentisering mot sakarkivsystem benyttes av P360

\*\* Denne brukes bare dersom du allerede har BestEdu og ønsker å sende filer via gammel MSH til deltakere som ikke er en del av piloten. Integrasjonspunktet vil da opptre som en proxy.

Last ned eksempel for [P360](../resources/integrasjonspunkt-local.properties_360), Acos, [ephorte](../resources/integrasjonspunkt-local.properties_ephorte)