package seng202.team0.managers;

import seng202.team0.model.Role;

/**
 * Authentication Manager (MORE DETAIL HERE!)
 */
public class AuthenticationManager {
  private boolean authenticated;
  private boolean admin;
  private String username;
  public AuthenticationManager() {
    this.authenticated = false;
    this.admin = false;
  }
  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }
  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  public boolean isAdmin() {
    return admin;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
  public void logout() {
    authenticated = false;
    admin = false;
    username = "";
  }

  // Getters and Setters
  public Role getRole(){return null;} // TODO Implement me!
  public String getUserID(){return null;} // TODO Implement me!
}
