Feature: Receiving a Next Move DPH message

  Background:
    Given a "GET" request to "http://localhost:9099/info/her-id:8143154" will respond with status "200" and the following "application/json" in "/restmocks/info/her-id_8143154.json"
    And a "GET" request to "http://localhost:9099/identifier/her-id:8143541/process/urn:no:difi:profile:helse:response:ver1.0?conversationId=f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71" will respond with status "200" and the following "application/json" in "/restmocks/identifier/her-id_8143541-response.json"
    And a "GET" request to "http://localhost:9099/virksert/991825827" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And NHN prepares a message with the following SBD:
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
                        "instanceIdentifier": "c9e4a7b2-1d3f-4a5e-b8c6-7d9e0f1a2b31",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71",
                "standard": "urn:no:difi:helse:xsd:schema::dialogmelding",
                "type": "dialogmelding",
                "typeVersion": "1.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "her-id:8143154"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "nhn-actorid",
                        "value": "her-id:8143541"
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
            }
        }
    }
    """
    And appends a file named "dialogmelding.xml" with mimetype="application/xml":
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
    And the NHN adapter has the message available with id "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e"
    And the application checks for new DPH messages

  Scenario: As a user I want to receive a DPH message

    Given I peek and lock a message
    And I pop the locked message
    And I remove the message
    Then the received SBD matches the incoming SBD:
    And I have an ASIC that contains a file named "dialogmelding.xml" with mimetype="application/xml":
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
    And the NHN adapter message with id "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e" is marked as read
    And a DPH message is sent to the NHN adapter
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
          "standard" : "urn:no:difi:helse:xsd::dialogmelding_kvittering",
          "typeVersion" : "2.0",
          "instanceIdentifier" : "19efbd4c-413d-4e2c-bbc5-257ef4a65b38",
          "type" : "dialogmelding_kvittering",
          "creationDateAndTime" : "2019-03-25T12:38:18+01:00"
        },
        "businessScope" : {
          "scope" : [ {
            "type" : "ConversationId",
            "instanceIdentifier" : "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71",
            "identifier" : "urn:no:difi:profile:helse:response:ver1.0",
            "scopeInformation" : [ {
              "expectedResponseDateTime" : "2019-03-26T12:38:23+01:00"
            } ]
          } ]
        }
      },
      "dialogmelding_kvittering" : {
        "relatedToMessageId" : "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e",
        "status" : "OK"
      }
    }
    """
    And the message statuses for the conversation with id = "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71" are:
    """
    {
      "content" : [ {
        "id" : 3,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "messageId" : "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71",
        "conversationId" : "c9e4a7b2-1d3f-4a5e-b8c6-7d9e0f1a2b31",
        "convId" : 2
      }, {
        "id" : 4,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT",
        "messageId" : "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71",
        "conversationId" : "c9e4a7b2-1d3f-4a5e-b8c6-7d9e0f1a2b31",
        "convId" : 2
      }, {
        "id" : 9,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT",
        "messageId" : "f3b8d2e1-5c4a-4b7e-8d9f-0a1b2c3d4e71",
        "conversationId" : "c9e4a7b2-1d3f-4a5e-b8c6-7d9e0f1a2b31",
        "convId" : 2
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
      "totalElements" : 3,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "unsorted" : false,
        "sorted" : true,
        "empty" : false
      },
      "numberOfElements" : 3,
      "first" : true,
      "empty" : false
    }
    """
