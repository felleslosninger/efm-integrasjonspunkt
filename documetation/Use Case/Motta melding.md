
**Motta melding**
------------

>**Kan motta melding**
> 1. Mottatt melding på SBD format.
> 2. Validerer sender orgnummer og sertifikat, begge ok. (Skal det ikkje sjekkes noke her?)sjekker avsender
> 2. Sjekker hvilken meldingstype det er, ikke en kvittering. sjekk logging
> 3. Lager en leveringskvittering som sendes tilbake til avsender.
> 2. Meldingen blir dekryptert og ASiC pakkens signatur valideret.
> 4. Sender melding til mottakende Arkivsystem, melding mottatt.
> 6. Lager åpningsvkvittering å sender til avsender sitt integarsjonspungt.
nb. sjekk antall meldingskvitteringer 

>**Kan motta melding - kvittering**
> 1. Mottatt melding på SBD format.
> 2. Validerer sender orgnummer og sertifikat, begge ok. (Skal det ikkje sjekkes noke her?)
> 3. Sjekker hvilken meldingstype det er, type kvittering.
> 4.  Logger at kviteringsmelding er  mottatt.

>**Dekryptering av mottatt melding feiler**
> 1. Mottatt melding på SBD format.
> 2. Validerer sender orgnummer og sertifikat, begge ok.?
> 3. Sjekker hvilken meldingstype det er, er de en kvittering logges den om mottatt og er ferdig.
> 4. Er det en annen meldingstype, lages det en leveringskvittering som sendes tilbake til avsender.
> 5. Dekryptering av meldingfeiler.
> 6. Valideringsfeil, feilmeldingsendt og logget. 

> **Validering av signatur feiler**
> 1. Mottatt melding på SBD format.
> 2. Validerer sender orgnummer og sertifikat, begge ok.?
> 2. Sjekker hvilken meldingstype det er, er de en kvittering logges den om mottatt og er ferdig.
> 3. Er det en annen meldingstype, lages det en leveringskvittering som sendes tilbake til avsender.
> 2. Meldingen blir dekryptert og ASiC pakkens, valideringa av sigatur feiler.
> 6. Valideringsfeil, feilmeldingsendt og logget. 


> Written with [StackEdit](https://stackedit.io/).