# Altinn 3 client

Altinn 3 client er ein modul for å kommunisere med Altinn for DPV og DPO meldinger.

# TODO
- [ ] Få innsyn i korleis DPO tjenesten var konfigurert i altinn 2 og oppdatere ressursen i altinn 3 til å samsvare med konfigurasjonen.
- [ ] Få innsyn i korleis DPV tjenestene var konfigurert i altinn 2 og opprett ressurser i altinn 3
- [ ] Få clientid for altinn 3 token inn i properties, kan man kombinere den med den eksisterende clientid?
- [ ] Indersøk hvordan varsling (notification) fungerte i altinn2, og sett opp notification i altinn 3 til å vere riktig basert på tidlegare funksjonalitet og ønska funksjonalitet.
- [ ] Få statestikk fra altinn https://docs.altinn.studio/nb/broker/broker-transition/getting-started/#konfigurer-ressurs-til-bruk-i-overgangsløsningen for å vurdere overgangsløsningen i produksjon
- [ ] Benyttes denne til noe?  https://platform.tt02.altinn.no/authentication/api/v1/openid/.well-known/openid-configuration

# DPO

- Nyttar formidling API-et til Altinn: https://docs.altinn.studio/nb/broker/
- Grov plan for implementasjon https://github.com/Altinn/altinn-authentication/issues/1394

[ManuallyTestingBroker.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpo/ManuallyTestingBroker.java) kan brukast for å teste DPO funksjonalitet manuelt.

[ManualResourceTest.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpo/ManualResourceTest.java) kan brukes for å konfigurere instillinger på ressursen til DPO.

Slik kan vi se hvilke systemer som er registrert i Altinn for en organisasjon :
```
# Digdir (991825827)
curl https://platform.tt02.altinn.no/authentication/api/v1/systemregister | grep 991825827 | grep systemId | cat

# KUL SLITEN TIGER AS (314240979)
curl https://platform.tt02.altinn.no/authentication/api/v1/systemregister | grep 314240979 | grep systemId | cat
```

# Test organisasjoner og maskinporten klienter
For å teste har vi registrert klienter i maskinporten for hver sine syntetiske organiasjon.
Fremgangsmåten for å gjøre dette er da :

- Bruk [Tenor](https://testdata.skatteetaten.no/web/testnorge/) for å finne organiasjon og daglig leder.
- Bruk [xxx](xxxx) for å aktivere BankID med "otp" for daglig leder
- Bruk [Forvaltningsadmin](https://forvaltningsadmin.apps.kt.digdir.cosng.net) for å opprette klientene med scopes `altinn:broker.write` og `altinn:broker.read`
- Snakk med Daniel for å få lagt til scopes som klientene trenger for å opprette system og systembruker
- - Legg til scopes `altinn:authentication/systemregister.write`, `altinn:authentication/systemuser.request.write` og `altinn:authentication/systemuser.request.read`
- - Opprett system for integrasjonspunktet for organisasjon (navnestandard `<orgnr>_integrasjonspunkt`) og tilgangspakke `xxxx`
- - Opprett `standard systembruker` i system `<orgnr>_integrasjonspunkt` med tilgangspakke `xxxx`
- - Bruk approval url fra responsen for å logge inn i Altinn som daglig leder slik at du kan godkjenne systembrukeren
- - Verifiser at systembrukeren kan få token fra maskinporten
    
## KUL SLITEN TIGER AS (314240979)
```
Tenor : KUL SLITEN TIGER AS (314240979)
Daglig leder : KOGNITIV TANGENT (03888398847)
Maskinporten : eformidling-tenor-test-klient-01 (826acbbc-ee17-4946-af92-cf4885ebe951)
Nøkkelpar : 314240979-kul-sliten-tiger-as.jwk (kid: RyResLhzSS5p2qUq6tNla_mOkG9vU8oiYNmDEYJ9OGg)
System :
Systembruker :
```

## STERK ULYDIG HUND DA (311780735)
```
Tenor : STERK ULYDIG HUND DA (311780735)
Daglig leder : ULYDIG LEVEREGEL (28923148371)
Maskinporten : eformidling-tenor-test-klient-02 (b590f149-d0ba-4fca-b367-bccd9e444a00)
Nøkkelpar : 311780735-sterk-ulydig-hund-da.jwk (kid: 8333ede6-60cd-450c-84b1-4c12dfb80cb6)
System :
Systembruker :
```

## Begrensninger

Kallet til Altinn for å hente ut tilgjengelige filer for ein organisasjon returnerer maks 100 filer, som er dei 100 siste filene.
Dei 100 filene som blir henta uavhengig av kanalen på meldingen.


# DPV

- Nyttar melding API-et til Altinn: https://docs.altinn.studio/nb/correspondence/
- Grov plan for implementasjon https://github.com/Altinn/altinn-authentication/issues/1395

[ManuallyTestingCorrespondence.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpv/ManuallyTestingCorrespondence.java) kan brukast for å teste DPV funksjonalitet manuelt.
