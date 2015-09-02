
SET serverport=%1

IF [%1]==[] (
SET serverport=8080
)

java -jar -Dprivatekeyalias=974720760 -Dkeystorelocation=src/main/resources/test-certificates.jks -Dprivatekeypassword=changeit target/integrasjonspunkt-1.0-SNAPSHOT.jar no.difi.meldingsutveksling.IntegrasjonspunktApplication --spring.profiles.active=dev --server.port=%serverport%