Feature: Sending a nextmove DPFIO message

  Background:
    And a "GET" request to "http://localhost:9099/identifier/910075935/process/no.digdir.einnsyn.v1?conversationId=21efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935-einnsyn_fiksio.json"
    And the Noark System is disabled

  Scenario: As a user I want to send a DPE message
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
                        "identifier": "no.digdir.einnsyn.v1",
                        "instanceIdentifier": "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "20c8849c-e281-4809-8555-7cd54952b917",
                "standard": "no.digdir.einnsyn.v1",
                "type": "fiksio",
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
        "fiksio" : {
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then a message is sent to FIKS IO with kontoId "e0a0fba9-b7cf-44f8-99a6-a7f04b9e0347" and protocol "no.digdir.einnsyn.v1"
    And the sent message contains the following files:
      | filename     | content type |
      | test.txt     | text/plain   |
    And the content of the file named "test.txt" is:
    """
    Testing 1 2 3
    """
    And the message statuses for the conversation with id = "20c8849c-e281-4809-8555-7cd54952b917" are:
    """
    {
      "content" : [ {
        "id" : 167,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "messageId" : "20c8849c-e281-4809-8555-7cd54952b917",
        "convId" : 166
      }, {
        "id" : 170,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "SENDT",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "messageId" : "20c8849c-e281-4809-8555-7cd54952b917",
        "convId" : 166
      }, {
        "id" : 171,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "MOTTATT",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "messageId" : "20c8849c-e281-4809-8555-7cd54952b917",
        "convId" : 166
      }, {
        "id" : 172,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "LEVERT",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "messageId" : "20c8849c-e281-4809-8555-7cd54952b917",
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
      "totalElements" : 4,
      "totalPages" : 1,
      "size" : 10,
      "number" : 0,
      "first" : true,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 4,
      "empty" : false
    }
  """
