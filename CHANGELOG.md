# Changelog

## Move Integrasjonspunkt v4

- Kotlin er konvertert til Java (kode, bygg, dependendies, test)
- For produksjon er loggene sikret med klient sertifikat

### Altinn 3

DPV

- Størrelsesgrense for DPV er økt til 250 mb.
- Går over til Correspondence api fra Altinn 3 istedenfor soap Altinn 2
- Correspondence Api returnerer ny status "Klar for publisering" som ikkje er tatt i bruk i IP.
- Nytt Notification
- Nytt oppsett for onboarding av kunder.
- "allowForwarding" på varsel er fjernet i Altinn 3

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

Fixme & todo
- [x] Enable `enableLogstash` as default for prod and staging again ([see v2 bootstrap config](https://github.com/felleslosninger/efm-integrasjonspunkt/blob/main/integrasjonspunkt/src/main/resources/config/bootstrap.yml))
- [x] Bytte usikret logstash fra `*stream-meldingsutveksling.difi.no:443` til `*logs.eformidling.no:80`
- [x] Støtte for secure logstash på `*logs.eformidling.no:443`
- [ ] `management.endpoints.enabled-by-default` is deprecated (but still used in some property files)
- [ ] Started to remove Kotlin (fremdeles endel som gjenstår, men dette må skrives om)
- [ ] Search for StatisticsrepositoryNoOperations "usage" (non-existing service loader reference)
- [ ] Det er noen eldre `TODO` kommentarer som har vært med i flere år og som kanskje bare kan fjernes?
- [ ] Dokumentere hvilke applikasjons-spesifikke metrics vi har lagt til (see `@Timed` og `MetricsRestClientInterceptor`)
- [ ] Make sure ["old rest template"](https://digdir.atlassian.net/browse/MOVE-2438) metrics still works with the new rest client approach
- [ ] Undersøk om websidene som er innebygget i IP fremdeles er relevante og skal være med (`viewreceipts` ser f.eks. ikke ut til å ha noen funksjon)

Foreløpige `eksperimentelle` endringer som testes ut (kommer / kommer ikke i endelig versjon) :
- Maven Wrapper (sikrer at alle bygger med korrekt Maven versjon)
- Swagger-UI (http://localhost:9093/swagger-ui/index.html)
