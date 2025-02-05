# Move Integrasjonspunkt v3

Dette blir en ny versjon med Java 21+ og SpringBoot 3+

- Java 21+ oppgradering (some code require 17+)
- Spring Boot 3.4 oppgradering ([lots of changes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes))
- Jakarta EE migrering (from Java EE i SB 2.x)
- All jaxb/jaxws usage migrated to jakarata
- JUnit upgrade (5.x)
- Removed Spring Cloud and Eureka
- Removed Sikker Digital Post Klient
- Removed old / un-maintained "spring-security-oauth2" 2.5.2.RELEASE
- Merged removal of old DPI message service
- Merged removal of eFormidling 1.0 (BEST/EDU)
- Started to remove Kotlin (fremdeles endel som gjenstår og som må skrives om, FIXME)

## Bygg og kjøre lokalt 
Testet og bygget med OpenJDK 21.0.5 og Maven 3.9.9.

```bash
mvn clean package
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
Dokumentasjon her : https://docs.digdir.no/docs/eFormidling/

Webside der man kan kikke på og slette konversasjoner :
- http://localhost:9093/conversations
- [http://localhost:9093/viewreceipts 🚨 ?? skal fjernes ?? 🚨](http://localhost:9093/viewreceipts)

En API funksjon som er lett å teste i nettleser :
- http://localhost:9093/api/statuses

Linker til observability 
- http://localhost:9093/manage/info
- http://localhost:9093/manage/health
- http://localhost:9093/manage/health/liveness
- http://localhost:9093/manage/health/readiness
- http://localhost:9093/manage/metrics
- http://localhost:9093/manage/prometheus


## Konfigurasjon av Integrasjonspunktet
Det ligger en [sample.properties](integrasjonspunkt-local.sample.properties) fil i dette prosjektet som vise eksempler på
konfig, for mer detaljer sjekk dokumentasjonen https://docs.digdir.no/docs/eFormidling/installasjon/installasjon
