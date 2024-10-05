package seng202.team6.managers;

/**
 * ManagerContext is simply a bag of managers. Members are public because of this
 *
 */
public class ManagerContext {

  final private DatabaseManager databaseManager;

  final private GuiManager guiManager;

  final private AuthenticationManager authenticationManager;

  public DatabaseManager getDatabaseManager() {
    return databaseManager;
  }

  public seng202.team6.managers.GuiManager getGuiManager() {
    return guiManager;
  }

  public AuthenticationManager getAuthenticationManager() {
    return authenticationManager;
  }

  /**
   * Constructor for ManagerContext
   *
   * @param databaseManager       database manager
   * @param guiManager            interface manager
   * @param authenticationManager authentication manager
   */
  public ManagerContext(
      DatabaseManager databaseManager,
      GuiManager guiManager,
      AuthenticationManager authenticationManager
  ) {
    this.databaseManager = databaseManager;
    this.guiManager = guiManager;
    this.authenticationManager = authenticationManager;
  }
}
