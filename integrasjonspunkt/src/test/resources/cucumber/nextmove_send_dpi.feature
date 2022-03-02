@Ignore
Feature: Sending a Next Move DPI message (XML SOAP)

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/09118532322?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322.json"
    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
    And a "GET" request to "http://localhost:9099/identifier/09118532322/process/urn:no:difi:profile:digitalpost:info:ver1.0?securityLevel=3&conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322-info.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And the Noark System is disabled
    And a SOAP request to "http://localhost:3193/dpi/9908:910077473/9908:910077473" will respond with the following payload:
    """
    <dummy></dummy>
    """

  Scenario: As a user I want to send a DPI message
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
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b921",
                "standard": "urn:no:difi:digitalpost:xsd:digital::digital",
                "type": "digital",
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
        "digital": {
          "sikkerhetsnivaa": 3,
          "hoveddokument": "arkivmelding.xml",
          "avsenderId": "avsender910077473",
          "fakturaReferanse": "faktura910077473",
          "tittel" : "Min supertittel",
          "spraak": "NO",
          "digitalPostInfo": {
            "virkningsdato": "2019-05-12",
            "aapningskvittering": "false"
          },
          "varsler": {
            "epostTekst": "Varseltekst",
            "smsTekst": "Varseltekst"
          },
          "metadataFiler": {
            "arkivmelding.xml": "test.txt"
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
    Then an upload to Digipost is initiated with:
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <ns3:StandardBusinessDocument xmlns:ns3="http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader"
                                  xmlns:ns5="http://www.w3.org/2000/09/xmldsig#"
                                  xmlns:ns9="http://begrep.difi.no/sdp/schema_v10">
        <ns3:StandardBusinessDocumentHeader>
            <ns3:HeaderVersion>1.0</ns3:HeaderVersion>
            <ns3:Sender>
                <ns3:Identifier Authority="urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908">9908:910077473</ns3:Identifier>
            </ns3:Sender>
            <ns3:Receiver>
                <ns3:Identifier Authority="urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908">9908:987464291</ns3:Identifier>
            </ns3:Receiver>
            <ns3:DocumentIdentification>
                <ns3:Standard>urn:no:difi:sdp:1.0</ns3:Standard>
                <ns3:TypeVersion>1.0</ns3:TypeVersion>
                <ns3:InstanceIdentifier>0bfe4b94-ea1a-465f-95b7-5d06bc986dfb</ns3:InstanceIdentifier>
                <ns3:Type>digitalPost</ns3:Type>
                <ns3:CreationDateAndTime>2019-03-25T11:35:00+01:00</ns3:CreationDateAndTime>
            </ns3:DocumentIdentification>
            <ns3:BusinessScope>
                <ns3:Scope>
                    <ns3:Type>ConversationId</ns3:Type>
                    <ns3:InstanceIdentifier>97efbd4c-413d-4e2c-bbc5-257ef4a61212</ns3:InstanceIdentifier>
                    <ns3:Identifier>urn:no:difi:sdp:1.0</ns3:Identifier>
                </ns3:Scope>
            </ns3:BusinessScope>
        </ns3:StandardBusinessDocumentHeader>
        <ns9:digitalPost>
            <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
                <SignedInfo>
                    <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                    <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
                    <Reference URI="">
                        <Transforms>
                            <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                        </Transforms>
                        <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                        <DigestValue>2APvYXY7DRGWkcVqTVzXiKo7EadN8cPubdOAk70+79g=</DigestValue>
                    </Reference>
                </SignedInfo>
                <SignatureValue>e0Yb5ebHWMHQSpSR2OPPCfNT1Fn+C9dWRLcl5HVbofME6odV6MbBHry+9kjJgv3BpAJOJiY4tALq
                    kc2thRm4v9Y9nIeUsSSPqmFoq8BKGDDgecy01OcYX07Sf/Ca6Er4Z6FDt5u070fKBoPBNAQZN0e8
                    XY8BRYUMtX+mc1QP1Z5a8CbhZZnTZMECj4IWmyZHS4eZF88C4hK8bvhb0d4BIDu9rkTeZlF0CysE
                    i+891uaSyAKLj7RkTrahrfD0Kl+8gNMacTPZ5pHriMlmXeRviPeIQAoot62pUyQImZVR7jQVrk+7
                    qCwAj5bBFmRnICxguS8dV8LRK6yCtUv8bSuf/g==
                </SignatureValue>
                <KeyInfo>
                    <X509Data>
                        <X509Certificate>MIICujCCAaKgAwIBAgIEXIe4ADANBgkqhkiG9w0BAQsFADAeMRwwGgYDVQQDDBNESUZJIHRlc3Qg
                            OTc0NzIwNzYwMCAXDTE5MDMxMjEzNDUzNloYDzIxMTkwMzEyMTM0NTM2WjAeMRwwGgYDVQQDDBNE
                            SUZJIHRlc3QgOTc0NzIwNzYwMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApzVFU9nR
                            s5Az+dM6ruoh6EER6ByiH5GhIwXEr8DBXV0R17CSu+gnwjuJPAjpOtBQp8xN45W7zNoIopMUOc5i
                            OS/6dRoyaoTpkpjA27P6X4RI/jM5Fl8rARwfHNKe0oQ3knvNkLeqw15xgubHp2Mkicc/5kiUwMjn
                            OSHoQGd0k/JLsCbZrclCejyxhsSyjHFtzQf9ElzzEEOoEHdWk7rrwo97rxdlaHeX6fBKK/muY23W
                            dDw/OL6OlE+0dQKl3+Mdt2/IZmuaJMw8VwJQ8xH3nV7kjQMiIaTvZTY892+d5Xr2jShmqkNrh/E7
                            V/+ex5SI2/eLwRbKeg1V8TAK7OuXmQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBB5A1QnHyGGLvq
                            FpUVPKhRDW41AnzlwcVwKcktm5jQxVVoQ0WDR1BSgyjJo81IeUQU5/nvmZx6bOqLvIzyA6weORyu
                            C8KWmW4tj9eBlLpMcSrGjpNALV1+JtOTCf+18/+/DHcu7bUsvCr7dvnUEnau2JNhPlyN6MGROti1
                            vNGZo1OonN4ARHc8FgdsPsdvRDFPEQp5bhUwxUjnpW0iQUcpWUYuvW8Ht7yyiRDzhEYP6qlr4JB9
                            ERujM6CfBCVXOUaLWYZp1BGfzV9r5j20N2y9b139niOdNm5/bOM3KayTfNCkLv/R7WW3DX3c5+/J
                            q33LstwTkodlqC8We/C+8ycb
                        </X509Certificate>
                    </X509Data>
                </KeyInfo>
            </Signature>
            <ns9:avsender>
                <ns9:organisasjon authority="iso6523-actorid-upis">9908:910077473</ns9:organisasjon>
                <ns9:avsenderidentifikator>avsender910077473</ns9:avsenderidentifikator>
                <ns9:fakturaReferanse>faktura910077473</ns9:fakturaReferanse>
            </ns9:avsender>
            <ns9:mottaker>
                <ns9:person>
                    <ns9:personidentifikator>09118532322</ns9:personidentifikator>
                    <ns9:postkasseadresse>dummy</ns9:postkasseadresse>
                </ns9:person>
            </ns9:mottaker>
            <ns9:digitalPostInfo>
                <ns9:virkningstidspunkt>2019-04-23T11:00:42.408+01:00</ns9:virkningstidspunkt>
                <ns9:aapningskvittering>false</ns9:aapningskvittering>
                <ns9:sikkerhetsnivaa>3</ns9:sikkerhetsnivaa>
                <ns9:ikkeSensitivTittel lang="NO" xmlns:ns9="http://begrep.difi.no/sdp/schema_v10">Min supertittel</ns9:ikkeSensitivTittel>
                <ns9:varsler/>
            </ns9:digitalPostInfo>
            <ns9:dokumentpakkefingeravtrykk>
                <ns5:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                <ns5:DigestValue>WT7hUO2ZMoRSKXFn2yrvz0+77W6oEwFhkHOx4LnvOq8=</ns5:DigestValue>
            </ns9:dokumentpakkefingeravtrykk>
        </ns9:digitalPost>
    </ns3:StandardBusinessDocument>
    """
    And the sent message contains the following files:
      | filename         | content type |
      | arkivmelding.xml |              |
      | test.txt         |              |
      | manifest.xml     |              |

    And the XML content of the file named "manifest.xml" is:
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <manifest xmlns="http://begrep.difi.no/sdp/schema_v10">
        <mottaker>
            <person>
                <personidentifikator>09118532322</personidentifikator>
                <postkasseadresse>dummy</postkasseadresse>
            </person>
        </mottaker>
        <avsender>
            <organisasjon authority="iso6523-actorid-upis">9908:910077473</organisasjon>
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
