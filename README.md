# Move Integrasjonspunkt v4

- Spring Boot 3.5.x og Java 21
- Oppgradert til Altinn v3 REST (Fjernet altinnexchange / altinnclient soap)
- DPO - fjernet brukernavn og passord fra konfigurasjon (det er nå maskinporten)
- DPV - endret upload size limit til `difi.move.dpv.upload-size-limit=250MB`

## FIXME og mulige forbedringer i etterkant
- [ ] Fjerne `endpointUrl` fra mocket SR data, tror ikke denne finnes lenger (eksempel [974720760.json](integrasjonspunkt/src/test/resources/restmocks/identifier/974720760.json))
- [ ] Vurdere å bytte til `/broker/api/v1/filetransfer/{fileTransferId}` som også inneholder sendersReference (fra `/broker/api/v1/filetransfer/{fileTransferId}/details` som inneholder ganske mye mer)  
- [ ] Rydde config filer, fjerne username/password fra DPO (se også test properties [application.properties](altinn-v3-client/src/test/resources/application.properties))
- [ ] AltinnInSteps.java, Cucumber koden som testet SOAP er kommentert ut - må fikses for REST
- [ ] Det er kode for ASIC generering i [altinn-v3-client](altinn-v3-client), kan vurderes å benytte tilsvarende funksjonalitet i [dokumentpakking](dokumentpakking)

## Bygg og kjøre lokalt 
Testet og bygget med OpenJDK 21.0.6 og Maven 3.9.9.

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
