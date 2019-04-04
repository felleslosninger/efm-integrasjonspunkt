Feature: Validating Time to live in message

  Scenario: I want to check if expectedResponseDateTime is after currentDateTime
  Given: I have obtained expectedResponseDateTime and currentDateTime
  And I want to compare them.
	
	
  Scenario: I want to check if expectedResponseDateTime is expired
  Given I have obtained expectedResponseDateTime 
  And compare it to current system time
  