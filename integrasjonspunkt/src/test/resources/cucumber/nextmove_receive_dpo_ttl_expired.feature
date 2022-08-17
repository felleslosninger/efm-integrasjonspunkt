Feature: Receiving an expired Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/0192:974720760?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/identifier/0192:910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And the Noark System is disabled
    And Altinn prepares a message with the following SBD:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": "2019-03-25T11:00:00+01:00"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "2507fcb8-6543-4c5f-ac06-f5df75bb677e",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b924",
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

  Scenario: As a user I want an expired DPO message to be stopped

    Given the application checks for new DPO messages
    Then the message statuses for the conversation with id = "ff88849c-e281-4809-8555-7cd54952b924" are:
    """
    {
      "content" : [ {
        "id" : 25,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "conversationId" : "2507fcb8-6543-4c5f-ac06-f5df75bb677e",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952b924",
        "convId" : 24
      }, {
        "id" : 26,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "LEVETID_UTLOPT",
        "description" : "Levetiden for meldingen er utgått. Må sendes på nytt",
        "conversationId" : "2507fcb8-6543-4c5f-ac06-f5df75bb677e",
        "messageId" : "ff88849c-e281-4809-8555-7cd54952b924",
        "convId" : 24
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
      "totalElements" : 2,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 2,
      "first" : true,
      "empty" : false
    }
    """
