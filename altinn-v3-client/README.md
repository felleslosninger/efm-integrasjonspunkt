# Altinn 3 client

Altinn 3 client er ein modul for å kommunisere med Altinn for DPV og DPO meldinger.

# TODO
- Få innsyn i korleis DPO tjenesten var konfigurert i altinn 2 og oppdatere ressursen i altinn 3 til å samsvare med konfigurasjonen.
- Få innsyn i korleis DPV tjenestene var konfigurert i altinn 2 og opprett ressurser i altinn 3
- få clientid for altinn 3 token inn i properties, kan man kombinere den med den eksisterende clientid?
- undersøk hvordan varsling (notification) fungerte i altinn2, og sett opp notification i altinn 3 til å vere riktig basert på tidlegare funksjonalitet og ønska funksjonalitet.
- få statestikk fra altinn https://docs.altinn.studio/nb/broker/broker-transition/getting-started/#konfigurer-ressurs-til-bruk-i-overgangsløsningen for å vurdere overgangsløsningen i produksjon
- cucumber testene
- 

## DPO

Nyttar formidling API-et til Altinn: https://docs.altinn.studio/nb/broker/

ManualResourceTest.java kan brukes for å konfigurere instillinger på ressursen til DPO.

### Begrensninger

Kallet til Altinn for å hente ut tilgjengelige filer for ein organisasjon returnerer maks 100 filer, som er dei 100 siste filene.
Dei 100 filene som blir henta blir henta uavhengig av kanalen på meldingen.

## DPV

Nyttar melding API-et til Altinn: https://docs.altinn.studio/nb/correspondence/
