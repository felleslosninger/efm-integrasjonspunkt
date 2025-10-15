# Altinn 3 client

Altinn 3 client er ein modul for å kommunisere med Altinn for DPV og DPO meldinger.

# TODO (marker de som er fikset)

- [x] Daniel : DPV ressurser skal hete `eformidling-dpv-administrasjon` (og `eformidling-dpv-taushetsbelagt` dersom vi trenger egen for taushetsbelagt) https://digdir.atlassian.net/browse/MOVE-4615
- [x] Proxy koden bygges og deployes i systest
- [x] Oppdatere notification med subject og sånt
- [ ] Daniel : scope mot proxy `efm:dpv` (eller `efm:dpv.read`) (ikkje bruk `altinn:broker.read`), se også [SP-602](https://digdir.atlassian.net/browse/SP-602)
- [ ] Daniel : DPO ressurser bør tilsvarende hete `eformidling-dpo` ?
- [ ] Taushetsbelagt DPV, må vi ha dedikert ressurs eller kan vi benytte den samme for taushetsbelagt og ikke taushetsbelagt? https://digdir.atlassian.net/browse/MOVE-4615
- [ ] Burde vi ha noen nye cucumber tester?
- [ ] Klar til publisering -> "Annet" status på dpv.
- [ ] Onboarding støtte i kode? https://digdir.atlassian.net/browse/MOVE-4618
- [ ] Flytte proxy koden ut i eget repo (pro / cons)
- [ ] Finne ut kva tilgangspakke vi skal ha på DPO https://digdir.atlassian.net/browse/MOVE-4614


Dokumentasjon:
- [ ] Onboarding for nye kunder
- [ ] Digdir docs
- [ ] Confluence

# DPO

- Nyttar formidling API-et til Altinn: https://docs.altinn.studio/nb/broker/
- Grov plan for implementasjon https://github.com/Altinn/altinn-authentication/issues/1394
- Sekvens diagram som viser [onboarding og bruk](altinn_dpo.md)

[ManuallyTestingBroker.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpo/ManuallyTestingBroker.java) kan brukast for å teste DPO funksjonalitet manuelt.

[ManualResourceTest.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpo/ManualResourceTest.java) kan brukes for å konfigurere instillinger på ressursen til DPO.

Slik kan vi se hvilke systemer som er registrert i Altinn for ulike organisasjoner (åpen tjeneste) :
```
# Digdir (991825827)
curl https://platform.tt02.altinn.no/authentication/api/v1/systemregister | \
grep 991825827 | grep systemId | cat

# KUL SLITEN TIGER AS (314240979)
curl https://platform.tt02.altinn.no/authentication/api/v1/systemregister | \
grep 314240979 | grep systemId | cat

# STERK ULYDIG HUND DA (311780735)
curl https://platform.tt02.altinn.no/authentication/api/v1/systemregister | \
grep 311780735 | grep systemId | cat
```

# Test-organisasjoner og maskinporten-klienter
For å teste har vi registrert klienter i maskinporten for hver sine syntetiske organiasjon.
Fremgangsmåten for å gjøre dette er da :

- Bruk [Tenor](https://testdata.skatteetaten.no/web/testnorge/) for å finne organiasjon og daglig leder.
- Bruk [BankID RA PREPROD](https://ra-preprod.bankidnorge.no/#!/search/endUser) for å aktivere BankID for daglig leder, onetimepassword: "otp"  passord: "qwer1234"
- Bruk [Forvaltningsadmin](https://forvaltningsadmin.apps.kt.digdir.cosng.net) for å opprette klientene med scopes `altinn:broker.write` og `altinn:broker.read`
- Snakk med Daniel for å få lagt til scopes som klientene trenger for å opprette system og systembruker
- - Legg til scopes `altinn:authentication/systemregister.write`, `altinn:authentication/systemuser.request.write` og `altinn:authentication/systemuser.request.read`
- - Opprett system for integrasjonspunktet for organisasjon (navnestandard `<orgnr>_integrasjonspunkt`)
- - Legg til tilgangspakke `urn:altinn:accesspackage:maskinlesbare-hendelser` (NB denne er midlertidig, ikke avklart at vi skal benytte denne, `urn:altinn:accesspackage:programmeringsgrensesnitt` eller opprette vår egen tilgangspakke) på systemet (sånn at systembrukeren kan benytte det)
- - Opprett `standard systembruker` i system `<orgnr>_integrasjonspunkt` med tilgangspakken som ble definert på systemet
- - Bruk approval url fra responsen for å logge inn i Altinn som daglig leder og godkjenne at systembrukeren får tilgangspakken
- - Verifiser at systembrukeren kan få token fra maskinporten

Daglig leder for organisasjoner kan logge inn i [Altinn TT02](https://tt02.altinn.no/) for å lese meldinger.

    
## KUL SLITEN TIGER AS (314240979)
```
Tenor : KUL SLITEN TIGER AS (314240979)
Daglig leder : KOGNITIV TANGENT (03888398847)
Maskinporten : eformidling-tenor-test-klient-01 (826acbbc-ee17-4946-af92-cf4885ebe951)
Nøkkelpar : 314240979-kul-sliten-tiger-as.jwk (kid: RyResLhzSS5p2qUq6tNla_mOkG9vU8oiYNmDEYJ9OGg, utløper 09.09.2026)
System : 314240979_integrasjonspunkt
Systembruker (standard) : 314240979_integrasjonspunkt_systembruker_test2 (10982a4d-4f9c-424a-b1bc-376dcef7316c)
Systembruker (agent) : 314240979_integrasjonspunkt_systembruker_test (5b205bec-aad7-4e3f-a504-3e84b8a778fd)
```

## STERK ULYDIG HUND DA (311780735)
```
Tenor : STERK ULYDIG HUND DA (311780735)
Daglig leder : ULYDIG LEVEREGEL (28923148371)
Maskinporten : eformidling-tenor-test-klient-02 (b590f149-d0ba-4fca-b367-bccd9e444a00)
Nøkkelpar : 311780735-sterk-ulydig-hund-da.jwk (kid: 8333ede6-60cd-450c-84b1-4c12dfb80cb6, utløper 16.09.2026)
System : 311780735_integrasjonspunkt
Systembruker (standard) : 311780735_integrasjonspunkt_systembruker_test (19bed6de-092d-4f4e-9a8c-7b95efaa400c)
```

## FRISK VOKSENDE TIGER AS (314244370)
```
Tenor : FRISK VOKSENDE TIGER AS (314244370)
Daglig leder : RØDFOTSULE KONTROLLERT (30848299746)
Kun brukt som mottaker av meldinger i Altinn, ingen tilgang til maskinporten
```

## DIGDIR (991825827)
```
Dette er en ekte org.
eformidling-meldingsteneste-test (a63cac91-3210-4c35-b961-5c7bf122345c)
```


## Begrensninger

Kallet til Altinn for å hente ut tilgjengelige filer for ein organisasjon returnerer maks 100 filer, som er dei 100 siste filene.
Dei 100 filene som blir henta uavhengig av kanalen på meldingen.


# DPV

- Nyttar melding API-et til Altinn: https://docs.altinn.studio/nb/correspondence/
- Grov plan for implementasjon https://github.com/Altinn/altinn-authentication/issues/1395
- Sekvens diagram som viser [onboarding og bruk](altinn_dpv.md)

[ManuallyTestingCorrespondence.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpv/ManuallyTestingCorrespondence.java) kan brukast for å teste DPV funksjonalitet manuelt.
