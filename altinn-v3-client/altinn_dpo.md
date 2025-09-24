# Slik fungerer DPO løsningen med Altinn v3 
Bakgrunn og overordnet er beskrevet av Altinn i JIRA saken :
https://github.com/Altinn/altinn-authentication/issues/1394

## Onboarding av virksomhet til DPO med Integrasjonspunktet
Kunden må lage en maskinporten client (med nødvendige scopes),
registrere ett system for sitt integrasjonspunkt og opprette
en systemuser som kan benyttes av integrasjonspunktet for å
sende og motta filer via Altinn Broker API.
```mermaid
sequenceDiagram
    actor K as Kunde
    actor D as Daniel
    participant S as Samarbeidsportalen<br><webapp>
    participant M as Maskinporten<br><token endpoint>
    participant AS as Altinn SystemRegister<br><rest api>
    participant A as Altinn<br><webapp>
    
    K->>S: Opprett maskinporten client (selvbetjent)
    note over S : Tildeler scopes for : broker read + write
    D->>S: Daniel tildeler scopes (de som ikke er åpne for kunde) 
    note over S : Tildeler scopes for : system + systembruker
    
    K->>M: Hent token for client
    note over M : scopes : [altinn:authentication/systemregister.write]
    K->>AS : Opprett system (/systemregister/vendor)
    note over AS: systemId = <orgno>_integrasjonspunkt
    K->>AS : Registrer tilgangspakke på system (/systemregister/vendor/{id}/accesspackages)
    note over AS: tilgangspakke = urn:altinn:accesspackage:???
    
    K->>M: Hent token for client
    note over M : scopes : [systemuser.request.write,<br>systemuser.request.read]

    K->>AS: Opprett "standard" systemuser forespørsel (/systemuser/request/vendor)
    note over AS : externalRef = <systemId>_systembruker_<name>
    note over AS : tilgangspakke = urn:altinn:accesspackage:???
    AS-->>K: Retur av URL for å godkjenne opprettelse av systembruker på vegne av virksomheten
    note over K : URL kan presenteres kunde for rask godkjenning,<br>varsel blir sendt til daglig leder i Altinn
    
    K->>A: Bruk url eller logg inn i Altinn for bekrefte opprettelse av systemuser
```

## Bruk av systemuser for å sende og motta filer via broker tjenesten
Broker tjenesten kan sende og motta filer.  For å benytte den må man ha en systemuser
med korrekt tilgangspakke.  Det er en policy på Digdir's broker ressurs som begrenser
bruken til systemusers med rett tilganspakke.

Henting av filer er basert på polling, det er ikke støtte for webhook/notification
i denne versjonen (selv om det finnes Altinn API'er for dette).
```mermaid
sequenceDiagram
    actor K as Kunde
    participant IP as IntegrasjonsPunkt<br><applikasjon>
    participant M as Maskinporten<br><token endpoint>
    participant AT as Altinn Token Exchange<br><token endpoint>
    participant AB as Altinn Broker API<br><rest api>
    
    K->>IP: Sende fil
    IP->>M: Hent access token for systemuser<br>(client + authorization_details claim)
    note over M : Vi støtter client autentisering med cert eller jwk<br>Maskinporten token har kort levetid (2 min)
    note over M : Scope 'authorization_details' kreves, beskriver systembruker 
    note over M : scopes : [altinn:authentication/systemuser.request.read,<br>altinn:authentication/systemuser.request.write]

IP-->>AT: Gjør token exchange hos Altinn (ikke påkrevet, men anbefalt)
    note over AT : Altinn token har lang levetid på (30 min)
    IP-->>AB: Sende filer med systemuser token
    IP-->>AB: Hente filer med systemuser token
```
