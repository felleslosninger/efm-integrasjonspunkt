# Altinn 3 client

TODO
Beskrive svakheter med kanal begrep i altinn v3 https://docs.digdir.no/docs/eFormidling/Utvikling/kanal
- får kun liste over inntil 100 meldinger
- må hente ned details for hver enkelt for å se sender reference (så potensielt 101 kall)
- dersom det er flere IP instanser med hver sin kanal, så kan vil jo 100 meldinger til kanal A blokkere for melding 101 som skal til kanal B
- så viktig at de som bruker kanal sørger for at alle kanaler faktisk tømmes
- Få innsyn i korleis DPO tjenesten var konfigurert i altinn 2 og oppdatere ressursen i altinn 3 til å samsvare med konfigurasjonen.
- få clientid for altinn 3 token inn i properties, kan man kombinere den med den eksisterende clientid?

## DPO

ManualResourceTest.java kan brukes for å konfigurere instillinger på ressursen til DPO.

## DPV

