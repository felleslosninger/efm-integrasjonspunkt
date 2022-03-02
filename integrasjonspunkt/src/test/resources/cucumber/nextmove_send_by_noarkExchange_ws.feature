Feature: Sending a BEST/EDU message by the noarkExchange WebService

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075918?conversationId=6e9cab22-a682-461c-bcd4-f201bfb3de8c" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-administrasjon.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?conversationId=6e9cab22-a682-461c-bcd4-f201bfb3de8c" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    Given a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario: As a user I want to send a BEST/EDU message

    Given the sender is "910077473"
    And the receiver is "910075918"
    And the conversationId is "6e9cab22-a682-461c-bcd4-f201bfb3de8c"
    And the messageId is "6e9cab22-a682-461c-bcd4-f201bfb3de8c"
    And the payload is:
    """
    <?xml version="1.0" encoding="utf-8"?>
    <Melding xmlns="http://www.arkivverket.no/Noark4-1-WS-WD/types">
        <journpost xmlns="">
            <jpId>40</jpId>
            <jpJaar>2016</jpJaar>
            <jpSeknr>25</jpSeknr>
            <jpJpostnr>13</jpJpostnr>
            <jpJdato>2016-03-21</jpJdato>
            <jpNdoktype>U</jpNdoktype>
            <jpDokdato>2016-03-21</jpDokdato>
            <jpStatus>F</jpStatus>
            <jpInnhold>Test - liten fil - 22.04.16</jpInnhold>
            <jpU1>0</jpU1>
            <jpForfdato/>
            <jpTgkode/>
            <jpUoff/>
            <jpAgdato/>
            <jpAgkode/>
            <jpSaksdel/>
            <jpU2>0</jpU2>
            <jpArkdel/>
            <jpTlkode/>
            <jpAntved>0</jpAntved>
            <jpSaar>2016</jpSaar>
            <jpSaseknr>1</jpSaseknr>
            <jpOffinnhold>Test18 - liten fil - 22.04.16</jpOffinnhold>
            <jpTggruppnavn/>
            <avsmot>
                <amId>92</amId>
                <amOrgnr>910075918</amOrgnr>
                <amIhtype>1</amIhtype>
                <amKopimot>0</amKopimot>
                <amBehansv>0</amBehansv>
                <amNavn>Kontoret for voldsoffererstatning</amNavn>
                <amU1>0</amU1>
                <amKortnavn>KFV</amKortnavn>
                <amAdresse>Pb 253</amAdresse>
                <amPostnr>9951</amPostnr>
                <amPoststed>VARDØ</amPoststed>
                <amUtland/>
                <amEpostadr>best@voldsoffererstatning.no</amEpostadr>
                <amRef/>
                <amJenhet/>
                <amAvskm/>
                <amAvskdato/>
                <amFrist/>
                <amForsend>D</amForsend>
                <amAdmkort>[Ufordelt]</amAdmkort>
                <amAdmbet>Ufordelt/sendt
                    tilbake til arkiv
                </amAdmbet>
                <amSbhinit>[Ufordelt]</amSbhinit>
                <amSbhnavn>Ikke
                    fordelt til saksbehandler
                </amSbhnavn>
                <amAvsavdok/>
                <amBesvardok/>
            </avsmot>
            <dokument>
                <dlRnr>1</dlRnr>
                <dlType>H</dlType>
                <dbKategori>ND</dbKategori>
                <dbTittel>Test18 - liten fil - 22.04.16</dbTittel>
                <dbStatus>F</dbStatus>
                <veVariant>A</veVariant>
                <veDokformat>RA-TXT</veDokformat>
                <fil>
                    <base64>VGVzdGluZyAxIDIgMw==</base64>
                </fil>
                <veFilnavn>test.txt</veFilnavn>
                <veMimeType>text/plain</veMimeType>
            </dokument>
        </journpost>
        <noarksak xmlns="">
            <saId>6</saId>
            <saSaar>2016</saSaar>
            <saSeknr>1</saSeknr>
            <saPapir>0</saPapir>
            <saDato>2016-01-25</saDato>
            <saTittel>Test18 - Difi</saTittel>
            <saU1>0</saU1>
            <saStatus>B</saStatus>
            <saArkdel>SAK1</saArkdel>
            <saType/>
            <saJenhet>SIVHJ</saJenhet>
            <saTgkode/>
            <saUoff/>
            <saBevtid/>
            <saKasskode/>
            <saKassdato/>
            <saProsjekt/>
            <saOfftittel>Test18 - Difi</saOfftittel>
            <saAdmkort>ES-ADM</saAdmkort>
            <saAdmbet>Administrasjon</saAdmbet>
            <saAnsvinit>MABE</saAnsvinit>
            <saAnsvnavn>Mari Berg</saAnsvnavn>
            <saTggruppnavn/>
        </noarksak>
    </Melding>
    """
    And I call the noarkExchange WebService
    Then an upload to Altinn is initiated with:
    """
    <?xml version='1.0' encoding='UTF-8'?>
    <S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
        <S:Body>
            <ns2:InitiateBrokerServiceBasic xmlns="http://schemas.altinn.no/services/ServiceEngine/Broker/2015/06"
                                            xmlns:ns2="http://www.altinn.no/services/ServiceEngine/Broker/2015/06"
                                            xmlns:ns3="http://www.altinn.no/services/common/fault/2009/10"
                                            xmlns:ns4="http://www.altinn.no/services/2009/10"
                                            xmlns:ns5="http://schemas.microsoft.com/2003/10/Serialization/"
                                            xmlns:ns6="http://schemas.altinn.no/services/serviceEntity/2015/06">
                <ns2:systemUserName>testuser</ns2:systemUserName>
                <ns2:systemPassword>testpass</ns2:systemPassword>
                <ns2:brokerServiceInitiation>
                    <Manifest>
                        <ExternalServiceCode>4192</ExternalServiceCode>
                        <ExternalServiceEditionCode>270815</ExternalServiceEditionCode>
                        <ArrayOfFile>
                            <File>
                                <FileName>sbd.zip</FileName>
                            </File>
                        </ArrayOfFile>
                        <Reportee>910077473</Reportee>
                        <SendersReference>19efbd4c-413d-4e2c-bbc5-257ef4a65b38</SendersReference>
                    </Manifest>
                    <RecipientList>
                        <Recipient>
                            <PartyNumber>910075918</PartyNumber>
                        </Recipient>
                    </RecipientList>
                </ns2:brokerServiceInitiation>
            </ns2:InitiateBrokerServiceBasic>
        </S:Body>
    </S:Envelope>
    """
    And the sent Altinn ZIP contains the following files:
      | filename       |
      | manifest.xml   |
      | recipients.xml |
      | sbd.json       |
      | asic.zip       |
    And the content of the Altinn ZIP file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <BrokerServiceManifest xmlns="http://schema.altinn.no/services/ServiceEngine/Broker/2015/06">
       <ExternalServiceCode>v3888</ExternalServiceCode>
       <ExternalServiceEditionCode>70515</ExternalServiceEditionCode>
       <SendersReference>19efbd4c-413d-4e2c-bbc5-257ef4a65b38</SendersReference>
       <Reportee>910077473</Reportee>
       <FileList>
          <File>
             <FileName>sbd.json</FileName>
          </File>
       </FileList>
    </BrokerServiceManifest>
    """
    And the content of the Altinn ZIP file named "recipients.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <BrokerServiceRecipientList xmlns="http://schema.altinn.no/services/ServiceEngine/Broker/2015/06">
       <Recipient>
          <PartyNumber>910075918</PartyNumber>
       </Recipient>
    </BrokerServiceRecipientList>
    """
    And the JSON content of the Altinn ZIP file named "sbd.json" is:
    """
    {
      "standardBusinessDocumentHeader" : {
        "headerVersion" : "1.0",
        "sender" : [ {
          "identifier" : {
            "value" : "0192:910077473",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "receiver" : [ {
          "identifier" : {
            "value" : "0192:910075918",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "documentIdentification" : {
          "standard" : "urn:no:difi:arkivmelding:xsd::arkivmelding",
          "typeVersion" : "2.0",
          "instanceIdentifier" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
          "type" : "arkivmelding",
          "creationDateAndTime" : "2019-03-25T12:38:18+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
            "identifier" : "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-03-26T12:38:23+01:00"
            } ]
          } ]
        }
      },
      "arkivmelding" : {
        "hoveddokument" : "arkivmelding.xml"
      }
    }
    """
    And the sent message contains the following files:
      | filename         | content type    |
      | manifest.xml     | application/xml |
      | arkivmelding.xml | text/xml        |
      | test.txt         | text/plain      |
    And the content of the file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <manifest>
       <mottaker>
          <organisasjon authority="iso6523-actorid-upis">0192:910075918</organisasjon>
       </mottaker>
       <avsender>
          <organisasjon authority="iso6523-actorid-upis">0192:910077473</organisasjon>
       </avsender>
       <hoveddokument href="arkivmelding.xml" mime="text/xml">
          <tittel lang="no">Hoveddokument</tittel>
       </hoveddokument>
    </manifest>
    """
    And the XML content of the file named "arkivmelding.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <arkivmelding xmlns="http://www.arkivverket.no/standarder/noark5/arkivmelding">
        <antallFiler>0</antallFiler>
        <mappe xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="saksmappe">
            <systemID>6</systemID>
            <tittel>Test18 - Difi</tittel>
            <offentligTittel>Test18 - Difi</offentligTittel>
            <referanseArkivdel>SAK1</referanseArkivdel>
            <skjerming>
                <skjermingshjemmel></skjermingshjemmel>
            </skjerming>
            <basisregistrering xsi:type="journalpost">
                <systemID>40</systemID>
                <referanseArkivdel></referanseArkivdel>
                <dokumentbeskrivelse>
                    <tittel>Test18 - liten fil - 22.04.16</tittel>
                    <tilknyttetRegistreringSom>Hoveddokument</tilknyttetRegistreringSom>
                    <dokumentnummer>1</dokumentnummer>
                    <dokumentobjekt>
                        <variantformat>Arkivformat</variantformat>
                        <referanseDokumentfil>test.txt</referanseDokumentfil>
                    </dokumentobjekt>
                </dokumentbeskrivelse>
                <tittel>Test - liten fil - 22.04.16</tittel>
                <offentligTittel>Test18 - liten fil - 22.04.16</offentligTittel>
                <journalaar>2016</journalaar>
                <journalsekvensnummer>25</journalsekvensnummer>
                <journalpostnummer>13</journalpostnummer>
                <journalposttype>Utgående dokument</journalposttype>
                <journalstatus>Godkjent av leder</journalstatus>
                <journaldato>2016-03-21</journaldato>
                <dokumentetsDato>2016-03-21</dokumentetsDato>
                <antallVedlegg>0</antallVedlegg>
                <korrespondansepart>
                    <korrespondanseparttype>Mottaker</korrespondanseparttype>
                    <korrespondansepartNavn>Kontoret for voldsoffererstatning</korrespondansepartNavn>
                    <postadresse>Pb 253</postadresse>
                    <postnummer>9951</postnummer>
                    <poststed>VARDØ</poststed>
                    <land></land>
                    <administrativEnhet>[Ufordelt]</administrativEnhet>
                    <saksbehandler>[Ufordelt]</saksbehandler>
                </korrespondansepart>
                <avskrivning>
                    <referanseAvskrivesAvJournalpost></referanseAvskrivesAvJournalpost>
                </avskrivning>
            </basisregistrering>
            <saksaar>2016</saksaar>
            <sakssekvensnummer>1</sakssekvensnummer>
            <saksdato>2016-01-25</saksdato>
            <administrativEnhet>ES-ADM</administrativEnhet>
            <saksansvarlig>MABE</saksansvarlig>
            <journalenhet>SIVHJ</journalenhet>
            <saksstatus>Under behandling</saksstatus>
        </mappe>
    </arkivmelding>
    """
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """
    And the message statuses for the conversation with id = "6e9cab22-a682-461c-bcd4-f201bfb3de8c" are:
    """
    {
      "content" : [ {
        "id" : 25,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
        "messageId" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
        "convId" : 24
      }, {
        "id" : 28,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "SENDT",
        "conversationId" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
        "messageId" : "6e9cab22-a682-461c-bcd4-f201bfb3de8c",
        "convId" : 24
      } ],
      "pageable" : {
        "sort" : {
          "unsorted" : false,
          "sorted" : true,
          "empty" : false
        },
        "offset" : 0,
        "pageNumber" : 0,
        "pageSize" : 10,
        "unpaged" : false,
        "paged" : true
      },
      "last" : true,
      "totalElements" : 2,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "unsorted" : false,
        "sorted" : true,
        "empty" : false
      },
      "numberOfElements" : 2,
      "first" : true,
      "empty" : false
    }
    """
