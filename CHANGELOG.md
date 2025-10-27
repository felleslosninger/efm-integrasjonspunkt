# Changelog

## Move Integrasjonspunkt v4

- Kotlin er konvertert til Java (kode, bygg, dependendies, test)

### Altinn 3

DPV

- Størrelsesgrense for DPV er økt til 250 mb.
- Går over til Correspondence api fra Altinn 3 istedenfor soap Altinn 2
- Correspondence Api returnerer ny status "Klar for publisering" som ikkje er tatt i bruk i IP.
- Nytt Notification
- Nytt oppsett for onboarding av kunder.

DPO

- Går over til Broker api fra Altinn 3 istedenfor soap Altinn 2
- Nytt oppsett for onboarding av kunder.

DPO og DPV

- Går fra service rights registry (SRR) til ressursregisteret (RRR)
  - Går fra å bruke servicecode + serviceeditioncode til ressursid.
- Går fra å bruke brukernavn og passord for tilgang til Altinn tjenestene til å benytte Maskinporten token + Altinn token
- Går fra å benytte Altinn roller til Altinn tilgangspakker
- Lagt til støtte for jwk tokens istedenfor sertifikat. Dette er kun brukt for testing, 
siden man ikkje har virksomhetssertifikat for syntetiske testbrukere og det kreves for token fra maskinporten, 
man må fortsatt benytte digdirs virksomhetssertifikat for signering av asic pakken.
