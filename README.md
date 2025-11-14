## FIXME og mulige forbedringer i etterkant
- [ ] Asciidoc mangler noen snippets (det har vært referert til kodesnippets som var SOAP basert mot gamle Altinn v2 koden?)
- [ ] AltinnPackage.java har referanse til gamle / ukjente service code / edition codes, kan det ryddes / fjernes?
- [ ] Er error responser alltid samme format, bør vi da sette request headers `Accept: application/hal+json` ?
- [ ] Fjerne `endpointUrl` fra mocket SR data, tror ikke denne finnes lenger (eksempel [974720760.json](integrasjonspunkt/src/test/resources/restmocks/identifier/974720760.json))
- [ ] Det er kode for ASIC generering i [altinn-v3-client](altinn-v3-client), kan vurderes å benytte tilsvarende funksjonalitet i [dokumentpakking](dokumentpakking)
- [ ] Make sure ["old rest template"](https://digdir.atlassian.net/browse/MOVE-2438) metrics still works with the new rest client approach
- [ ] Onboarding støtte i kode, vi har koden som skal til (se i de manuelle testene), kan f.eks. eksponeres som WEB eller API
- [ ] Undersøk om websidene som er innebygget i IP fremdeles er relevante og skal være med 

Foreløpige `eksperimentelle` endringer som testes ut (kommer / kommer ikke i endelig versjon) :
- Maven Wrapper (sikrer at alle bygger med korrekt Maven versjon)

## Bygg og kjør lokalt
Testet og bygget med OpenJDK 21.0.8 og Maven 3.9.11.

Lag egen lokale konfigurasjonsfil i roten av prosjektet med navn `integrasjonspunkt-local.properties`
(alternativt `integrasjonspunkt-local.yml` eller `integrasjonspunkt-local.yaml`).  Den vil bli inkludert
automatisk når du starter en av de forhåndsdefinerte maven-profilene.

```bash
mvn clean package
java -Dspring.profiles.active=staging -jar integrasjonspunkt/target/integrasjonspunkt.jar
```

Når man starter med `dev | staging | yt | production` profil så vil den kunne overstyres med properties fra
en lokal [integrasjonspunkt-local.properties](integrasjonspunkt-local.properties) fil.

Dette skjer automatisk siden [application-dev.properties](integrasjonspunkt/src/main/resources/config/application-dev.properties),
[application-staging.properties](integrasjonspunkt/src/main/resources/config/application-staging.properties) og
[application-production.properties](integrasjonspunkt/src/main/resources/config/application-production.properties)
inneholder en optional import av lokal konfig slik (vha `spring.config.import=optional:file:integrasjonspunkt-local.properties,optional:file:integrasjonspunkt-local.yml,optional:file:integrasjonspunkt-local.yaml`).


## Bygge REST API dokumentasjon
Tests must run for this to work (generated-snippets will be missing if you skip running tests).
For å bygge API dokumentasjon og sjekke den i lokal nettleser bruk profil `restdocs` :
```bash
mvn clean package -Prestdocs
open integrasjonspunkt/target/generated-docs/restdocs.html
```


## Linker når Integrasjonspunkt er starter lokalt
Ekstern dokumentasjon finnes her : https://docs.digdir.no/docs/eFormidling/

Hovedsiden med masse informasjon om Integrasjonspunktet :
- http://localhost:9093/

Webside der man kan kikke på og slette konversasjoner :
- http://localhost:9093/conversations

En API funksjon som er lett å teste i nettleser :
- http://localhost:9093/api/statuses

Linker til observability :
- http://localhost:9093/manage/info
- http://localhost:9093/manage/health
- http://localhost:9093/manage/health/liveness
- http://localhost:9093/manage/health/readiness
- http://localhost:9093/manage/metrics
- http://localhost:9093/manage/prometheus

Work in progress (kan dette ta over for REST API dokumentasjon, `restdoc`) :
- http://localhost:9093/swagger-ui/index.html

Linker til logger, config og alt annet :
- http://localhost:9093/manage/logfile
- `curl http://localhost:9093/manage | jq` (lister over alle observability endpoints)
- `curl http://localhost:9093/manage/configprops | jq`
- `curl http://localhost:9093/manage/env | jq` (lister over alle properties og env settings)
- `curl http://localhost:9093/manage/configprops/difi.move | jq` (kun `difi.move` konfig)

## Konfigurasjon av Integrasjonspunktet
Det ligger en [sample.properties](integrasjonspunkt-local.sample.properties) fil i dette prosjektet som vise eksempler på konfig,
for mer detaljer sjekk dokumentasjonen https://docs.digdir.no/docs/eFormidling/installasjon/installasjon


## Release
Sjå dokumentasjon for [maven-release-plugin](https://maven.apache.org/maven-release/maven-release-plugin/) og [guide for maven-release-plugin](https://maven.apache.org/guides/mini/guide-releasing.html).

> **⚠️**  Main branch er protected, release må gjøres i en egen branch med PR tilbake til main.  Dette kan forenkles ved å benytte `mvn release:branch` goal.

```bash
# lokalt repo må være i sync med origin/GitHub
git push

mvn release:prepare
# svar på tre spørsmål (sett tag lik release-versjon) 
# What is the release version for "efm-virksert"? (no.difi.meldingsutveksling:efm-virksert) 1.0: : 1.0.0
# What is SCM release tag or label for "efm-virksert"? (no.difi.meldingsutveksling:efm-virksert) 1.0.0: :
# What is the new development version for "efm-virksert"? (no.difi.meldingsutveksling:efm-virksert) 1.0.1-SNAPSHOT: :

mvn release:perform
```
