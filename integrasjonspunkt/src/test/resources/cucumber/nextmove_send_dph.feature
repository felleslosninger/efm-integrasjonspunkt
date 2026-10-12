Feature: Sending a Next Move DPH message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/her-id:8143541/process/urn:no:difi:profile:helse:helse:ver1.0?conversationId=d5a1f2c3-7b8e-4d6f-9a0b-1c2d3e4f5a61" will respond with status "200" and the following "application/json" in "/restmocks/identifier/her-id_8143541-helse.json"
    And a "GET" request to "http://localhost:9099/info/her-id:8143154" will respond with status "200" and the following "application/json" in "/restmocks/info/her-id_8143154.json"
    And a "GET" request to "http://localhost:9099/virksert/991825827" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario: As a user I want to send a DPH message
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
                        "identifier": "urn:no:difi:profile:helse:helse:ver1.0",
                        "instanceIdentifier": "d5a1f2c3-7b8e-4d6f-9a0b-1c2d3e4f5a61",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "e7c2a9b4-3d1f-4e8a-b6c5-2f9d8e7a6b52",
                "standard": "urn:no:difi:helse:xsd:schema::dialogmelding",
                "type": "dialogmelding",
                "typeVersion": "1.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "her-id:8143541"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "her-id:8143154"
                    }
                }
            ]
        },
        "dialogmelding": {
            "hoveddokument": "dialogmelding.xml",
            "pasient": {
                "fnr": "09118532322",
                "fornavn": "Raff",
                "etternavn": "Raffen"
            },
            "metadataFiler": {
                "vedlegg.pdf": {
                    "description": "Vedlegg til dialogmeldingen"
                }
            }
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "dialogmelding.xml" with mimetype "application/xml" and title "Dialogmelding" with the following body:
    """
    <Dialogmelding xmlns="http://www.kith.no/xmlstds/dialog/2013-01-23">
        <Notat>
            <TemaKodet V="6" S="2.16.578.1.12.4.1.1.7322" DN="Henvendelse om pasient"/>
            <Tema>Test tema</Tema>
            <TekstNotatInnhold xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">Testing 1 2 3
            </TekstNotatInnhold>
        </Notat>
    </Dialogmelding>
    """
    And I upload a file named "vedlegg.pdf" with mimetype "application/pdf" and title "Vedlegg" with the following body:
    """
    Dette er vedlegget.
    """
    And I send the message
    Then a DPH message is sent to the NHN adapter
    And the sent message's SBD is:
    """
    {
      "standardBusinessDocumentHeader" : {
        "headerVersion" : "1.0",
        "sender" : [ {
          "identifier" : {
            "value" : "her-id:8143154",
            "authority" : "nhn-actorid"
          }
        } ],
        "receiver" : [ {
          "identifier" : {
            "value" : "her-id:8143541",
            "authority" : "nhn-actorid"
          }
        } ],
        "documentIdentification" : {
          "standard" : "urn:no:difi:helse:xsd:schema::dialogmelding",
          "typeVersion" : "1.0",
          "instanceIdentifier" : "e7c2a9b4-3d1f-4e8a-b6c5-2f9d8e7a6b52",
          "type" : "dialogmelding",
          "creationDateAndTime" : "2019-03-25T11:35:00+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "d5a1f2c3-7b8e-4d6f-9a0b-1c2d3e4f5a61",
            "identifier" : "urn:no:difi:profile:helse:helse:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-05-10T00:31:52+01:00"
            } ]
          } ]
        }
      },
      "dialogmelding" : {
        "hoveddokument" : "dialogmelding.xml",
        "pasient" : {
          "fnr" : "09118532322",
          "fornavn" : "Raff",
          "etternavn" : "Raffen"
        },
        "metadataFiler" : {
          "vedlegg.pdf" : {
            "description" : "Vedlegg til dialogmeldingen"
          }
        }
      }
    }
    """
    And the sent message contains the following files:
      | filename          | content type    |
      | dialogmelding.xml | application/xml |
      | vedlegg.pdf       | application/pdf |
    And the XML content of the file named "dialogmelding.xml" is:
    """
    <Dialogmelding xmlns="http://www.kith.no/xmlstds/dialog/2013-01-23">
        <Notat>
            <TemaKodet V="6" S="2.16.578.1.12.4.1.1.7322" DN="Henvendelse om pasient"/>
            <Tema>Test tema</Tema>
            <TekstNotatInnhold xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">Testing 1 2 3
            </TekstNotatInnhold>
        </Notat>
    </Dialogmelding>
    """
    And the content of the file named "vedlegg.pdf" is:
    """
    Dette er vedlegget.
    """
