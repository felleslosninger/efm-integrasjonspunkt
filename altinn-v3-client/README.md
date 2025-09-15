# Altinn 3 client

Altinn 3 client er ein modul for å kommunisere med Altinn for DPV og DPO meldinger.

# TODO
- [ ] Få innsyn i korleis DPO tjenesten var konfigurert i altinn 2 og oppdatere ressursen i altinn 3 til å samsvare med konfigurasjonen.
- [ ] Få innsyn i korleis DPV tjenestene var konfigurert i altinn 2 og opprett ressurser i altinn 3
- [ ] Få clientid for altinn 3 token inn i properties, kan man kombinere den med den eksisterende clientid?
- [ ] Indersøk hvordan varsling (notification) fungerte i altinn2, og sett opp notification i altinn 3 til å vere riktig basert på tidlegare funksjonalitet og ønska funksjonalitet.
- [ ] Få statestikk fra altinn https://docs.altinn.studio/nb/broker/broker-transition/getting-started/#konfigurer-ressurs-til-bruk-i-overgangsløsningen for å vurdere overgangsløsningen i produksjon

## DPO

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

Test klienter i maskinporten for DPO
```
KUL SLITEN TIGER AS (314240979)
eformidling-tenor-test-klient-01 (826acbbc-ee17-4946-af92-cf4885ebe951)
```

### Begrensninger

Kallet til Altinn for å hente ut tilgjengelige filer for ein organisasjon returnerer maks 100 filer, som er dei 100 siste filene.
Dei 100 filene som blir henta uavhengig av kanalen på meldingen.


## DPV

- Nyttar melding API-et til Altinn: https://docs.altinn.studio/nb/correspondence/
- Grov plan for implementasjon https://github.com/Altinn/altinn-authentication/issues/1395

[ManuallyTestingCorrespondence.java](src/test/java/no/difi/meldingsutveksling/altinnv3/dpv/ManuallyTestingCorrespondence.java) kan brukast for å teste DPV funksjonalitet manuelt.
