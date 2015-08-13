## Kommandolinje-verktøy for release og distribusjon av Integrasjonspunktet

BinTray er en gratis tjeneste i skyen for kontinuerlig release og distribusjon. Den tilbyr mange ulike typer repositories slik som Maven, 
Vagrant, Docker og mange flere. BinTray tilbyr også et REST-grensesnitt som gjør det enkelt scripte opp release og distribusjons delen 
av en deploymentprosess, enten via byggservere (som feks Jenkins) eller via kommandolinje-verktøy slik som presentert i denne pakken.

## BinTray.com Maven repository

### Setup

For å bruke scriptene må du først gjøre dem kjørbare

```shell
$ chmod u+x ./upload.sh && chmod u+x ./delete.sh && ./latestversion.sh
```

### Opplastning til BinTray Maven repository

Format: ./upload.sh filname bintrayPackageName

```shell
./upload.sh integrasjonspunkt-1.0.jar integrasjonspunkt
Filename integrasjonspunkt-1.0.jar
Version 1.0
Package integrasjonspunkt
Upload? [y/n]
```

Jar version will be automatically extracted from the filename.

### Slette en versjon i BinTray Maven repository

Format ./delete.sh bintrayPackageName version

```shell
./delete.sh integrasjonspunkt 1.0
Package integrasjonspunkt
Version 1.0
Delete? [y/n]y
{"message":"success"}
```

### Finne ut hva som er nyeste versjon

 Format ./latestversion.sh bintrayPackageName

```shell
 ./latestversion.sh integrasjonspunkt
Latest version: 
1.0
```
