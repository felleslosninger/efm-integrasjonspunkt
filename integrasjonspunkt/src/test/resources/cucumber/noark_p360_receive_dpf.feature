Feature: Receiving a DPF message and forward to Noark P360

  Background:
    Given the Noark System is enabled
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
              "orgnr": "910077473",
              "fnr": null
          },
          "id": "d4751d30-c9e5-4648-9612-956cdb5e53bc",
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
          "downloadUrl": "/mottaker/forsendelse/d4751d30-c9e5-4648-9612-956cdb5e53bc"
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
    And Fiks has the message with conversationId="d4751d30-c9e5-4648-9612-956cdb5e53bc" available

  Scenario: As Noark p360 I want to receive a DPF message

    Given a SOAP request to "http://localhost:8088/testExchangeBinding" will respond with the following payload:
    """
    <typ:PutMessageResponse xmlns:typ="http://www.arkivverket.no/Noark/Exchange/types">
        <!--Optional:-->
        <result type="OK">
           <!--Zero or more repetitions:-->
           <message code="?">
              <!--Optional:-->
              <text>?</text>
           </message>
        </result>
     </typ:PutMessageResponse>
    """
    And the application checks for new DPF messages
    Then an upload to Noark P360 is initiated with:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <ns3:PutMessageRequest xmlns:ns3="http://www.arkivverket.no/Noark/Exchange/types">
        <envelope
            contentNamespace="http://www.arkivverket.no/Noark4-1-WS-WD/types" conversationId="d4751d30-c9e5-4648-9612-956cdb5e53bc">
            <sender>
                <orgnr>910229028</orgnr>
                <name>Kontoret for voldsoffererstatning</name>
                <ref>d4751d30-c9e5-4648-9612-956cdb5e53bc</ref>
            </sender>
            <receiver>
                <orgnr>910077473</orgnr>
                <name>Sømådalen og Bessaker Revisjon</name>
            </receiver>
        </envelope>
        <payload><!--payload--></payload>
    </ns3:PutMessageRequest>
    """
    And the XML payload of the message is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <Melding xmlns="http://www.arkivverket.no/Noark4-1-WS-WD/types">
        <journpost xmlns="">
            <jpJaar>2016</jpJaar>
            <jpSeknr>25</jpSeknr>
            <jpJpostnr>13</jpJpostnr>
            <jpJdato>2016-03-21</jpJdato>
            <jpNdoktype>U</jpNdoktype>
            <jpDokdato>2016-03-21</jpDokdato>
            <jpStatus>F</jpStatus>
            <jpInnhold>Test4 - liten fil - 22.04.16</jpInnhold>
            <jpOffinnhold>Test4 - liten fil - 22.04.16</jpOffinnhold>
            <avsmot>
                <amIhtype>0</amIhtype>
            </avsmot>
            <avsmot>
                <amOrgnr>910229028</amOrgnr>
                <amIhtype>1</amIhtype>
                <amNavn>Kontoret for voldsoffererstatning</amNavn>
                <amAdresse>Pb 253</amAdresse>
                <amPostnr>9951</amPostnr>
                <amPoststed>VARDØ</amPoststed>
                <amUtland></amUtland>
            </avsmot>
            <dokument>
                <dbTittel>primary.html</dbTittel>
                <veVariant>P</veVariant>
                <veDokformat>html</veDokformat>
                <fil>
                    <base64>PGgxPlByaW1hcnkgZG9jdW1lbnQ8L2gxPgo8cD5UaGlzIGlzIHRoZSBjb250ZW50IG9mIHRoZSBwcmltYXJ5IGRvY3VtZW50LjwvcD4=</base64>
                </fil>
                <veFilnavn>primary.html</veFilnavn>
                <veMimeType>text/html</veMimeType>
            </dokument>
            <dokument>
                <dbTittel>before_the_law.txt</dbTittel>
                <veVariant>P</veVariant>
                <veDokformat>txt</veDokformat>
                <fil>
                    <base64>QmVmb3JlIHRoZSBsYXcgc2l0cyBhIGdhdGVrZWVwZXIu</base64>
                </fil>
                <veFilnavn>before_the_law.txt</veFilnavn>
                <veMimeType>text/plain</veMimeType>
            </dokument>
        </journpost>
        <noarksak xmlns="">
            <saSaar>2016</saSaar>
            <saSeknr>1</saSeknr>
            <saTittel>Test4 - liten fil - 22.04.16</saTittel>
        </noarksak>
    </Melding>
    """
    And the sent message contains the following files:
      | filename           | content type |
      | primary.html       | text/html    |
      | before_the_law.txt | text/plain   |
    And the content of the file named "primary.html" is:
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And the content of the file named "before_the_law.txt" is:
    """
    Before the law sits a gatekeeper.
    """
    And the message statuses for the conversation with id = "d4751d30-c9e5-4648-9612-956cdb5e53bc" are:
    """
    {
      "content" : [ {
        "id" : 221,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "messageId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "convId" : 220
      }, {
        "id" : 222,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT",
        "conversationId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "messageId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "convId" : 220
      }, {
        "id" : 223,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT",
        "conversationId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "messageId" : "d4751d30-c9e5-4648-9612-956cdb5e53bc",
        "convId" : 220
      } ],
      "pageable" : {
        "sort" : {
          "sorted" : true,
          "unsorted" : false,
          "empty" : false
        },
        "offset" : 0,
        "pageNumber" : 0,
        "pageSize" : 10,
        "unpaged" : false,
        "paged" : true
      },
      "last" : true,
      "totalPages" : 1,
      "totalElements" : 3,
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

