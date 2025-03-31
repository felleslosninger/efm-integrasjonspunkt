Feature: Sending a Next Move DPF message

  Background:

    Given a "GET" request to "http://localhost:9099/identifier/987464291?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/987464291.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924?securityLevel=3&conversationId=22efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?securityLevel=3&conversationId=22efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924-administrasjon.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/info/910075924" will respond with status "200" and the following "application/json" in "/restmocks/info/910075924.json"
    And a "GET" request to "http://localhost:9099/info/910077473" will respond with status "200" and the following "application/json" in "/restmocks/info/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And a SOAP request to "https://test.svarut.ks.no/tjenester/forsendelseservice/ForsendelsesServiceV9" with element "retreiveForsendelseTyper" will respond with the following payload:
    """
    <ser:retreiveForsendelseTyperResponse xmlns:ser="http://www.ks.no/svarut/servicesV9">
       <!--Optional:-->
       <return>forsendelsetype-1</return>
    </ser:retreiveForsendelseTyperResponse>
    """
    And a SOAP request to "https://test.svarut.ks.no/tjenester/forsendelseservice/ForsendelsesServiceV9" with element "sendForsendelseMedId" will respond with the following payload:
    """
    <ser:sendForsendelseMedIdResponse xmlns:ser="http://www.ks.no/svarut/servicesV9">
       <!--Optional:-->
       <return>?</return>
    </ser:sendForsendelseMedIdResponse>
    """

  Scenario: As a user I want to send a DPF message with valid forsendelsetype
    Given I POST the following message:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": "2019-05-10T00:31:52+01:00"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "22efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "abc8849c-e281-4809-8555-7cd54952b946",
                "standard": "urn:no:difi:arkivmelding:xsd::arkivmelding",
                "type": "arkivmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075924"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910077473"
                    }
                }
            ]
        },
        "arkivmelding": {
          "sikkerhetsnivaa": 3,
          "dpf": {
              "forsendelseType": "forsendelsetype-1"
              },
          "hoveddokument": "arkivmelding.xml"
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "arkivmelding.xml" with mimetype "application/xml" and title "Arkivmelding" with the following body:
    """
    <?xml version="1.0" encoding="utf-8"?>
    <arkivmelding xmlns="http://www.arkivverket.no/standarder/noark5/arkivmelding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.arkivverket.no/standarder/noark5/arkivmelding arkivmelding.xsd">
        <system>LandLord</system>
        <meldingId>3380ed76-5d4c-43e7-aa70-8ed8d97e4835</meldingId>
        <tidspunkt>2017-05-23T12:46:00</tidspunkt>
        <antallFiler>1</antallFiler>

        <mappe xsi:type="saksmappe">
            <systemID>43fbe161-7aac-4c9f-a888-d8167aab4144</systemID>
            <tittel>Nye lysrør Hauketo Skole</tittel>
            <opprettetDato>2017-06-01T10:10:12.000+01:00</opprettetDato>
            <opprettetAv/>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Funksjoner</referanseKlassifikasjonssystem>
                <klasseID>vedlikehold av skole</klasseID>
                <tittel>vedlikehold av skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <klassifikasjon>
                <referanseKlassifikasjonssystem>Objekter</referanseKlassifikasjonssystem>
                <klasseID>20500</klasseID>
                <tittel>Hauketo Skole</tittel>
                <opprettetDato>2017-05-23T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>Knut Hansen</opprettetAv>
            </klassifikasjon>
            <basisregistrering xsi:type="journalpost">
                <systemID>430a6710-a3d4-4863-8bd0-5eb1021bee45</systemID>
                <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                <opprettetAv>LandLord</opprettetAv>
                <arkivertDato>2012-02-17T21:56:12.000+01:00</arkivertDato>
                <arkivertAv>LandLord</arkivertAv>
                <referanseForelderMappe>43fbe161-7aac-4c9f-a888-d8167aab4144</referanseForelderMappe>
                <dokumentbeskrivelse>
                    <systemID>3e518e5b-a361-42c7-8668-bcbb9eecf18d</systemID>
                    <dokumenttype>Bestilling</dokumenttype>
                    <dokumentstatus>Dokumentet er ferdigstilt</dokumentstatus>
                    <tittel>Bestilling - nye lysrør</tittel>
                    <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                    <opprettetAv>Landlord</opprettetAv>
                    <tilknyttetRegistreringSom>Hoveddokument</tilknyttetRegistreringSom>
                    <dokumentnummer>1</dokumentnummer>
                    <tilknyttetDato>2012-02-17T21:56:12.000+01:00</tilknyttetDato>
                    <tilknyttetAv>Landlord</tilknyttetAv>
                    <dokumentobjekt>
                        <versjonsnummer>1</versjonsnummer>
                        <variantformat>Produksjonsformat</variantformat>
                        <opprettetDato>2012-02-17T21:56:12.000+01:00</opprettetDato>
                        <opprettetAv>Landlord</opprettetAv>
                        <referanseDokumentfil>test.txt</referanseDokumentfil>
                    </dokumentobjekt>
                </dokumentbeskrivelse>
                <tittel>Nye lysrør</tittel>
                <offentligTittel>Nye lysrør</offentligTittel>

                <virksomhetsspesifikkeMetadata>
                    <forvaltningsnummer>20050</forvaltningsnummer>
                    <objektnavn>Hauketo Skole</objektnavn>
                    <eiendom>200501</eiendom>
                    <bygning>2005001</bygning>
                    <bestillingtype>Materiell, elektro</bestillingtype>
                    <rammeavtale>K-123123-elektriker</rammeavtale>
                </virksomhetsspesifikkeMetadata>

                <journalposttype>Utgående dokument</journalposttype>
                <journalstatus>Journalført</journalstatus>
                <journaldato>2017-05-23</journaldato>
                <korrespondansepart>
                    <korrespondanseparttype>Mottaker</korrespondanseparttype>
                    <korrespondansepartNavn>elektrikeren AS, Veien 100, Oslo</korrespondansepartNavn>
                </korrespondansepart>
            </basisregistrering>
            <saksdato>2017-06-01</saksdato>
            <administrativEnhet>Blah</administrativEnhet>
            <saksansvarlig>KNUTKÅRE</saksansvarlig>
            <saksstatus>Avsluttet</saksstatus>
        </mappe>
    </arkivmelding>
    """
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then an upload to Fiks is initiated with:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <retreiveForsendelseTyper xmlns="http://www.ks.no/svarut/servicesV9"/>
    """
    Then an upload to Fiks is initiated with:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <sendForsendelseMedId xmlns="http://www.ks.no/svarut/servicesV9">
        <forsendelse>
            <avgivendeSystem></avgivendeSystem>
            <dokumenter>
                <data><!--encrypted content--></data>
                <ekskluderesFraPrint>false</ekskluderesFraPrint>
                <filnavn>test.txt</filnavn>
                <mimetype>text/plain</mimetype>
                <skalSigneres>false</skalSigneres>
            </dokumenter>
            <eksternref>abc8849c-e281-4809-8555-7cd54952b946</eksternref>
            <forsendelseType>forsendelsetype-1</forsendelseType>
            <krevNiva4Innlogging>false</krevNiva4Innlogging>
            <kryptert>true</kryptert>
            <kunDigitalLevering>false</kunDigitalLevering>
            <metadataFraAvleverendeSystem>
                <ekstraMetadata>
                    <key>forvaltningsnummer</key>
                    <value>20050</value>
                </ekstraMetadata>
                <ekstraMetadata>
                    <key>objektnavn</key>
                    <value>Hauketo Skole</value>
                </ekstraMetadata>
                <ekstraMetadata>
                    <key>eiendom</key>
                    <value>200501</value>
                </ekstraMetadata>
                <ekstraMetadata>
                    <key>bygning</key>
                    <value>2005001</value>
                </ekstraMetadata>
                <ekstraMetadata>
                    <key>bestillingtype</key>
                    <value>Materiell, elektro</value>
                </ekstraMetadata>
                <ekstraMetadata>
                    <key>rammeavtale</key>
                    <value>K-123123-elektriker</value>
                </ekstraMetadata>
                <journalaar>0</journalaar>
                <journaldato>2017-05-23T00:00:00</journaldato>
                <journalpostnummer>0</journalpostnummer>
                <journalposttype>U</journalposttype>
                <journalsekvensnummer>0</journalsekvensnummer>
                <journalstatus>J</journalstatus>
                <saksaar>0</saksaar>
                <sakssekvensnummer>0</sakssekvensnummer>
                <tittel>Nye lysrør</tittel>
            </metadataFraAvleverendeSystem>
            <mottaker>
                <digitalAdresse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:type="organisasjonDigitalAdresse">
                    <orgnr>910075924</orgnr>
                </digitalAdresse>
                <postAdresse>
                    <land>Norge</land>
                    <navn>TEST - C4</navn>
                    <postnr>0192</postnr>
                    <poststed>Oslo</poststed>
                </postAdresse>
            </mottaker>
            <printkonfigurasjon>
                <brevtype>BPOST</brevtype>
                <fargePrint>false</fargePrint>
                <tosidig>true</tosidig>
            </printkonfigurasjon>
            <signaturtype xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            <signeringUtloper xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            <svarPaForsendelseLink>false</svarPaForsendelseLink>
            <svarSendesTil>
                <digitalAdresse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:type="organisasjonDigitalAdresse">
                    <orgnr>910077473</orgnr>
                </digitalAdresse>
                <postAdresse>
                    <land>Norge</land>
                    <navn>TEST - C4</navn>
                    <postnr>0192</postnr>
                    <poststed>Oslo</poststed>
                </postAdresse>
            </svarSendesTil>
            <tittel>Nye lysrør</tittel>
        </forsendelse>
        <forsendelsesid>abc8849c-e281-4809-8555-7cd54952b946</forsendelsesid>
    </sendForsendelseMedId>
    """
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """
