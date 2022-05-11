Feature: Receiving a FIKS IO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910077473/process/no.digdir.einnsyn.v1?conversationId=21efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473-einnsyn_fiksio.json"
    And FIKS IO prepares a message with messageId "21efbd4c-413d-4e2c-bbc5-257ef4a65a91"
    And appends a file named "primary.html" with mimetype="text/html":
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And appends a file named "before_the_law.txt" with mimetype="text/plain":
    """
    Before the law sits a gatekeeper.
    """
    And FIKS IO has the message available for kontoId "47b0c75b-ddb5-447b-88d2-c4030d183fb3" with protocol "no.digdir.einnsyn.v1"
    And the FIKS IO subscriber is registered

  Scenario: As a user I want to receive a DPE message

    Given I peek and lock a message
    And I pop the locked message
    And I remove the message
    And I have an ASIC that contains a file named "primary.html" with mimetype="text/html":
    """
    <h1>Primary document</h1>
    <p>This is the content of the primary document.</p>
    """
    And I have an ASIC that contains a file named "before_the_law.txt" with mimetype="text/plain":
    """
    Before the law sits a gatekeeper.
    """
    And the message statuses for the conversation with id = "21efbd4c-413d-4e2c-bbc5-257ef4a65a91" are:
    """
    {
      "content" : [ {
        "id" : 10,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "OPPRETTET",
        "messageId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "convId" : 9
      }, {
        "id" : 11,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_MOTTATT",
        "messageId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "convId" : 9
      }, {
        "id" : 12,
        "lastUpdate" : "2019-03-25T12:38:23+01:00",
        "status" : "INNKOMMENDE_LEVERT",
        "messageId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "conversationId" : "21efbd4c-413d-4e2c-bbc5-257ef4a65a91",
        "convId" : 9
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