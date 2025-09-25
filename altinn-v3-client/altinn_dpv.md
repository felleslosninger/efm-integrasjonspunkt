# Slik fungerer DPV løsningen med Altinn v3
Bakgrunn og overordnet er beskrevet av Altinn i JIRA saken :
https://github.com/Altinn/altinn-authentication/issues/1395

## Onboarding av virksomhet til DPV med Integrasjonspunktet
Kunden må lage en maskinporten client (med nødvendige scopes),
deretter må Digdir legge til virksomheten i tilgangslisten
på correspondence ressursen i Altinn.

Forutsetter at nytt scope `altinn:eformidling.write` er opprette og tilgjengelig for kunden. 

```mermaid
sequenceDiagram
    actor K as Kunde
    actor D as Digdir
    participant S as Samarbeidsportalen<br><webapp>
    participant AR as Altinn ResourceRegistry<br><rest api>
    
    K->>S: Opprett maskinporten client (selvbetjent)
    note over S : Tildeler scope : altinn:eformidling.write 
    D->>AR: Legg virksomhet inn i tilgangslisten (/access-lists/{owner}/{identifier}/members) 
```

## Bruk av correspondence tjenesten
Nedenfor vises flyten for å sende en melding.
Flyten er lik enten det er en åpen eller taushetsbelagt melding.

```mermaid
sequenceDiagram
    actor K as Kunde
    participant IP as IntegrasjonsPunkt<br><applikasjon>
    participant M as Maskinporten<br><token endpoint>
    participant AT as Altinn Token Exchange<br><token endpoint>
    participant IB as Integrasjonspunkt Altinn v3 Proxy<br><rest api>
    participant AR as Altinn ResourceRegistry<br><rest api>
    participant AC as Altinn Correspondence API<br><rest api>
    
    note over AR: Mulig dette skal være PEP endpoint?
    
    K->>IP: Send melding
    IP->>M: Hent access token for client
    note over M : scopes : [altinn:eformidling.write]
    note over M : Vi støtter client autentisering med cert eller jwk
    IP-->>AT: Gjør token exchange hos Altinn (ikke påkrevet, men anbefalt)
    note over AT : Altinn token har lang levetid på (30 min)

    IP->>IB: Send melding (med kundens token)
    IB->>IB: Valider token, verfisier korrekt scope
    IB->>AR: Verfisier at kunden er i tilgangslisten
    IB-->>M: Hent token for tilgang correspondence resource
    note over IB: scopes : [altinn:correspondence.read, altinn:correspondence.write]
    IB-->>AT: Gjør token exchange hos Altinn (ikke påkrevet, men anbefalt)
    note over AT : Altinn token har lang levetid på (30 min)
    IB->>AC: Send melding med corresponence api
```

