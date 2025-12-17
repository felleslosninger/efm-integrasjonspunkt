## FIXME og mulige forbedringer i etterkant
- [ ] AltinnPackage.java har referanse til gamle / ukjente service code / edition codes, kan det ryddes / fjernes?
- [ ] Er error responser alltid samme format, bør vi da sette request headers `Accept: application/hal+json` ?
- [ ] Fjerne `endpointUrl` fra mocket SR data, tror ikke denne finnes lenger (eksempel [974720760.json](integrasjonspunkt/src/test/resources/restmocks/identifier/974720760.json))
- [ ] Det er kode for ASIC generering i [altinn-v3-client](altinn-v3-client), kan vurderes å benytte tilsvarende funksjonalitet i [dokumentpakking](dokumentpakking)
- [ ] Make sure ["old rest template"](https://digdir.atlassian.net/browse/MOVE-2438) metrics still works with the new rest client approach
- [ ] Onboarding støtte i kode, vi har koden som skal til (se i de manuelle testene), kan f.eks. eksponeres som WEB eller API
- [ ] Undersøk om websidene som er innebygget i IP fremdeles er relevante og skal være med
- [ ] Fjerne bruk av docker-file og gå over bruke mvn spring boot build image: [jira](https://digdir.atlassian.net/jira/software/c/projects/MOVE/boards/32?assignee=5f2cfe00ef11df0025e5cd23&selectedIssue=MOVE-4422)

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


## Utvikle nye web sider
Prosjektet inneholder en `web` modul, som inneholder web sider for å administrere Integrasjonspunktet.
Denne modulen inneholder en kjørbar klasse og kan startes separat uten resten av Integrasjonspunktet
og alle dets avhengigheter.

Dette er by-design, slik at det skal være mulig å raskt utvikle websiden med hot-reload aktivert.
For informasjon om hvordan dette fungerer kan du se [web/README.md](web/README.md).

Kortversjon er at web-modulen med `fake` backend og hot-reload kan startes slik :
```bash
cd web
mvn spring-boot:run -Dspring-boot.run.profiles=reload
open http://localhost:8080/
```

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

## Release

Release av ny versjon gjerast via GitHub GUI
- Gå til "Releases" i GitHub repo
- Klikk på "Draft a new release"
- Velg tag (ny eller eksisterande)
- Fyll inn tittel og beskrivelse
- Klikk på "Publish release"
