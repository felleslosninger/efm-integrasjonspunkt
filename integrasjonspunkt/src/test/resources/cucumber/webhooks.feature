Feature: Webhooks

  Background:
    Given the endpoint "/my/test/endpoint" accepts posts
    And I create the following webhook subscription:
    """
    {
      "name": "My test webhook",
      "pushEndpoint": "http://127.0.0.1:9800/my/test/endpoint",
      "resource": "all",
      "event": "all"
    }
    """
    Then the following ping message is posted to "/my/test/endpoint":
    """
    {
      "createdTs" : "2019-03-25T12:38:23+01:00",
      "event" : "ping"
    }
    """

  Scenario: As a user I want to register a webhook and receive pushes to it
    Given the following message status is published:
      """
      {
        "conversationId" : "bb8bb41a-c559-47ed-84f1-1846ece5d590",
        "messageId" : "bb8bb41a-c559-47ed-84f1-1846ece5d591",
        "direction" : "INCOMING",
        "status" : "MOTTATT",
        "description" : "some description"
      }
      """
    Then the following message is posted to "/my/test/endpoint":
      """
       {
        "createdTs" : "2019-03-25T12:38:23+01:00",
        "resource" : "messages",
        "event" : "status",
        "conversationId" : "bb8bb41a-c559-47ed-84f1-1846ece5d590",
        "messageId" : "bb8bb41a-c559-47ed-84f1-1846ece5d591",
        "direction" : "INCOMING",
        "status" : "MOTTATT",
        "description" : "some description"
      }
      """