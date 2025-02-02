package seng202.team6.gui;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import seng202.team6.managers.ManagerContext;

/**
 * Controller for the user manual page. Loads a different index if you are admin.
 */
public class HelpController extends Controller {

  private static final Logger log = LogManager.getLogger(HelpController.class);
  @FXML
  private WebView webView;

  /**
   * Constructor.
   *
   * @param managerContext the manager context
   */
  public HelpController(ManagerContext managerContext) {
    super(managerContext);
  }

  @FXML
  private void initialize() {
    String url = "";
    try {
      url = getClass().getResource("/html/ManualIndex.html").toExternalForm();
      if (getManagerContext().getAuthenticationManager().isAdmin()) {
        url = getClass().getResource("/html/AdminIndex.html").toExternalForm();
      }

    } catch (NullPointerException e) {
      log.info("Failed to load HTML index file");
      log.info(e.getMessage());
    }
    webView.getEngine()
        .setUserStyleSheetLocation(
            getClass().getResource("/css/helppages.css").toExternalForm());
    webView.getEngine().load(url);
  }
}
