## Kommandolinje-verktøy for deployment og release

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

### Delete a version in BinTray Maven repository

Format ./delete.sh bintrayPackageName version

```shell
./delete.sh integrasjonspunkt 1.0
Package integrasjonspunkt
Version 1.0
Delete? [y/n]y
{"message":"success"}
```

### Latest version

 Format ./latestversion.sh bintrayPackageName

```shell
 ./latestversion.sh integrasjonspunkt
Latest version: 
1.0
```
