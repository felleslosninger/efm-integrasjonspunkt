# Integrasjonspunkt Monitorering

Dette prosjektet gjør det mulig å monitorere Integrasjonspunktet ved å eksportere metrikker til en ekstern Eureka-server.
Monitoreringen er en kombinasjon av Spring Actuators (din app) -> Spring Boot Admin (monitor ui) <-> Eureka (tjeneste register).

## Setup

For å starte og få tilgang til monitoreringsgrensesnittet, gjør du følgende i angitt rekkefølge:

1) Start Spring Eureka Server. Eureka har en egen UI som du finner her: http://localhost:8761/

```shell
$ java -jar eureka-1.0.jar
```

2) Start Spring Boot Admin UI. Monitor UI finner du her: http://localhost:8090/

```shell
$ java -jar integrasjonspunkt-monitor-1.0.jar
```

Neste gang en instans av Integrasjonspunktet starter, vil du kunne se dette i grensesnittet.

### Lokalt utviklingsmiljø

3) Dersom du tester på lokalt utviklingsmiljø, kan du starte en instans av Integrasjonspunktet slik:

```shell
java -jar -Dprivatekeyalias=974720760 -Dprivatekeyloacation=test-certificates.jks -Dprivatekeypassword=changeit 
target/integrasjonspunkt-1.0-SNAPSHOT.jar no.difi.meldingsutveksling.IntegrasjonspunktApplication --spring.profiles.active=dev
```