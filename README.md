# Move Integrasjonspunkt v3

Dette blir en versjon med Java 21+ og SpringBoot 3.5+

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

## Bygg og kjør lokalt 
Testet og bygget med OpenJDK 21.0.6 og Maven 3.9.9.

Lag egen lokale konfigurasjonsfil i roten av prosjektet med navn `integrasjonspunkt-local.properties`
(alternativt `integrasjonspunkt-local.yml` eller `integrasjonspunkt-local.yaml`).  Den vil bli inkludert
automatisk når du starter en av de forhåndsdefinerte maven-profilene (som `staging`, `dev`, `prod`).

```bash
mvn clean package

# start med staging profil (som også leser fra din lokale konfigurasjonsfil) :
java -Dspring.profiles.active=staging -jar integrasjonspunkt/target/integrasjonspunkt.jar
```

For å bygge API dokumentasjon samtidig og sjekke den i lokal nettleser bruk profil `restdocs` :
```bash
mvn clean package -Prestdocs
open integrasjonspunkt/target/generated-docs/restdocs.html
```

For å bygge, kjøre dokka og signere med gpg bruk profil `ossrh` :
```bash
mvn clean package -Possrh
```

## Linker når Integrasjonspunkt er starter lokalt
Ekstern dokumentasjon finnes her : https://docs.digdir.no/docs/eFormidling/

Webside der man kan kikke på og slette konversasjoner :
- http://localhost:9093/conversations
- http://localhost:9093/viewreceipts  🚨 Ikke i bruk / kan fjernes ? 🚨

En API funksjon som er lett å teste i nettleser :
- http://localhost:9093/api/statuses

Linker til observability :
  http://localhost:9093/manage/info
- http://localhost:9093/manage/health
- http://localhost:9093/manage/health/liveness
- http://localhost:9093/manage/health/readiness
- http://localhost:9093/manage/metrics
- http://localhost:9093/manage/prometheus

Linker til logger, config og alt annet :
- http://localhost:9093/manage/logfile
- `curl http://localhost:9093/manage | jq` (lister over alle observability endpoints)
- `curl http://localhost:9093/manage/configprops | jq`
- `curl http://localhost:9093/manage/configprops/difi.move | jq` (kun `difi.move` konfig)

## Konfigurasjon av Integrasjonspunktet
Det ligger en [sample.properties](integrasjonspunkt-local.sample.properties) fil i dette prosjektet som vise eksempler på konfig,
for mer detaljer sjekk dokumentasjonen https://docs.digdir.no/docs/eFormidling/installasjon/installasjon
