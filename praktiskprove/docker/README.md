# Kjøre Integrasjonspunktet i en isolert Docker-container

### Innhold
---

+ [Docker og Java](#about)
+ [Hva kan imaget brukes til](#bruk)
+ [Bruke dette imaget for å andre starte Java-applikasjoner](#andrejavaapps)
+ [Integrasjonstesting](#integrasjonstest)
+ [Installere Docker](#installeredocker)
+ [Bygge Docker-image for Integrasjonspunktet](#byggeimage)
+ [Opprette og starte en container](#opprettecontainer)
+ [Aksessere tjenestene fra egen nettleser](#nettleseraksess)
+ [Aksessere shellet / terminalen til linux distroen](#shelltilgang)
+ [Starte flere instanser](#flereinstanser)
+ [Kontrollere system-informasjonen til Docker-containeren](#inspect)
+ [Følge consol outputen fra Docker-containeren](#logging)
+ [Starte og stopp Docker-containeren](#startstopp)

---

<a name="about">
## Docker og Java

Dette imaget baserer seg på en Linux distro som heter Alpine Linux. Denne er blant de aller minste av størrelse 
og sammen med Java JRE 7 og Integrasjonspunktet, gir den en total størrelse på rundt 200 mb. Til sammenligning 
gir de vanligste Ubuntu-distroene (Trusty og Precise 64-bit) størrelser fra 800mb (uten Java) og opp til 1gb.

Difi_Integrasjonspunkt-imaget du finner her er bygd lagvis

1. alpine:3.2 (minimal linux distro, https://hub.docker.com/_/alpine/)
2. dervism/dockerjava:jre7 (utvidelse med Java 7: https://hub.docker.com/r/dervism/dockerjava/)
3. dervism/difi_integrasjonspunkt (Integrasjonspunktet)

<a name="bruk">
## Hva kan imaget brukes til

Docker-imaget gjør det enkelt å starte flere containere som kjører Integrasjonspunktet. Dette gjør det enklere å 
blant annet integrasjonsteste med flere endpoints både på lokal utviklingsmiljø og via Jenkins (i kombinasjon Docker-basert 
Adresse-Register og "Docker Container Linking").

For å bygge imaget sammen med Maven, må Dockerfile-filen ligge i rot-katalogen til Integrasjonspunktet. Dermed 
kan du bruke kommandoene nedenfor direkte. Dockerfile-scriptet er lagt opp slik at den automatisk identifiserer 
versjonsnummeret på jar-filen som bygges - mao, du kan bygge nye versjoner av Integrasjonspunktet og få et nytt 
Docker-image uten å endre Docker-scriptet. 

Scriptet henter automatisk jar-filen fra "target" mappen og 
provisjonerer denne i imaget. Scriptet installerer også automatisk Unlimited Security Profile i Java-installasjonen 
på imaget.

<a name="andrejavaapps">
### Bruke dette imaget for å andre starte Java-applikasjoner

Alt du trenger å gjøre for å starte andre Java-applikasjoner med dette imaget, er å endre miljøvariablene i scriptet:

- **APP_PREFIX:** Her skriver du prefixet til jar-filen du vil kjøre. Eks: xyz-1.0.jar, prefix = xyz.
- **APP_MAIN_CLASS:** Navnet på den kjørbare klassen
- **APP_PROFILE:** Dersom Java-applikasjonen bruker Spring-profiles, så angir du hvilken som skal aktiveres her.
- **CMD:** Dette er kommandoen Docker kjører for å starte *din* applikasjon når all provisjonering er ferdig. I dette scriptet, 
vil denne kommandoen identifisere og starte opp en standalone Java-applikasjon. Om du ønsker å starte et shell-script, 
en database, ulike typer servere og etc kan du endre kommandoen slik du normalt sett ville starte applikasjonen: 
CMD *<kommando som starter din app her>*

<a name="integrasjonstest">
## Integrasjonstesting

For integrasjonstester kan man bruke 
[Maven Docker Plugin](https://github.com/bibryam/docker-maven-plugin). Denne bruker [Docker Java Client]
(https://github.com/docker-java/docker-java) som for øvrig kan brukes uavhengig av hverandre.

Eksempel på hvordan man kan bruke Docker Java Client: https://github.com/docker-java/docker-java/wiki
Se også Spotify sin Docker-klient for Java: https://github.com/spotify/docker-client

Eksempel på hvordan man kan lage en integrasjonstest med Maven Docker Plugin står forklart her: 
http://www.javacodegeeks.com/2014/04/a-docker-maven-plugin-for-integration-testing.html

<a name="installeredocker">
## Installere Docker

Denne guiden forteller deg hvordan du installerer og tar i bruk Docker. 

1. Last ned og installer: https://docs.docker.com/machine/install-machine/  
*NB, Windows-brukere: Pass på at virtualisering ("Virtualization") er aktivert på din maskin, ellers vil ikke Docker 
fungere. Dette kan aktiveres i maskinens BIOS-innstillinger.*

2. Finn Docker QuickStart Terminal og start den. Docker vil nå bli konfigurert for din maskin.
3. Når prosessen er ferdig, skriv "docker-machine ls" for å verifisere at det finnes en Docker-server med navnet "default".

Her må du kontrollere at "state" er "running":

```shell
$ docker-machine ls
NAME      ACTIVE   DRIVER       STATE     URL                     
default   *        virtualbox   Running   tcp://192.168.99.100:2376
```

Dersom state er angitt som "Stopped" eller "Saved", må du først starte Docker-serveren:

```shell
$ docker-machine start default
Starting VM...
```

4. Skriv "docker run hello-world" og sjekk i konsoll-outputen at du ser meldingen "Hello from Docker."

```shell
$ docker run hello-world  
Unable to find image 'hello-world:latest' locally  
latest: Pulling from library/hello-world  
535020c3e8ad: Pull complete  

Hello from Docker.  
This message shows that your installation appears to be working correctly.
```

5. Docker er nå installert og du kan enten velge å fortsette med QuickStart Terminal eller konfigurere 
en vanlig Mac-terminal / Windows Commandline for Docker. Sistnevnte er anbefalt for avanserte brukere.

6. Les videre på kapittelet [Bygge Docker-image](#byggeimage) for å bygge og kjøre Integrasjonspunktet.

#### Bruke Docker med en vanlig Mac Terminal eller Windows Commandline
1. Åpne en ny terminal (Mac) eller CMD.exe (Windows, Trykk Start->Kjør->Skriv "cmd" og trykk Enter)
2. Skriv "docker-machine ls" og sjekk at Docker-serveren kjører (dsv state=running som vist i steg 3 over)

Dersom state vises som "error" eller du får meldingen *machine does not exist*, kan du enten kjøre QuickStart Terminal

3. Koble terminalen/commandline til Docker-serveren:

Mac og Linux: 

```shell
$ eval "$(docker-machine env default)" 
```

Windows: Kjør det vedlagte scriptet "setup-docker.bat" som du finner i rot-katalogen til Integrasjonspunktet.

```shell
C:\> cd <path til kildekoden>\...\praktiskprove\integrasjonspunkt
C:\...\praktiskprove\integrasjonspunkt> setup-docker.bat
Kontrollerer Docker-VM...
Konfigurerer kommandolinjen...
Kommandolinjen er klar.
```

Les mer på Docker sine egne sider:
Mac: https://docs.docker.com/installation/mac/#from-your-shell
Windows: https://docs.docker.com/installation/windows/#using-docker-from-windows-command-line-prompt-cmd-exe

<a name="byggeimage">
## Bygge Docker-image for Integrasjonspunktet

```shell
$ docker build --no-cache -t dervism/difi_integrasjonspunkt .
```

<a name="opprettecontainer">
## Opprette og starte en container

Først må du opprette en container

```shell
$ docker create --name Difi_Integrasjonspunkt -p 8080:8080 dervism/difi_integrasjonspunkt
18c87e6730917abd5d2530abb5fddae60638285c35cd792b8e184772e21a562e
```

og deretter starte den:

```shell
$ docker start Difi_Integrasjonspunkt
```

hvis du i tillegg ønsker å se console outputen, les videre om [logging](#logging).


<a name="nettleseraksess">
## Aksessere tjenestene fra egen nettleser

Først må du finne IP-adressen til den virtuelle maskinen som kjører Docker-serveren (i dette tilfellet er det VirtualBox):

```shell
$ docker-machine ip default
192.168.99.100
```

Åpne en nettleser og gå til url'en:

http://192.168.99.100:8080/noarkExchange

<a name="shelltilgang">
## Aksessere shellet / terminalen til linux distroen

Det kan av å til være nødvendig å kontrollere imaget etter at den er bygget med "docker build", feks for å
sjekke at jar-filen din ble kopiert til riktig mappe eller for å teste linux distroen. Følgende kommando starter opp 
linux distroen imaget ble bygd med og gir deg en "one-time" tilgang til shellet. I dette tilfellet, er det kun /bin/sh som
distribueres med Alpine Linux.

```shell
$ docker run -it --rm dervism/difi_integrasjonspunkt /bin/sh
/var/lib/difi # <dine shell-kommandoer her>
```

Skriv feks "ls" og du vil se jar-filen som ble kopiert inn i imaget:

```shell
/var/lib/difi # ls
integrasjonspunkt-1.0-SNAPSHOT.jar
```

Skriv "exit" for å avslutte shellet til linux distroen.

Flag:

- **-it: Interactive mode / tty:** Starter linux distroen i en isolert prosess og gjør det mulig å kjøre kommandoer "live" rett i
shellet.
- **--rm: Clean Up:** Sletter containeren når du er ferdig.


<a name="flereinstanser">
## Starte flere instanser

Samme kommando som over, med unntak av at "run" i tillegg automatisk starter containeren når den er opprettet.
Merk at de to containere må ha forskjellige navn og være mappet på ulike utgående porter (se beskrivelse nedenfor).

```shell
$ docker run --name Difi_Integrasjonspunkt1 -d -p 8088:8080 dervism/difi_integrasjonspunkt
$ docker run --name Difi_Integrasjonspunkt2 --link Difi_Integrasjonspunkt1 -d -p 8089:8080 dervism/difi_integrasjonspunkt
```

Dermed kan du aksessere dem via hver sin port på den virtuelle maskinen:

http://192.168.99.100:8088/noarkExchange og http://192.168.99.100:8089/noarkExchange

Flagg som brukes:

- **--name:** Gir et navn som gjør det enklere å starte og avslutte containeren

- **-p, Port forwarding:** Videresender informasjon fra din fysiske maskin til den virtuelle Docker-maskinen.
Format: -p hostPort:containerPort (dinMaskin:virtuellMaskin)

- **-d, Detached mode:** Kjører containeren din i en bakgrunnsprosess

- **--link Difi_Integrasjonspunkt1:** Gjør det mulig for container Difi_Integrasjonspunkt2 å bruke tjenester fra 
Difi_Integrasjonspunkt1.

Dersom du har flere tjenester og ønsker å opprette kommunikasjon mellom 
dem (typisk micro-services arkitektur), kan du lese videre om 
[container linking her](https://docs.docker.com/userguide/dockerlinks/#communication-across-links).


<a name="inspect">
## Kontrollere system-informasjonen til Docker-containeren

```shell
$ docker inspect dervism/difi_integrasjonspunkt
```

<a name="logging">
## Følge consol outputen fra Docker-containeren

```shell
$ docker logs -f Difi_Integrasjonspunkt
```

<a name="startstopp">
## Starte og stopp Docker-containeren

```shell
$ docker start Difi_Integrasjonspunkt
```

```shell
$ docker stop Difi_Integrasjonspunkt
```




