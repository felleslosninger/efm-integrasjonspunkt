Feature: Sending a Next Move DPI utskrift message (JSON)

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/09118532322?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322.json"
    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?securityLevel=3&conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario: As a user I want to send a DPI print message
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "60b9da9f-c056-430a-936b-3922721086fd",
                "standard": "urn:no:difi:digitalpost:xsd:fysisk::print",
                "type": "print",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "09118532322"
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
        "print": {
            "hoveddokument": "arkivmelding.xml",
            "avsenderId": "avsender910077473",
            "fakturaReferanse": "faktura910077473",
            "mottaker":{
                "navn": "Ola Nordmann",
                "adresselinje1": "Fjellveien 1",
                "adresselinje2": "Langt oppi lia",
                "adresselinje3": "der trollene bor",
                "postnummer": "3400",
                "poststed": "FJELLET",
                "land": "Norway"
            },
            "utskriftsfarge" : "SORT_HVIT",
            "posttype": "B_OEKONOMI",
            "retur":{
                "mottaker":{
                    "navn": "Dr. Sprøyte Gal",
                    "adresselinje1": "Sadistveien 1",
                    "adresselinje2": "argh",
                    "adresselinje3": "uff",
                    "adresselinje4": "off",
                    "postnummer": "0195",
                    "poststed": "Oslo",
                    "land": "Norge"
                },
                "returhaandtering": "DIREKTE_RETUR"
            }
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
    Then a DPI message is sent to corner2
    And the sent message's SBD is:
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
            "value" : "0192:987464291",
            "authority" : "iso6523-actorid-upis"
          }
        } ],
        "documentIdentification" : {
          "standard" : "urn:fdc:digdir.no:2020:innbyggerpost:xsd::innbyggerpost##urn:fdc:digdir.no:2020:innbyggerpost:schema:utskrift::1.0",
          "typeVersion" : "1.0",
          "instanceIdentifier" : "60b9da9f-c056-430a-936b-3922721086fd",
          "type" : "utskrift",
          "creationDateAndTime" : "2019-03-25T11:38:23Z"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
            "identifier" : "urn:fdc:digdir.no:2020:profile:egovernment:innbyggerpost:utskrift:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-05-09T23:31:52Z"
            } ]
          } ]
        }
      },
      "utskrift" : {
        "avsender" : {
          "virksomhetsidentifikator" : {
            "authority" : "iso6523-actorid-upis",
            "value" : "0192:910077473"
          },
          "avsenderidentifikator" : "avsender910077473",
          "fakturaReferanse": "faktura910077473"
        },
        "mottaker" : {
          "navn" : "Ola Nordmann",
          "adresselinje1" : "Fjellveien 1",
          "adresselinje2" : "Langt oppi lia",
          "adresselinje3" : "der trollene bor",
          "postnummer" : "3400",
          "poststed" : "FJELLET"
        },
        "dokumentpakkefingeravtrykk" : {
          "digestMethod" : "http://www.w3.org/2001/04/xmlenc#sha256",
          "digestValue" : "dummy"
        },
        "maskinportentoken" : "DummyMaskinportenToken",
        "utskriftstype" : "SORT_HVIT",
        "retur":{
                "mottaker":{
                    "navn": "Dr. Sprøyte Gal",
                    "adresselinje1": "Sadistveien 1",
                    "adresselinje2": "argh",
                    "adresselinje3": "uff",
                    "adresselinje4": "off",
                    "postnummer": "0195",
                    "poststed": "Oslo"
                },
          "returposthaandtering" : "DIREKTE_RETUR"
        },
        "posttype" : "B"
      }
    }
    """
    And the sent message contains the following files:
      | filename         | content type |
      | manifest.xml     |              |
      | arkivmelding.xml |              |
      | test.txt         |              |

    And the XML content of the file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <manifest xmlns="http://begrep.difi.no/sdp/schema_v10">
        <avsender>
            <organisasjon authority="iso6523-actorid-upis">0192:910077473</organisasjon>
            <avsenderidentifikator>avsender910077473</avsenderidentifikator>
            <fakturaReferanse>faktura910077473</fakturaReferanse>
        </avsender>
        <hoveddokument href="arkivmelding.xml" mime="application/xml">
            <tittel lang="NO">Arkivmelding</tittel>
        </hoveddokument>
        <vedlegg href="test.txt" mime="text/plain">
            <tittel lang="NO">Test</tittel>
        </vedlegg>
    </manifest>
    """
    And the XML content of the file named "arkivmelding.xml" is:
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
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """

  Scenario: As a user I want to send a DPI print message without return address information
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "bb74b391-177e-41b8-a7bb-14eb5b06ec3c",
                "standard": "urn:no:difi:digitalpost:xsd:fysisk::print",
                "type": "print",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
             "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "09118532322"
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
        "print": {
            "hoveddokument": "arkivmelding.xml",
            "avsenderId": "avsender910077473",
            "fakturaReferanse": "faktura910077473",
            "mottaker":{
                "navn": "Ola Nordmann",
                "adresselinje1": "Fjellveien 1",
                "adresselinje2": "Langt oppi lia",
                "adresselinje3": "der trollene bor",
                "postnummer": "3400",
                "poststed": "FJELLET",
                "land": "Norway"
            },
            "utskriftsfarge" : "SORT_HVIT",
            "posttype": "B_OEKONOMI",
            "retur":{
                "mottaker":{
                    "navn": "Dr. Sprøyte Gal",
                    "adresselinje1": "Sadistveien 1",
                    "adresselinje2": "argh",
                    "adresselinje3": "uff",
                    "adresselinje4": "off",
                    "postnummer": "",
                    "poststed": "",
                    "land": "Norge"
                },
                "returhaandtering": "DIREKTE_RETUR"
            }
        }
    }
    """
    Then the response status is "BAD_REQUEST"

  Scenario: As a user I want to send a DPI print message without receiver address information
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "95e6f67a-ad3c-467b-bab7-61234b27a220",
                "standard": "urn:no:difi:digitalpost:xsd:fysisk::print",
                "type": "print",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910077473"
                    }
                }
            ]
        },
        "print": {
            "hoveddokument": "arkivmelding.xml",
            "avsenderId": "avsender910077473",
            "fakturaReferanse": "faktura910077473",
            "mottaker":{
                "navn": "Ola Nordmann",
                "land": "Norway"
            },
            "utskriftsfarge" : "SORT_HVIT",
            "posttype": "B_OEKONOMI",
            "retur":{
                "mottaker":{
                    "navn": "Dr. Sprøyte Gal",
                    "adresselinje1": "Sadistveien 1",
                    "adresselinje2": "argh",
                    "adresselinje3": "uff",
                    "adresselinje4": "off",
                    "postnummer": "",
                    "poststed": "",
                    "land": "Norge"
                },
                "returhaandtering": "DIREKTE_RETUR"
            }
        }
    }
    """
    Then the response status is "BAD_REQUEST"
