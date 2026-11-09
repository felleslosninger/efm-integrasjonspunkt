# Signeringsskript

Desse skripti handterer GPG-signering og verifisering av eFormidling-artifaktar.
Kvart skript opprettar ein isolert mellombels GPG-heimekatalog og ryddar opp etter seg.

> [!NOTE]
> Ved release blir JAR-fila no signert automatisk av jobben `sign-and-attach-jar` i
> [call-release.yaml](../.github/workflows/call-release.yaml), som brukar org-secrets
> `EFM_GPG_PRIVATE_KEY` og `EFM_GPG_PASSPHRASE` og lastar opp `.jar`, `.sha1` og `.asc` til releasen.
> Sjå [README.md](../README.md#release-for-interne). `sign.sh` er reserveløysinga dersom den automatiske
> signeringa ikkje kan brukast, medan `verify.sh` og `extend.sh` framleis er i bruk.

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

Ved manuell signering er det denne filen med signatur som skal lastes opp til GitHub releasen sammen med den nye versjonen av artifakta.
Normalt skjer dette automatisk i release-workflowen.

### extend.sh

Forlengar utløpsdatoen til kodesigneringsnøkkelen med 5 år og eksporterer ny og oppdaterte offentlege nøkkelen.

Sjekk utløpsdato slik :
```bash
gpg --show-keys --keyid-format LONG kodesignering-eformidling-public.key
# pub   rsa4096/CA5643393753ECE3 2021-04-20 [SC] [expires: 2031-04-28]
 
gpg --show-keys --keyid-format LONG kodesignering-eformidling-private.key
# sec#  rsa4096/CA5643393753ECE3 2021-04-20 [SC] [expires: 2031-04-28] 
```

Den nye offentlege nøkkelen vil kunne validere både tidligere og nye signaturer, og
det er *ikke* nødvendig å oppdatere den private nøkkelen når man forlenger på denne måten. 

**Krev:** `kodesignering-eformidling-private.key` og miljøvariabelen `GPG_PASSPHRASE`.

```bash
GPG_PASSPHRASE="privat-nøkkel-passord" ./extend.sh sti/til/oppdatert-offentleg-nokkel
```

Skriv den oppdaterte offentlege og private nøkkelen (med ny utløpsdato) til den oppgjevne utdatastien, slik : 
```bash 
./sti/til/oppdatert-offentleg-nokkel.public.key
./sti/til/oppdatert-offentleg-nokkel.private.key
```

Husk å oppdatere offentlig nøkkel på docs.digdir.no og public + private key på Teams område.

Hugs også å oppdatere org-secret `EFM_GPG_PRIVATE_KEY` med den forlenga private nøkkelen, sidan den automatiske
signeringa elles feilar med "Unusable secret key" når den gamle utløpsdatoen er passert.
Set secret frå fil (ikkje lim inn) og test etterpå med workflowen `test-signing-secrets.yaml` (sjå [README.md](../README.md#release-for-interne)).
