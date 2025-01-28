# Move Integrasjonspunkt v3

Dette blir en ny versjon med Java 21+ og SpringBoot 3+

- Java 21 upgrade (some code requires 17+)
- Spring Boot 3.4.1 upgrade (lots of changes)
- Jakarta EE migrering (from SB 2.x)
- JUnit upgrade
- All jaxb/jaxws usage migrated to jakarata
- Removed Spring Cloud
- Removed Sikker Digital Post Klient
- Removed old / un-maintained "spring-security-oauth2" 2.5.2.RELEASE
- Removed Kotlin (fremdeles noe som gjenstår, FIXME)
- Merged removal of old DPI message service
- Merged removal of eFormidling 1.0 (BEST/EDU)


## Linker når man starter lokalt

Dokumentasjon her : https://docs.digdir.no/docs/eFormidling/

Webside der man kan kikke på og slette konversasjoner
- http://localhost:9093/conversations

Linker til observability
- http://localhost:9093/manage/info
- http://localhost:9093/manage/health
- http://localhost:9093/manage/health/liveness
- http://localhost:9093/manage/health/readiness
- http://localhost:9093/manage/metrics
- http://localhost:9093/manage/prometheus


## Konfigurasjon av Integrasjonspunktet
FIXME her beskriver vi alle mulige konfigurasjonsparameter og hva som er default.

Det er en [sample.properties](integrasjonspunkt-local.sample.properties) fil som er ment å vise eksempler på
alt av konfig, men den virker ikke å være 100% oppdatert.

| Innstilling | Standardverdi | Beskrivelse |
|-------------|---------------|-------------|
| test.all    | false         | om alt skal testes eller ei |
| test.all    | false         | om alt skal testes eller ei |
| test.all    | false         | om alt skal testes eller ei |
| test.all    | false         | om alt skal testes eller ei |
| test.all    | false         | om alt skal testes eller ei |
| test.all    | false         | om alt skal testes eller ei |