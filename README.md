# Move Integrasjonspunkt v4

- Spring Boot 3.5.x og Java 21
- Oppgradert til Altinn v3 REST (Fjernet altinnexchange / altinnclient soap)
- DPO - fjernet brukernavn og passord fra konfigurasjon (det er nå maskinporten)
- DPV - endret upload size limit til `difi.move.dpv.upload-size-limit=250MB`

## FIXME og mulige forbedringer i etterkant
- [x] AltinnInSteps.java, Cucumber koden som testet SOAP er kommentert ut - må fikses for REST
- [x] OIDC for DPO må kunne angis separat (default kan være at den kopierer oidc settings fra "rot")
- [x] OIDC for DPV må kunne angis separat (default kan være at den kopierer oidc settings fra "rot")
- [x] OIDC settings for maskinporten (scope, clientid mm) må kunne overstyres for hver av tjenestene DPI, DPO og DPV
- [x] Burde vi gå for Java 25 LTS (vi gjør ikke dette nå - det er allerede kommunisert ut at IPv3/v4 vil bruke Java 21 LTS)
- [ ] Efm-common er oppdatert i v3 branch
- [ ] Vi har støtte for lokal konfig i yml/yaml format i v3, skal vi videreføre dette (søk etter `spring.config.import=` i v3)
- [ ] Sett cache levetid for altinn access token (mulig den er forskjellig levetid i de ulike miljøene)
- [ ] Er error responser alltid samme format, bør vi da sette request headers `Accept: application/hal+json` ?
- [ ] Fjerne `endpointUrl` fra mocket SR data, tror ikke denne finnes lenger (eksempel [974720760.json](integrasjonspunkt/src/test/resources/restmocks/identifier/974720760.json))
- [ ] Vurdere å bytte til `/broker/api/v1/filetransfer/{fileTransferId}` som også inneholder sendersReference (fra `/broker/api/v1/filetransfer/{fileTransferId}/details` som inneholder ganske mye mer)  
- [ ] Rydde config filer, fjerne username/password fra DPO (se også test properties [application.properties](altinn-v3-client/src/test/resources/application.properties))
- [ ] Asciidoc mangler noen snippets (det har vært referert til kodesnippets som var SOAP basert mot gamle Altinn v2 koden)
- [ ] Det er kode for ASIC generering i [altinn-v3-client](altinn-v3-client), kan vurderes å benytte tilsvarende funksjonalitet i [dokumentpakking](dokumentpakking)
- [ ] Sette log endepunkter til secure 443 som default for QA og PROD, ingen elastic logging uten security i v4
- [ ] Dokumentere hvilke applikasjons-spesifikke metrics vi har lagt til (see `@Timed` og `MetricsRestClientInterceptor`)
- [ ] Make sure ["old rest template"](https://digdir.atlassian.net/browse/MOVE-2438) metrics still works with the new rest client approach
- [ ] Undersøk om websidene som er innebygget i IP fremdeles er relevante og skal være med (`viewreceipts` ser f.eks. ikke ut til å ha noen funksjon)

Foreløpige `eksperimentelle` endringer som testes ut (kommer / kommer ikke i endelig versjon) :
- Maven Wrapper (sikrer at alle bygger med korrekt Maven versjon)
- 
## Bygg og kjør lokalt 
Testet og bygget med OpenJDK 21.0.8 og Maven 3.9.10.

Lag egen lokale konfigurasjonsfil i roten av prosjektet med navn `integrasjonspunkt-local.properties`
(alternativt `integrasjonspunkt-local.yml` eller `integrasjonspunkt-local.yaml`).  Den vil bli inkludert
automatisk når du starter en av de forhåndsdefinerte maven-profilene.

```bash
mvn clean package
java -Dspring.profiles.active=staging -jar integrasjonspunkt/target/integrasjonspunkt.jar
```

Når man starter med `dev | staging | yt | production` profil så vil den kunne overstyres med properties fra
en lokale [integrasjonspunkt-local.properties](integrasjonspunkt-local.properties) fil.

Dette skjer automatisk siden [application-dev.properties](integrasjonspunkt/src/main/resources/config/application-dev.properties),
[application-staging.properties](integrasjonspunkt/src/main/resources/config/application-staging.properties), 
[application-yt.properties](integrasjonspunkt/src/main/resources/config/application-yt.properties) og
[application-production.properties](integrasjonspunkt/src/main/resources/config/application-production.properties)
inneholder en optional import av eksterne properties fra den file (vha `spring.config.import=optional:file:integrasjonspunkt-local.properties`).

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
- `curl http://localhost:9093/manage/env | jq` (lister over alle properties og env settings)
- `curl http://localhost:9093/manage/configprops/difi.move | jq` (kun `difi.move` konfig)

## Konfigurasjon av Integrasjonspunktet
Det ligger en [sample.properties](integrasjonspunkt-local.sample.properties) fil i dette prosjektet som vise eksempler på konfig,
for mer detaljer sjekk dokumentasjonen https://docs.digdir.no/docs/eFormidling/installasjon/installasjon
