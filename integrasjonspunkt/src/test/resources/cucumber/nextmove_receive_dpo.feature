Feature: Receiving a Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/974720760?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3&conversationId=37efbd4c-413d-4e2c-bbc5-257ef4a65a45" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473/process/urn:no:difi:profile:arkivmelding:response:ver1.0?conversationId=37efbd4c-413d-4e2c-bbc5-257ef4a65a45" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473-arkivmelding_response.json"
    And Altinn prepares a message with the following SBD:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": "2019-05-10T01:31:52+02:00"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b919",
                "standard": "urn:no:difi:arkivmelding:xsd::arkivmelding",
                "type": "arkivmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075918"
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
          "securityLevel": 3
        }
    }
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
    And Altinn sends the message
    And the application checks for new DPO messages

  Scenario: As a user I want to receive a DPO message

    Given I peek and lock a message
    And I pop the locked message
    And I remove the message
    Then the received SBD matches the incoming SBD:
    And I have an ASIC that contains a file named "primary.html" with mimetype="text/html":
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And I have an ASIC that contains a file named "before_the_law.txt" with mimetype="text/plain":
    """
    Before the law sits a gatekeeper.
    """
    And the message statuses for the conversation with id = "ff88849c-e281-4809-8555-7cd54952b919" are:
    """
    {
      "content" : [ {
        "id" : 10,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952b919",
        "convId" : 9
      }, {
        "id" : 11,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT",
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952b919",
        "convId" : 9
      }, {
        "id" : 12,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT",
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952b919",
        "convId" : 9
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
