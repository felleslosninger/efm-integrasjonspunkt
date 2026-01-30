# Changelog

## Move Integrasjonspunkt v4

- Overgang til Altinn v3 for DPO og DPV
- Kotlin er konvertert til Java (kode, bygg, dependencies, test)
- For produksjon er loggene sikret med klient sertifikat
- Innebygget web-ui som bla støtter onboarding av DPO

DPV
- Størrelsesgrense for DPV er økt til 250 mb.
- Går over til Correspondence REST API fra Altinn 3 istedenfor SOAP på Altinn 2
- Correspondence API returnerer ny status "Klar for publisering" som ikkje er tatt i bruk i IP.
- Nytt Notification
- Nytt og enklere oppsett for onboarding av kunder (kunder legges til i tilgangsliste via MoveAdmin)
- "allowForwarding" på varsel er fjernet i Altinn 3
- En [Altinn Proxy](https://github.com/felleslosninger/efm-dpv-proxy) mellom Integrasjonspunktet og Altinn (håndhever tilgangskontroll mot tilgangsliste)

DPO
- Går over til Broker REST API fra Altinn 3 istedenfor SOAP på Altinn 2
- Nytt oppsett for onboarding av kunder (kunden må opprette system og systebruker i Altinn 3)
- Onboaring web-ui innebygget i Integrasjonspunktet forenkler onboarding for kunden (kan opprette system og systebruker i Altinn 3)

DPO og DPV
- Går fra service rights registry (SRR) til ressursregisteret (RRR)
  - Går fra å bruke servicecode + serviceeditioncode til ressursid.
- Går fra å bruke brukernavn og passord for tilgang til Altinn tjenestene til å benytte Maskinporten token + Altinn token
- Går fra å benytte Altinn roller til Altinn tilgangspakker
- Lagt til støtte for maskinporten token vha JWK i tillegg til sertifikat (JWK for testing av Tenor organisasjoner)
  - Siden man ikkje har virksomhetssertifikat for syntetiske testbrukere kan JWK benyttes mot maskinporten
  - Man må fortsatt benytte digdirs virksomhetssertifikat for signering av ASIC pakken.

## Changelog V2 to V3:

- Java 21+ oppgradering (some code require 17+)
- Spring Boot 3.5 oppgradering ([lots of changes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes))
- Jakarta EE migrering (from Java EE i SB 2.x)
- All jaxb/jaxws usage migrated to jakarata
- JUnit upgrade (5.x)
- Removed Spring Cloud and Eureka
- Removed Sikker Digital Post Klient
- Removed proxy-client for DPI, ref [MOVE-3688](https://digdir.atlassian.net/browse/MOVE-3688)
- Removed old / un-maintained "spring-security-oauth2" 2.5.2.RELEASE
- Merged removal of old DPI message service
- Merged removal of eFormidling 1.0 (BEST/EDU)
- Logstash default endepunkt endret til `*logs.eformidling.no:80`, ref [MOVE-1634](https://digdir.atlassian.net/browse/MOVE-1634)
