# Kjøre Integrasjonspunktet i en isolert Docker-container

### Innhold
---

+ [Docker og Java](#about)
+ [Hva kan imaget brukes til](#bruk)
+ [Installere Docker](#installeredocker)
+ [Bygge Docker-image for Integrasjonspunktet](#byggeimage)
+ [Bygge og kjøre Adresse-Registeret](#adresseregisteret)
+ [Aksessere tjenestene fra egen nettleser](#nettleseraksess)
+ [Følge consol outputen fra Docker-containeren](#logging)
+ [Starte og stopp Docker-containeren](#startstopp)
+ [Aksessere shellet / terminalen til linux distroen](#shelltilgang)
+ [Starte flere instanser](#flereinstanser)
+ [Kontrollere system-informasjonen til Docker-containeren](#inspect)
+ [Integrasjonstesting](#integrasjonstest)

---

<a name="about">
## Docker og Java

Dette imaget baserer seg på en Linux distro som heter Alpine Linux. Denne er blant de aller minste av størrelse 
og sammen med Java JRE 7 og Integrasjonspunktet, gir den en total størrelse på rundt 200 mb. Til sammenligning 
gir de vanligste Ubuntu-distroene (Trusty og Precise 64-bit) størrelser fra 800mb (uten Java) og opp til 1gb.

Difi_Integrasjonspunkt-imaget du finner her er bygd lagvis

1. alpine:3.2 (minimal linux distro, https://hub.docker.com/_/alpine/)
2. dervism/dockerjava:jre7 (utvidelse med Java 7: https://hub.docker.com/r/dervism/dockerjava/)
3. difi/difi_integrasjonspunkt (Integrasjonspunktet)

### Quick Start for Mac og Linux

Windows-brukere må foreløpig bygge manuelt: [Les her](#byggeimage)

1. Installer Docker Machine (beskrevet under)
2. Åpne en terminal (beskrevet [her](#commandline))
3. Naviger deg til integrasjonspunkt-mappen
4. Kjør kommando: chmod u+x build-docker.sh
5. Kjør kommando: ./build-docker.sh
6. Kjør kommando: docker start Difi_Integrasjonspunkt
7. Valgfritt: docker logs -f Difi_Integrasjonspunkt (trykk CTRL+C for å lukke loggen)

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

<a name="installeredocker">
## Installere Docker

Denne guiden forteller deg hvordan du installerer og tar i bruk Docker på Mac og Windows. Bruker du Linux, følger du i stedet 
guiden til Docker finnes her: https://docs.docker.com/linux/step_one/

1. Last ned og installer: https://docs.docker.com/machine/install-machine/  
*NB, Windows-brukere: Pass på at virtualisering ("Virtualization") er aktivert på din maskin, ellers vil ikke Docker 
fungere. Dette kan aktiveres i maskinens BIOS-innstillinger.*

2. Finn Docker QuickStart Terminal og start den. Docker vil nå bli konfigurert for din maskin.

3. Når prosessen er ferdig, skriv "docker-machine ls" for å verifisere at det finnes en Docker-server med navnet "default".

    Her må du kontrollere at "state" er "Running":
    
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

<a name="commandline">
#### Bruke Docker med en vanlig Mac Terminal eller Windows Commandline
1. Lukk QuickStart Terminal-vinduet dersom den er oppe og åpne en ny terminal (Mac) eller CMD.exe 
(Windows, Trykk Start->Kjør->Skriv "cmd" og trykk Enter)
2. Skriv "docker-machine ls" og sjekk at Docker-serveren kjører (dsv state=running som vist i steg 3 over)

    Dersom state vises som "error" eller du får meldingen *machine does not exist*, kan du enten kjøre QuickStart Terminal

3. Koble terminalen/commandline til Docker-serveren:

    **Mac og Linux:** 
    
    ```shell
    $ eval "$(docker-machine env default)" 
    ```

    **Windows:** Kjør det vedlagte scriptet "setup-docker.bat" som du finner i "build" til Integrasjonspunktet.
    
    ```shell
    C:\> cd <path til kildekoden>\...\integrasjonspunkt
    C:\...\integrasjonspunkt> build\setup-docker.bat
    Kontrollerer Docker-VM...
    Konfigurerer kommandolinjen...
    Kommandolinjen er klar.
    ```
    
    NB: Merk at kommandoen må kalles fra rot-katalogen!

    Les mer på Docker sine egne sider:
    Mac: https://docs.docker.com/installation/mac/#from-your-shell  
    Windows: https://docs.docker.com/installation/windows/#using-docker-from-windows-command-line-prompt-cmd-exe

<a name="byggeimage">
## Bygge Docker-image for Integrasjonspunktet

Dersom du bruker Windows, må du bygge manuelt. Se neste avsnitt om "Manuell bygging".

#### Automatisk bygging (kun for Mac og Linux)

Bygg med det medfølgende scriptet:

```shell
$ ./build-docker.sh 8098 12345678
```

Merk at vi må angi et portnummer (8098) og et organisasjonsnummer (12345678). Erstatt disse med eget portnummer på din server
og virksomhetens organisasjonsnummer.

Deretter startes Integrasjonspunktet automatisk under navnet "Difi_Integrasjonspunkt_8098".

#### Manuell bygging

I  dette eksemplet blir imaget som lagres på maskinen kalt "difi/difi_integrasjonspunkt" og 
dette er navnet du må angi når du skal opprette en container som kjører imaget. Formatet er "dockerHubBrukernavn / imageNavn", men dette er 
kun nødvendig om du senere skal opensource imaget til feks DockerHub. Du kan altså fint kalle den hva som helst, feks 
kun "integrasjonspunkt". 

```shell
$ docker build --no-cache -t difi/difi_integrasjonspunkt .
```

Når en applikasjonen er pakket i et *image*, kan du kjøre den i en *container*.

Opprette container:

```shell
$ docker create --name Difi_Integrasjonspunkt -p 8080:8080 difi/difi_integrasjonspunkt
18c87e6730917abd5d2530abb5fddae60638285c35cd792b8e184772e21a562e
```

og deretter start den:

```shell
$ docker start Difi_Integrasjonspunkt
```

Hvis du i tillegg ønsker å se console outputen, les videre om [logging](#logging).

**NB:** Når du så skal starte en container, er det navnet du har angitt etter "--name" som må angis!


<a name="adresseregisteret">
## Bygge og kjøre Adresse-Registeret

Den samme fremgangsmåten som beskrevet over:

```shell
$ docker build --no-cache -t difi/adresseregister .
...
$ docker create --name Difi_AdresseRegister -p 9999:9999 difi/adresseregister
...
$ docker start Difi_AdresseRegister
...
```

Og til slutt, om du ønsker å kontrollere konsoll-outputen:

```shell
$ docker logs -f Difi_AdresseRegister
```

Og når du er ferdig og ønsker å stoppe prosessen:

```shell
$ docker stop Difi_AdresseRegister
```

Dockerfile for Adresse-Registeret finner du i rot-katalogen til adresseregister-web:

https://github.com/difi/meldingsutveksling-mellom-offentlige-virksomheter/tree/master/adresseregister-web


<a name="nettleseraksess">
## Aksessere tjenestene fra egen nettleser

Først må du finne IP-adressen til den virtuelle maskinen som kjører Docker-serveren (i dette tilfellet er det VirtualBox):

```shell
$ docker-machine ip default
192.168.99.100
```

Åpne en nettleser og gå til url'en (ip-adressen her kan være forskjellig på din maskin):

http://192.168.99.100:8080/noarkExchange

Har du også startet Adresse-Registeret, vil du finne denne her:

http://192.168.99.100:9999/certificates

Om du lurer på hvorfor portene 8080 og 9999 angis her, kommer dette av en kombinasjon av parameteren "-p 9999:9999" i *docker create* 
kommandoen og EXPOSE-kommandoen som er angitt i Dockerfile. Disse to sammen sørger for korrekt port-forwarding slik at data blir eksponert 
ut fra containeren, til Docker VM og til slutt til din nettleser. Dataflyten er omtrent slik:

Din nettleser (fysisk maskin) <-- "-p 8080:8080" --> Docker-VM (virtuell maskin) <-- EXPOSE 8080 --> Container (isolerte prosessen)


<a name="logging">
## Følge consol outputen fra Docker-containeren

```shell
$ docker logs -f Difi_Integrasjonspunkt
```

<a name="startstopp">
## Starte og stopp Docker-containeren

Start og Stop kommandoene funker kun dersom du først har opprettet en container med "docker create"

```shell
$ docker start Difi_Integrasjonspunkt
```

```shell
$ docker stop Difi_Integrasjonspunkt
```

<a name="shelltilgang">
## Aksessere shellet / terminalen til linux distroen

Det kan av å til være nødvendig å kontrollere imaget etter at den er bygget med "docker build", feks for å
sjekke at jar-filen din ble kopiert til riktig mappe eller for å teste linux distroen. Følgende kommando starter opp 
linux distroen imaget ble bygd med og gir deg en "one-time" tilgang til shellet. I dette tilfellet, er det kun /bin/sh som
distribueres med Alpine Linux.

```shell
$ docker run -it --rm difi/difi_integrasjonspunkt /bin/sh
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
$ docker run --name Difi_Integrasjonspunkt1 -d -p 8088:8080 difi/difi_integrasjonspunkt
$ docker run --name Difi_Integrasjonspunkt2 -d -p 8089:8080 difi/difi_integrasjonspunkt
```

Dermed kan du aksessere dem via hver sin port på den virtuelle maskinen:

http://192.168.99.100:8088/noarkExchange og http://192.168.99.100:8089/noarkExchange

Flagg som brukes:

- **--name:** Gir et navn som gjør det enklere å starte og avslutte containeren

- **-p, Port forwarding:** Videresender informasjon fra din fysiske maskin til den virtuelle Docker-maskinen.
Format: -p hostPort:containerPort (dinMaskin:virtuellMaskin)

- **-d, Detached mode:** Kjører containeren din i en bakgrunnsprosess

<a name="integrasjonstest">
## Integrasjonstesting

For integrasjonstester kan man bruke 
[Maven Docker Plugin](https://github.com/bibryam/docker-maven-plugin). Denne bruker [Docker Java Client]
(https://github.com/docker-java/docker-java) som for øvrig kan brukes uavhengig av hverandre.

Eksempel på hvordan man kan bruke Docker Java Client: https://github.com/docker-java/docker-java/wiki
Se også Spotify sin Docker-klient for Java: https://github.com/spotify/docker-client

Eksempel på hvordan man kan lage en integrasjonstest med Maven Docker Plugin står forklart her: 
http://www.javacodegeeks.com/2014/04/a-docker-maven-plugin-for-integration-testing.html

