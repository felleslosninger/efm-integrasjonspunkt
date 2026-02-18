# Signeringsskript

Desse skripti handterer GPG-signering og verifisering av eFormidling-artifaktar.
Kvart skript opprettar ein isolert mellombels GPG-heimekatalog og ryddar opp etter seg.

## Føresetnader

- `gpg` må vere installert (testet med `gpg (GnuPG/MacGPG2) 2.2.41 libgcrypt 1.8.10`)
- Den rette private nøkkelfila må vere til stades `kodesignering-eformidling-private.key`
- Den rette offentlege nøkkelfila må vere til stades `kodesignering-eformidling-public.key`
- Du må kjenne passordet på den private nøkkelfila dersom du skal signere

## Skript

### verify.sh

Verifiserer ei fil mot den frittståande GPG-signaturen.

**Krev:** `kodesignering-eformidling-public.key`

```bash
./verify.sh sti/til/fil
```

Forventar at `sti/til/fil.asc` finst saman med fila, eks :
```bash
➜ ls -l
-rw-r--r--@ Feb 13 23:14 integrasjonspunkt-v4.0.0.jar
-rw-r--r--@ Feb 13 19:25 integrasjonspunkt-v4.0.0.jar.asc

➜ ./verify.sh integrasjonspunkt-v4.0.0.jar
gpg: Signature made Wed Feb 11 13:18:45 2026 CET
gpg:                using RSA key AEF27AA6948A3856932AF98ECA5643393753ECE3
gpg: Good signature from "Kodesignering eFormidling (Digitaliseringsdirektoratets nøkkel for kodesignering for eFormidling) <servicedesk@digdir.no>"
```

### sign.sh

Signerer ei fil med ein frittståande ASCII-armert GPG-signatur.

**Krev:** `kodesignering-eformidling-private.key` og miljøvariabelen `GPG_PASSPHRASE`.

```bash
GPG_PASSPHRASE="privat-nøkkel-passord" ./sign.sh sti/til/fil
```

Produserer `sti/til/fil.asc` som inneheld den frittståande signaturen og som kan verifiserast med `./verify.sh`.

Det er denne filen med signatur som skal lastes opp til GitHub releasen sammen med den nye versjonen av artifakta.

### extend.sh

Forlengar utløpsdatoen til kodesigneringsnøkkelen med 5 år og eksporterer ny og oppdaterte offentlege nøkkelen.

Den nye offentlege nøkkelen vil kunne valider både tidligere og nye signaturer, og
det er *ikke* nødvendig å oppdatere den private nøkkelen når man forlenger på denne måten. 

**Krev:** `kodesignering-eformidling-private.key` og miljøvariabelen `GPG_PASSPHRASE`.

```bash
GPG_PASSPHRASE="privat-nøkkel-passord" ./extend.sh sti/til/oppdatert-offentleg.key
```

Skriv den oppdaterte offentlege nøkkelen (med ny utløpsdato) til den oppgjevne utdatastien.
