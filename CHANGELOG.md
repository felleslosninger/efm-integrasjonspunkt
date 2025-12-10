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
