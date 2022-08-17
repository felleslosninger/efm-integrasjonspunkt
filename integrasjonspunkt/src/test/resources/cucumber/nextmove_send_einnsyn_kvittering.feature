Feature: Sending a Next Move eInnsyn kvittering message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/0192:910075935/process/urn:no:difi:profile:einnsyn:response:ver1.0?conversationId=217b18b4-a486-4999-8979-ef0957e9bdd6" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935-einnsyn_response.json"
    And the Noark System is disabled

  Scenario: As a user I want to send an eInnsyn kvittering
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
                        "identifier": "urn:no:difi:profile:einnsyn:response:ver1.0",
                        "instanceIdentifier": "217b18b4-a486-4999-8979-ef0957e9bdd6",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "217b18b4-a486-4999-8979-ef0957e9bdd6",
                "standard": "urn:no:difi:einnsyn:xsd::einnsyn_kvittering",
                "type": "einnsyn_kvittering",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075935"
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
        "einnsyn_kvittering" : {
          "dokumentId" : "5ad82ad7-46e3-40d4-ac77-a98ad97f605f",
          "status" : "publisert",
          "referanseType": "publisering"
        }
    }
    """
    And the response status is "OK"
    And I send the message
    Then a POST to the ServiceBus is initiated with:
    """
    {
      "sbd" : {
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
              "value" : "0192:910075935",
              "authority" : "iso6523-actorid-upis"
            }
          } ],
          "documentIdentification" : {
            "standard" : "urn:no:difi:einnsyn:xsd::einnsyn_kvittering",
            "typeVersion" : "2.0",
            "instanceIdentifier" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
            "type" : "einnsyn_kvittering",
            "creationDateAndTime" : "2019-03-25T11:35:00+01:00"
          },
          "businessScope" : {
            "scope" : [ {
              "type" : "ConversationId",
              "instanceIdentifier" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
              "identifier" : "urn:no:difi:profile:einnsyn:response:ver1.0",
              "scopeInformation" : [ {
                "expectedResponseDateTime" : "2019-05-10T00:31:52+01:00"
              } ]
            } ]
          }
        },
        "einnsyn_kvittering" : {
          "dokumentId" : "5ad82ad7-46e3-40d4-ac77-a98ad97f605f",
          "status" : "publisert",
          "referanseType": "publisering"
        }
      }
    }
    """
    And the message statuses for the conversation with id = "217b18b4-a486-4999-8979-ef0957e9bdd6" are:
    """
    {
      "content" : [ {
        "id" : 167,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
        "messageId" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
        "convId" : 166
      }, {
        "id" : 170,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "SENDT",
        "conversationId" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
        "messageId" : "217b18b4-a486-4999-8979-ef0957e9bdd6",
        "convId" : 166
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
      "totalElements" : 2,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "first" : true,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 2,
      "empty" : false
    }
    """