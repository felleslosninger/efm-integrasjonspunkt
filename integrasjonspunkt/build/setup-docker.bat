@echo off
REM author Dervis M
REM about  Eksporterer environment variabler slik at Docker-kommandoer kan kjøres i en vanlig Windows CMD-terminal.
REM usage  Skriv setup-docker.bat i en hvilken som helst CMD-terminal.
 
ECHO Kontrollerer Docker-VM...
docker-machine start default >nul
 
ECHO Konfigurerer kommandolinjen...
for /f "usebackq delims= tokens=*" %%i in (`docker-machine env --shell cmd default ^|find "set "`) do call %%i
 
ECHO Kommandolinjen er klar for Docker-kommandoer!