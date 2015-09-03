@echo off
REM author: Dervis M

REM Bruk enten angitt port fra kommandolinjen
REM eller sett default til 8080

SET serverport=%1
IF [%1]==[] (
SET serverport=8080
)

REM Let etter jar-filen som skal startes og
REM kopier sertifikatet

for /f "tokens=*" %%a in ('dir ..\target\*.jar /b') do set p=%%a
if defined p (
  if not exist certificate\test-certificates.jks (
     echo Kopierer sertifikat..
     copy ..\src\main\resources\test-certificates.jks certificate\
  )
  echo Starter Integrasjonspunkt på port %serverport%
  java -jar -Dprivatekeyalias=974720760 -Dkeystorelocation=certificate/test-certificates.jks -Dprivatekeypassword=changeit ../target/%p% no.difi.meldingsutveksling.IntegrasjonspunktApplication --spring.profiles.active=dev --server.port=%serverport%
) else (
  echo Jar-filen ble ikke funnet. Har du husket å bygge ("mvn clean install -DskipTests=true")
)

