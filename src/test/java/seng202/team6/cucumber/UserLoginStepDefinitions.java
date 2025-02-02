package seng202.team6.cucumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.sql.SQLException;
import seng202.team6.managers.AuthenticationManager;
import seng202.team6.managers.DatabaseManager;
import seng202.team6.enums.AuthenticationResponse;

public class UserLoginStepDefinitions {

  private DatabaseManager databaseManager;
  private AuthenticationManager authenticationManager;
  private String username;
  private String password;

  @Before
  public void setup() throws SQLException {
    databaseManager = new DatabaseManager();
    authenticationManager = new AuthenticationManager(databaseManager);
  }

  @After
  public void close() {
    databaseManager.teardown();
  }

  @Given("the user is not authenticated and is logging in")
  public void theUserIsNotAuthenticatedAndIsLoggingIn() {
    authenticationManager.setAuthenticatedUser(null);
  }

  @When("the user enters an invalid username or password combination")
  public void theUserEntersAnInvalidUsernameOrPasswordCombination() {
    username = "MyAccount";
    password = "invalidpass";

  }

  @When("the user enters a correct username and password combination")
  public void theUserEntersACorrectUsernameAndPasswordCombination() {
    username = "MyAccount";
    password = "ValidPassword1!";
    authenticationManager.validateRegistration(username, password, password);
  }

  @Then("the account is logged in")
  public void theAccountIsLoggedIn() {
    AuthenticationResponse response = authenticationManager.validateLoginPassword(username, password);
    assertEquals(AuthenticationResponse.LOGIN_SUCCESS, response);
    assertTrue(authenticationManager.isAuthenticated());
    assertEquals(username, authenticationManager.getAuthenticatedUsername());
  }

  @Then("the account is not logged in")
  public void theAccountIsNotLoggedIn() {

    AuthenticationResponse response = authenticationManager.validateLoginPassword(username, password);
    assertEquals(AuthenticationResponse.INVALID_LOGIN_USERNAME, response);
    assertFalse(authenticationManager.isAuthenticated());
    assertNotEquals(username, authenticationManager.getAuthenticatedUsername());
  }
}