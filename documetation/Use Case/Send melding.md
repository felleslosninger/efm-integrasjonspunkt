
**Send melding**
------------

Meldingsutveksling fra/til arkivsystem.
Når meldingen kommer fra arkivsystem inn til integrasjonspunktet har sjekken på at mottakers org. nummer finnes blitt kjørt.


> **Sende melding**
> 
 1. Mottar melding i henhold til NOARK standard.
 2. Sjekk om orgnummer avsender/mottaker finnes i adresseregiste.
 3. Begge orgnummera finnes i adresseregisteret.
 5. Henter sertifikat
 4. Validering av sertifikat avsender/mottaker, begge er gyldig.
 (avsenders sertifikat er det samme som i adresseresgister, altså at public og privat del av sertifikat hører sammen)kjør virksomhetssertifikat validator på eget sertifikat?
 6. Bygger melding på SBD format, legger på adresse info som orgnumemr + BEST/EDU melding som puttes i en ASiC dokumentpakke som signeres og krypteres.
 7. Transporteres via Altinn enten ws(<=1GB) eller sftp.Kva er egentlig grensa her? i Vår dok. står det at grensa er 200MB. Sjekk
 

> **Kan ikke sende melding, finner ikkje orgnummer**
>
 1. Mottar melding i henhold til NOARK standard.
 2. Sjekk om orgnummer avsender/mottaker finnes i adresseregister, finnes ikkje.
 3. Lager og sender feilmelding.

> **Kan ikke sende melding, ugyldig sertifikat**
>
 1. Mottar NOARK melding
 2. Sjekk om orgnummer avsender/mottaker finnes i adresseregister, finnes.
 2. Sjekker så sertifikat avsender/mottaker, ikkje gyldig sertifkat.
 3. Lager og sender feilmelding.
 
> Written with [StackEdit](https://stackedit.io/).