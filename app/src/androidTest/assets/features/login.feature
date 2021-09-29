
@Basic
Feature: Login to the Application
Scenario Outline: Successful Login
  Given I start the application
  And I grant permission to access storage
  When I click the home login button
  And I verify the page transition to the login page
  And I enter the login username for user <username>
  And I close the keyboard
  And I enter the login password for user <username>
  And I close the keyboard
  And I click the login button
  Then I verify the page transition to the home page
  Examples:
    | username   |
    | test-admin |
