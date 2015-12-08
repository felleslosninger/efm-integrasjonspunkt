---
title: Konfigurasjon
id: konfigurasjon
layout: default
description: Hvordan sette verider i .properties filen
isHome: false
---

Lokale konfigurasjonsverdier legges i en tekstfil med navnet integrasjonspunkt-local.properties
Denne filen legges på samme nivå som DeployManager ligger.

**Propertie**              	|**Beskrivelse**														|**Eksempel**
----------------------------|-----------------------------------------------------------------------|-----------------
noarksystem.endpointURL 	| URL integrasjonspunktet finner sak-/arkivsystemets BestEdu tjenester 	| 
noarksystem.type        	| Sak/-arkivsystem type 												|P360/Acos/ePhorte																	
noarksystem.userName\*   	|brukernavn for autentisering mot sakarkivsystem						|svc_sakark
noarksystem.password\*   	|passord for autentisering mot sakarkivsystem							|
noarksystem.domain*     	|domene sakarkivsystemet kjører på										|
							|																		|
orgnumber               	| Organisasjonsnummer til din organisasjon (9 siffer)					|123456789
server.port					| Portnummer integrasjonspunktet skal kjøre på (default 9093) 			| 9093		  
keystorelocation 			| path til '.jks fil 													|
privatekeypassword      	| Passord til keystore 													|
privatekeyalias  			| alieas til virksomhetssertifikatet som brukes i  integrasjonspunktet 	| 
							|																		|
altinn.username         	|brukernavnet du fikk når du opprettet AltInn systembruker				|
altinn.password         	|passord du satte når du opprettet AltInn systembruker					|
msh.endpointURL\*\*			|url til msh															|

\* Autentisering mot sakarkivsystem benyttes av P360

\*\* Denne brukes bare dersom du allerede har BestEdu og ønsker å sende filer via gammel MSH til deltakere som ikke er en del av piloten. Integrasjonspunktet vil da opptre som en proxy.

Last ned eksempel for Acos, [ephorte](../resources/integrasjonspunkt-local.properties_ephorte), [P360](../resources/integrasjonspunkt-local.properties_360)