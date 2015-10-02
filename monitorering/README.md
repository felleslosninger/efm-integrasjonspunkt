# Integrasjonspunkt Monitorering

Dette prosjektet gjør det mulig å monitorere Integrasjonspunktet ved å eksportere metrikker til en ekstern Eureka-server.
Monitoreringen er en kombinasjon av Spring Actuators (din app) -> Spring Boot Admin (monitor ui) <-> Eureka (tjeneste register).

# Quick start Q&A (for brukere av Integrasjonspunktet)

Denne beskrivelsen er ment for virksomheter som tar i bruk Integrasjonspunktet! Utviklere kan følge [Setup](#setup)

### Hva må jeg gjøre for at mitt Integrasjonspunkt skal kunne overvåkes sentralt av DIFI

Dersom den ikke finnes allerede, opprett filen "integrasjonspunkt-local.properties" i samme mappe der Integrasjonspunktet kjøres og legg til
følgende to linjer:

```shell
spring.boot.admin.url=<DIFI-monitor server ip-adresse:port>
spring.boot.admin.client.serviceUrl=<Din server ip-adresse:port>
```

Hva innstillingene betyr:

- **spring.boot.admin.url**: URL eller IP-adressen (med riktig port) til DIFI-serveren   
- **spring.boot.admin.client.serviceUrl**: URL eller IP-adressen til din server som kjører Integrasjonspunktet

**Hvordan vet jeg at overvåkningen funker?**

Dette kan du dobbelsjekke i "application.log". Denne loggfilen finner du i samme mappe der Integrasjonspunktet kjører. I loggen, dersom 
alt funker som det skal, vil du finne en loggsetning lignende denne:

*Application registered itself as Application [id=5a795541, name=Integrasjonspunkt DIFI*

### Kan jeg velge hvilket navn Integrasjonspunktet mitt skal registreres i overvåkningen?

Ja. Dersom din virksomhet eksempelvis heter "Virksomhet XY" legger du til følgende innstilling i integrasjonspunkt-local.properties:

```shell
spring.boot.admin.client.name=Virksomhet XY
```

DIFI vil dermed kunne se applikasjonen din slik:

![bilde](https://github.com/difi/meldingsutveksling-mellom-offentlige-virksomheter/blob/master/monitorering/egendefinert_navn.png "Egendefinert navn")

<a name="setup">
## Setup (uten Docker)

For å starte monitorering manuelt og få tilgang til administrasjonsgrensesnittet, gjør du følgende i angitt rekkefølge:

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

3) Opprett en fil som heter integrasjonspunkt-local.properties og legg til informasjon om din virksomhet:

```shell
orgnumber=<virksomhetens organisasjonsnummer>
spring.boot.admin.client.name=<virksomhetens navn>
```

4) Dersom du tester på lokalt utviklingsmiljø, kan du starte en instans av Integrasjonspunktet slik:

```shell
$ java -jar -Dprivatekeyalias=974720760 -Dkeystorelocation=src/main/resources/test-certificates.jks -Dprivatekeypassword=changeit 
target/integrasjonspunkt-1.0-SNAPSHOT.jar no.difi.meldingsutveksling.IntegrasjonspunktApplication --spring.profiles.active=dev
```

## Setup - med Docker

1) Bruk de medfølgende bygg-scriptene i dette repoet for å bygge og kjøre monitorering via Docker:

```shell
$ ./start-monitor.sh
```

Bygg-scriptet starter både Eureka og Admin UI med linking automatisk. 

2) Deretter starter du Integrasjonspunktet sammen med et par ekstra
parametere. Parameterne er port-nummer, org-nummer, applikasjonens ip og port, monitor sin ip og port og siste er et egendefinert navn.

```shell
./build-docker.sh 8098 12345678 http://192.168.99.100:8098 http://192.168.99.100:8090 "DIFI NOR"
```

NB: build-docker.sh finner du i mappen "integrasjonspunkt" i dette repository

3) Dersom du nå sjekker applikasjonsloggen (med kommandoen "docker logs -f Difi_Integrasjonspunkt_8098"), vil du se følgende linje.


```shell
Application registered itself as Application [id=9d31afee, name=DIFI NOR, managementUrl=http://192.168.99.100:8098, healthUrl=http://192.168.99.100:8098/health, serviceUrl=http://192.168.99.100:8098
```

4) Resultat er dermed følgende i monitoreringsverktøyet:

![bilde](https://github.com/difi/meldingsutveksling-mellom-offentlige-virksomheter/blob/master/monitorering/docker.png "Docker-monitor")


## Screenshots

![bilde](https://github.com/difi/meldingsutveksling-mellom-offentlige-virksomheter/blob/master/monitorering/metrics1.png "Toppmeny med statusinfo")

![bilde](https://github.com/difi/meldingsutveksling-mellom-offentlige-virksomheter/blob/master/monitorering/metrics2.png "Grafer og metrikker")