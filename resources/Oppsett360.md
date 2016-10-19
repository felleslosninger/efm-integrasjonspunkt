Oppsett P360

# UTGÅENDE INNSTILLINGER

Innstillinger for utgående meldinger kan endres her:
* Logg inn på server test-sakark01 med bruker difi\sakark_inst
* Dobbeltklikk på 360SnapIn.msc som du finner på skrivebordet.
* Velg 360 Code Table Edioter på venstre menyen
* Deretter Document Dispatch Channel på høyre siden

Format: ![ChanellData](ChannelData.png)

* Trykk på «Channel Data» kolonnen i BEST/EDU raden og legg inn riktig web service URL og kryss av for Update all languages.

Format: ![SnapIn](/SnapIn.png)

* Etter endringen, kjør en iisreset via CMD

Format: ![iisreset](../iisreset.png)


# INNKOMMENDE INNSTILLINGER

For innkommende meldingen skal følgende service brukes.
http://<maksinnavn>:8088/SI.WS.Core/Integration/EDUImport.svc/EDUImportService
Importen bør utføres med bruker <domene>\svc_sakark





