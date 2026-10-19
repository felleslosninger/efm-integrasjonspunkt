Feature: Receiving a bad sbd

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/974720760?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And Altinn prepares a message with the following SBDH:
    """
    {
      "businessScope": {
        "scope": [
          {
            "scopeInformation": [
              {
                "expectedResponseDateTime": "2019-05-10T01:31:52+02:00"
              }
            ],
            "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
            "instanceIdentifier": "0f5167d5-02ba-4dca-b377-3aeb714dfc6a",
            "type": "ConversationId"
          }
        ]
      },
      "documentIdentification": {
        "creationDateAndTime": "2019-03-25T11:35:00+01:00",
        "instanceIdentifier": "171bfb97-c58a-4f35-a971-a1cd2afdb514",
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

  Scenario: As a user I want a message with a bad sbd to be stopped

    Given the application checks for new DPO messages
    Then the message statuses for the conversation with id = "171bfb97-c58a-4f35-a971-a1cd2afdb514" are:
    """
    {
      "content" : [ ],
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
      "totalPages" : 0,
      "totalElements" : 0,
      "size" : 10,
      "number" : 0,
      "sort" : {
        "sorted" : true,
        "unsorted" : false,
        "empty" : false
      },
      "numberOfElements" : 0,
      "first" : true,
      "empty" : true
    }
    """
