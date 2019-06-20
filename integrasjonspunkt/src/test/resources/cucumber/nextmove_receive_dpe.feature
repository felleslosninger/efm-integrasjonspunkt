Feature: Receiving a Next Move DPE message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075935?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075935.json"
    And a "GET" request to "http://localhost:9099/identifier/974720760?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And ServiceBus prepares a message with the following SBD:
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
                        "identifier": "urn:no:difi:meldingsutveksling:2.0",
                        "instanceIdentifier": "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b916",
                "standard": "urn:no:difi:meldingsutveksling:2.0",
                "type": "innsynskrav",
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
        "innsynskrav": {
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
    And the ServiceBus has the message available
    And the application checks for new DPE messages

  Scenario: As a user I want to receive a DPE message

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
    And the message statuses for the conversation with id = "37efbd4c-413d-4e2c-bbc5-257ef4a65a45" are:
    """
    {
      "content" : [ {
        "id" : 1,
        "convId" : 1,
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET"
      }, {
        "id" : 2,
        "convId" : 1,
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT"
      }, {
        "id" : 3,
        "convId" : 1,
        "conversationId" : "37efbd4c-413d-4e2c-bbc5-257ef4a65a45",
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT"
      } ],
      "last" : true,
      "totalPages" : 1,
      "totalElements" : 3,
      "size" : 10,
      "number" : 0,
      "sort" : [ {
        "direction" : "ASC",
        "property" : "id",
        "ignoreCase" : false,
        "nullHandling" : "NATIVE",
        "ascending" : true,
        "descending" : false
      } ],
      "first" : true,
      "numberOfElements" : 3
    }
    """