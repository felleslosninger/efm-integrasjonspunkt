# Kjøre Integrasjonspunktet i en isolert Docker-container

## Docker og Java

Dette imaget baserer seg på en Linux distro som heter Alpine Linux. Denne er blant de aller minste av størrelse 
og sammen med Java JRE 7 og Integrasjonspunktet, gir den en total størrelse på rundt 200 mb. Til sammenligning 
gir de vanligste Ubuntu-distroene (Trusty og Precise 64-bit) størrelser fra 800mb (uten Java) og opp til 1gb.

Difi_Integrasjonspunkt-imaget du finner her er bygd lagvis

- alpine:3.2 (minimal linux distro, https://hub.docker.com/_/alpine/)
- dervism/dockerjava:jre7 (utvidelse med Java 7: https://hub.docker.com/r/dervism/dockerjava/)
- dervism/difi_integrasjonspunkt (Integrasjonspunktet)


## Hva kan imaget brukes til

Docker-imaget gjør det enkelt å starte flere containere som kjører Integrasjonspunktet. Dette gjør det enklere å 
**integrasjonsteste** med flere endpoints både på lokal utviklingsmiljø og via Jenkins (i kombinasjon Docker-basert 
Adresse-Register og "Docker Container Linking").

For å bygge imaget sammen med Maven, må Dockerfile-filen ligge i rot-katalogen til Integrasjonspunktet. Dermed 
kan du bruke kommandoene nedenfor direkte. Dockerfile-scriptet er lagt opp slik at den automatisk identifiserer 
versjonsnummeret på jar-filen som bygges - mao, du kan bygge nye versjoner av Integrasjonspunktet og få et nytt 
Docker-image uten å endre Docker-scriptet. 

Scriptet henter automatisk jar-filen fra "target" mappen og 
provisjonerer denne i imaget. Scriptet installerer også automatisk Unlimited Security Profile i Java-installasjonen 
på imaget.

### Bruke dette imaget for å andre starte Java-applikasjoner

Alt du trenger å gjøre for å starte andre Java-applikasjoner med dette imaget, er å endre miljøvariablene i scriptet:

- **APP_PREFIX:** Her skriver du prefixet til jar-filen du vil kjøre. Eks: xyz-1.0.jar, prefix = xyz.
- **APP_MAIN_CLASS:** Navnet på den kjørbare klassen
- **APP_PROFILE:** Dersom Java-applikasjonen bruker Spring-profiles, så angir du hvilken som skal aktiveres her.

- **CMD:** Dette er kommandoen Docker kjører for å starte *din* applikasjon når all provisjonering er ferdig. I dette scriptet, 
vil denne kommandoen identifisere og starte opp en standalone Java-applikasjon. Om du ønsker å starte et shell-script, 
en database, ulike typer servere og etc kan du endre kommandoen slik du normalt sett ville starte applikasjonen: 
CMD *<kommando som starter din app her>*

## Integrasjonstesting

For integrasjonstester kan man bruke 
[Maven Docker Plugin](https://github.com/bibryam/docker-maven-plugin). Denne bruker [Docker Java Client]
(https://github.com/docker-java/docker-java) som for øvrig kan brukes uavhengig av hverandre.

Eksempel på hvordan man kan bruke Docker Java Client: https://github.com/docker-java/docker-java/wiki
Se også Spotify sin Docker-klient for Java: https://github.com/spotify/docker-client

Eksempel på hvordan man kan lage en integrasjonstest med Maven Docker Plugin står forklart her: 
http://www.javacodegeeks.com/2014/04/a-docker-maven-plugin-for-integration-testing.html

## Installere Docker

Tidligere måtte man installere Boot2Docker for å kunne bruke Docker på Mac og Windows. Denne har nå blitt depricated og 
erstattet av Docker Maskin. Last ned og installer: https://docs.docker.com/machine/install-machine/

Verifiser etter installering at det finnes en virtuell maskin med navnet "default":

```shell
$ docker-machine ls
NAME      ACTIVE   DRIVER       STATE     URL                     
default   *        virtualbox   Running   tcp://192.168.99.100:2376
```

Dersom "default" ikke finnes, kan denne installeres ved å følge guidene:

Mac: https://docs.docker.com/installation/mac/#from-your-shell
Windows: https://docs.docker.com/installation/windows/#from-your-shell

## Bygge Docker imaget

```shell
$ docker build --no-cache -t dervism/difi_integrasjonspunkt .
```

## Opprette og starte en container

```shell
$ docker run --name Difi_Integrasjonspunkt -d -p 8080:8080 dervism/difi_integrasjonspunkt
```

Beskrivelse av flagg:

- --name: Gir et navn som gjør det enklere å starte og avslutte containeren

- p, Port forwarding: -p hostPort:containerPort (dinMaskin:VirtuellMaskin)

- d, Detached mode: Kjører containeren din i en bakgrunnsprosess

## Starte flere instanser

Samme kommando som over, men med forskjellige navn og port for Docker-containeren:

```shell
$ docker run --name Difi_Integrasjonspunkt1 -d -p 8088:8080 dervism/difi_integrasjonspunkt
$ docker run --name Difi_Integrasjonspunkt2 -d -p 8089:8080 dervism/difi_integrasjonspunkt
```


## Kontrollere systeminformasjonen til containeren/image/etc

```shell
$ docker inspect dervism/difi_integrasjonspunkt
```

## Følge consol outputen fra Docker-containeren

```shell
$ docker logs -f Difi_Integrasjonspunkt
```

## Starte og stopp Docker-containeren

```shell
$ docker start Difi_Integrasjonspunkt
```

```shell
$ docker stop Difi_Integrasjonspunkt
```

## Aksessere tjenestene fra egen nettleser

Først må du finne IP-adressen til den virtuelle maskinen som kjører Docker-serveren (i dette tilfellet er det VirtualBox):

```shell
$ docker-machine ip default
192.168.99.100
```

Åpne en nettleser og gå til url'en:

http://192.168.99.100:8080/noarkExchange



