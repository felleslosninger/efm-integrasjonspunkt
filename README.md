## Bygg og kjør lokalt
Testet og bygget med OpenJDK 21.0.9 og Maven 3.9.12.

Lag egen lokale konfigurasjonsfil i roten av prosjektet med navn `integrasjonspunkt-local.properties`
(alternativt `integrasjonspunkt-local.yml` eller `integrasjonspunkt-local.yaml`).  Den vil bli inkludert
automatisk når du starter en av de forhåndsdefinerte maven-profilene.

```bash
mvn clean package
java -Dspring.profiles.active=staging -jar integrasjonspunkt/target/integrasjonspunkt.jar
```

Når man starter med `dev | staging | yt | production` profil så kan properties overstyres fra
en lokal [integrasjonspunkt-local.properties](integrasjonspunkt-local.properties) fil.

Dette skjer automatisk siden [application-dev.properties](integrasjonspunkt/src/main/resources/config/application-dev.properties),
[application-staging.properties](integrasjonspunkt/src/main/resources/config/application-staging.properties) og
[application-production.properties](integrasjonspunkt/src/main/resources/config/application-production.properties)
inneholder en `optional` import av lokal konfig slik (vha `spring.config.import=optional:file:integrasjonspunkt-local.properties,optional:file:integrasjonspunkt-local.yml,optional:file:integrasjonspunkt-local.yaml`).


## Bygge dockerbilde lokalt:
Stå i roten av prosjektet og kjør kommandoene:
```bash
mvn clean install
mvn spring-boot:build-image --file integrasjonspunkt/pom.xml -Dspring-boot.build-image.imageName=NAME:TAG -Dspring-boot.build-image.builder=paketobuildpacks/builder-jammy-tiny
```


## Kjøre dockerbilde bygget lokalt eller lastet ned fra [GitHub Container Registry](https://github.com/felleslosninger/efm-integrasjonspunkt/pkgs/container/efm-integrasjonspunkt)
- Docker-bildet er bygget med maven spring boot plugin, og bruker paketo-base-tiny som builder. Dette er et sterkt herdet base-bilde som blir vedlikeholdt av Paketo Buildpacks. 
- Se docker-compose-TEMPLATE.yaml for hvordan starte opp lokalt med docker compose, ekstern ActiveMQ og Postgres, MariaDB, MYSQL og MSSQL.
- Man _må_ bruke ekstern activemq ved bruk av dockerimage
- Mount opp certs mot /workspace-mappen
- Mount opp logger mot /workspace/integrasjonspunkt-logs


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

## Release (for interne)

Release av ny versjon gjerast via GitHub GUI
- Gå til "Releases" i GitHub repo
- Klikk på "Draft a new release"
- Velg tag (ny eller eksisterande)
- Fyll inn tittel og beskrivelse
- Klikk på "Publish release"
