Feature: Onboarding with new key
  As a user, I want to be able to join the network

  @ignore
  Scenario: Generate random identifier
    When I open application
    Then I open drawer
    Then I click 'identifiers'
    Then I click 'New identifier'
    Then "load identifier" text is presented
    Then I click 'from entropy'
    Then I submit webview passing succesfully the captcha
    Then I fill input with testTag "alias" with value "test random"
    Then I click 'save'
    Then I open drawer
    Then I click 'identifiers'
    Then "test random@delog.in" text is presented


  Scenario: Generate random identifier
    When I open application
    Then I open drawer
    Then I click 'identifiers'
    Then I click 'New identifier'
    Then "load identifier" text is presented
    Then I click on testTag 'disable_server'
    Then I click 'from entropy'
    Then I fill input with testTag "alias" with value "test random"
    Then I click 'save'
    #Then I open drawer
    #Then I click 'identifiers'
    #Then I wait '10000'
    Then "test random" text is presented

  Scenario: Publish a message
    When I open application
    Then I open drawer
    Then I click 'thread'
    Then I click 'compose'
    Then I fill input with testTag "draft_edit_text_field" with value "test message"
    Then I click 'save'
    Then "test message" text is presented
    Then "publish" text is presented
    Then I open drawer
    Then I click 'draft'
    Then "test message" text is presented
    Then "compose" text is presented
    Then I click 'test message'
    Then "test message" text is presented
    Then I click 'save'
    Then "test message" text is presented
    Then I click 'publish'
    Then I wait '1000'
    Then I click 'dismiss'
    Then I click 'publish'
    Then I click on testTag 'confirm_publish'
