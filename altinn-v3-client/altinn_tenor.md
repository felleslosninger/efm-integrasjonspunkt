# Bruk av Tenor organisasjoner for testing 
For DPO og DPV er det mulig å benytte syntetiske test orgnisasjoner til testing.

FIXME : dette er ikke en fullstendig liste av trinn - må renskrives og oppdateres med mer detaljer

- Finn deg en syntetisk test organisasjon i Tenor
- Hvis du skal teste DPO
- - Velg en organisasjons med daglig leder
- - Sørg for at daglig leder har bankid test
- - Lag test sertifikat for CMS/ASIC og last opp i Virksert
- - Registrer org som mottaker i ELMA test
- Opprett maskinporten klient med JWK
- - Se kode for å generere JWK pair [TestMaskinportenTokenUsingJwk.java](src/test/java/no/difi/meldingsutveksling/altinnv3/TestMaskinportenTokenUsingJwk.java)
- - Registrer public JWK nøkkel i maskinporten via forvaltningsadmin
- - Den private JWK nøkkel konfigurere du i IPv4 properties

