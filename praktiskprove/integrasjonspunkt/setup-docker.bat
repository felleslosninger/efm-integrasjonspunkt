@echo off
REM author Dervis M
 
ECHO Kontrollerer Docker-VM...
docker-machine start default >nul
 
ECHO Konfigurerer kommandolinjen...
for /f "usebackq delims= tokens=*" %%i in (`docker-machine env --shell cmd default ^|find "set "`) do call %%i
 
ECHO Kommandolinjen er klar for Docker-kommandoer!