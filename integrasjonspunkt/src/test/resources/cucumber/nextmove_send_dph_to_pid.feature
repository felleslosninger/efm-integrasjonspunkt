Feature: Sending a Next Move DPH message to an individual using PID

#  Background:
#    Given a "GET" request to "http://localhost:9099/identifier/09118532322?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322.json"
#    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
#    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?securityLevel=3&conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
#    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
#    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario: As a user I want to send a DPH message
    Given I POST the following message:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "identifier": "urn:no:difi:profile:helse:fastlege:ver1.0",
                        "instanceIdentifier": "0d7779d1-a1ec-4233-8f12-926fbb216649",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2026-04-27T12:35:00+01:00",
                "instanceIdentifier": "a8976faf-0d5e-4210-b424-439501df9c41",
                "standard": "urn:no:difi:helse:json:schema::dialogmelding",
                "type": "dialogmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "fastlege-for:30878199614"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "her-id:8143548"
                    }
                }
            ]
        },
        "dialogmelding": {
          "hoveddokument": "dialogmelding.json",
          "metadataFiler": {
            "svada.pdf": "Et dokument med bare svada"
          }
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "dialogmelding.json" with mimetype "application/json" and title "Dialogmelding" with the following body:
    """
    {
        "notat": {
            "temaBeskrivelse": "Test tema",
            "innhold": "Testing 1 2 3"
        }
    }
    """
    And I upload a file named "svada.pdf" with mimetype "application/pdf" and title "Vedlegg"
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
          "standard" : "urn:fdc:digdir.no:2020:innbyggerpost:xsd::innbyggerpost##urn:fdc:digdir.no:2020:innbyggerpost:schema:digital::1.0",
          "typeVersion" : "1.0",
          "instanceIdentifier" : "ff88849c-e281-4809-8555-7cd54952b921",
          "type" : "digital",
          "creationDateAndTime" : "2019-03-25T11:38:23Z"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
            "identifier" : "urn:fdc:digdir.no:2020:profile:egovernment:innbyggerpost:digital:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-05-09T23:31:52Z"
            } ]
          } ]
        }
      },
      "digital" : {
        "avsender" : {
          "virksomhetsidentifikator" : {
            "authority" : "iso6523-actorid-upis",
            "value" : "0192:910077473"
          },
          "avsenderidentifikator" : "avsender910077473",
          "fakturaReferanse" : "faktura910077473"
        },
        "mottaker" : {
          "postkasseadresse" : "dummy"
        },
        "dokumentpakkefingeravtrykk" : {
          "digestMethod" : "http://www.w3.org/2001/04/xmlenc#sha256",
          "digestValue" : "dummy"
        },
        "maskinportentoken" : "DummyMaskinportenToken",
        "sikkerhetsnivaa" : 3,
        "virkningstidspunkt" : "2019-05-11T22:00:00Z",
        "aapningskvittering" : false,
        "ikkesensitivtittel" : "Min supertittel",
        "spraak" : "NO",
        "varsler" : { }
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
        <mottaker>
            <person>
                <postkasseadresse>dummy</postkasseadresse>
            </person>
        </mottaker>
        <avsender>
            <organisasjon authority="iso6523-actorid-upis">0192:910077473</organisasjon>
            <avsenderidentifikator>avsender910077473</avsenderidentifikator>
            <fakturaReferanse>faktura910077473</fakturaReferanse>
        </avsender>
        <hoveddokument href="arkivmelding.xml" mime="application/xml">
            <tittel lang="NO">Arkivmelding</tittel>
            <data href="test.txt" mime="text/plain"/>
        </hoveddokument>
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
