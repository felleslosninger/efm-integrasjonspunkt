Feature: Receiving a Next Move DPF message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910229028/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?conversationId=81264cfa-1ba5-4fb5-a95d-48c824ed3bbb" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910229028-administrasjon.json"
    And the Noark System is disabled
    And Fiks prepares a message with the following body:
    """
    [
      {
          "avsender": {
              "adresselinje1": "",
              "adresselinje2": "",
              "adresselinje3": "",
              "navn": "Difi",
              "poststed": "Oslo",
              "postnr": "0151"
          },
          "mottaker": {
              "adresse1": null,
              "adresse2": null,
              "adresse3": null,
              "postnr": "0192",
              "poststed": "Oslo",
              "navn": "Sømådalen og Bessaker Revisjon",
              "land": "Norge",
              "orgnr": "910229028",
              "fnr": null
          },
          "id": "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
          "tittel": "Test4 - liten fil - 22.04.16",
          "date": 1553183240681,
          "metadataFraAvleverendeSystem": {
              "sakssekvensnummer": 1,
              "saksaar": 2016,
              "journalaar": 2016,
              "journalsekvensnummer": 25,
              "journalpostnummer": 13,
              "journalposttype": "U",
              "journalstatus": "F",
              "journaldato": 1458514800000,
              "dokumentetsDato": 1458514800000,
              "tittel": "Test4 - liten fil - 22.04.16",
              "saksBehandler": null,
              "ekstraMetadata": []
          },
          "metadataForImport": {
              "sakssekvensnummer": 0,
              "saksaar": 0,
              "journalposttype": null,
              "journalstatus": null,
              "dokumentetsDato": null,
              "tittel": null
          },
          "status": "Klar for mottak",
          "niva": "3",
          "filmetadata": [
              {
                  "filnavn": "primary.html",
                  "mimetype": "text/html",
                  "sha256hash": "7a936ed5f396e6d73f0f33de991be54ab34987032c44a2c14613f03a33beb035",
                  "dokumentType": null,
                  "size": 77
              },
              {
                  "filnavn": "before_the_law.txt",
                  "mimetype": "text/plain",
                  "sha256hash": "7a936ed5f396e6d73f0f33de991be54ab34987032c44a2c14613f03a33beb035",
                  "dokumentType": null,
                  "size": 33
              }
          ],
          "svarSendesTil": {
              "adresse1": "Pb 253",
              "adresse2": null,
              "adresse3": null,
              "postnr": "9951",
              "poststed": "VARDØ",
              "navn": "Kontoret for voldsoffererstatning",
              "land": "",
              "orgnr": "910229028",
              "fnr": null
          },
          "svarPaForsendelse": null,
          "forsendelseType": null,
          "eksternRef": "bb8323b9-1023-4046-b620-63c4f9120c56",
          "lenker": [],
          "downloadUrl": "/mottaker/forsendelse/81264cfa-1ba5-4fb5-a95d-48c824ed3bbb"
      }
    ]
    """
    And appends a file named "primary.html" with mimetype="text/html":
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And appends a file named "before_the_law.txt" with mimetype="text/plain":
    """
    Before the law sits a gatekeeper.
    """
    And Fiks has the message with conversationId="81264cfa-1ba5-4fb5-a95d-48c824ed3bbb" available
    And the application checks for new DPF messages

  Scenario: As a user I want to receive a DPF message

    Given I peek and lock a message
    And I pop the locked message
    And I remove the message
    Then the converted SBD is:
    """
    {
      "standardBusinessDocumentHeader" : {
        "headerVersion" : "1.0",
        "sender" : [ {
          "identifier" : {
            "value" : "0192:910229028",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "receiver" : [ {
          "identifier" : {
            "value" : "0192:910229028",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "documentIdentification" : {
          "standard" : "urn:no:difi:arkivmelding:xsd::arkivmelding",
          "typeVersion" : "2.0",
          "instanceIdentifier" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
          "type" : "arkivmelding",
          "creationDateAndTime" : "2019-03-25T12:38:18+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
            "identifier" : "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-03-26T12:38:23+01:00"
            } ]
          } ]
        }
      },
      "arkivmelding" : { }
    }
    """
    And I have an ASIC that contains a file named "arkivmelding.xml" with mimetype="application/xml":
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <arkivmelding xmlns="http://www.arkivverket.no/standarder/noark5/arkivmelding">
        <system>Integrasjonspunkt</system>
        <meldingId>81264cfa-1ba5-4fb5-a95d-48c824ed3bbb</meldingId>
        <tidspunkt/>
        <antallFiler>0</antallFiler>
        <mappe xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="saksmappe">
            <basisregistrering xsi:type="journalpost">
                <dokumentbeskrivelse>
                  <tittel>primary.html</tittel>
                  <dokumentobjekt>
                    <referanseDokumentfil>primary.html</referanseDokumentfil>
                  </dokumentobjekt>
                </dokumentbeskrivelse>
                <dokumentbeskrivelse>
                  <tittel>before_the_law.txt</tittel>
                  <dokumentobjekt>
                    <referanseDokumentfil>before_the_law.txt</referanseDokumentfil>
                  </dokumentobjekt>
                </dokumentbeskrivelse>
                <tittel>Test4 - liten fil - 22.04.16</tittel>
                <offentligTittel>Test4 - liten fil - 22.04.16</offentligTittel>
                <journalaar>2016</journalaar>
                <journalsekvensnummer>25</journalsekvensnummer>
                <journalpostnummer>13</journalpostnummer>
                <journalposttype>Utgående dokument</journalposttype>
                <journalstatus>Godkjent av leder</journalstatus>
                <journaldato>2016-03-21+01:00</journaldato>
                <dokumentetsDato>2016-03-21+01:00</dokumentetsDato>
                <korrespondansepart>
                    <korrespondanseparttype>Avsender</korrespondanseparttype>
                    <korrespondansepartNavn>Kontoret for voldsoffererstatning</korrespondansepartNavn>
                    <postadresse>Pb 253</postadresse>
                    <postnummer>9951</postnummer>
                    <poststed>VARDØ</poststed>
                    <land></land>
                </korrespondansepart>
            </basisregistrering>
            <saksaar>2016</saksaar>
            <sakssekvensnummer>1</sakssekvensnummer>
        </mappe>
    </arkivmelding>
    """
    And I have an ASIC that contains a file named "primary.html" with mimetype="text/html":
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And I have an ASIC that contains a file named "before_the_law.txt" with mimetype="text/plain":
    """
    Before the law sits a gatekeeper.
    """
    And the message statuses for the conversation with id = "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb" are:
    """
    {
      "content" : [ {
        "id" : 10,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "messageId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "convId" : 9
      }, {
        "id" : 11,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT",
        "conversationId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "messageId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "convId" : 9
      }, {
        "id" : 12,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT",
        "conversationId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "messageId" : "81264cfa-1ba5-4fb5-a95d-48c824ed3bbb",
        "convId" : 9
      } ],
      "pageable" : {
        "sort" : {
          "sorted" : true,
          "unsorted" : false,
          "empty" : false
        },
        "offset" : 0,
        "pageSize" : 10,
        "pageNumber" : 0,
        "paged" : true,
        "unpaged" : false
      },
      "last" : true,
      "totalElements" : 3,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 3,
      "first" : true,
      "empty" : false
    }
    """

