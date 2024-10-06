package seng202.team6.gui.popup;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import seng202.team6.gui.Controller;
import seng202.team6.gui.controls.container.AddRemoveCardsContainer;
import seng202.team6.managers.ManagerContext;
import seng202.team6.model.Vineyard;
import seng202.team6.model.VineyardTour;
import seng202.team6.service.VineyardToursService;

public class AddToTourPopupController extends Controller {

  private final VineyardToursService vineyardToursService;
  private final Vineyard vineyard;
  @FXML
  ScrollPane vineyardToursContainer;
  private AddRemoveCardsContainer<VineyardTour> addRemoveCardsContainer;

  /**
   * Constructs a new AddToTourPopupController
   *
   * @param context The manager context
   */
  public AddToTourPopupController(ManagerContext context, Vineyard vineyard) {
    super(context);
    vineyardToursService = new VineyardToursService(context.getAuthenticationManager(),
        context.getDatabaseManager());
    this.vineyard = vineyard;
    bindToVineyardToursService();
  }

  @Override
  public void init() {
    addRemoveCardsContainer = new AddRemoveCardsContainer<>(
        vineyardToursContainer.viewportBoundsProperty(),
        vineyardToursContainer.widthProperty());
    vineyardToursContainer.setContent(addRemoveCardsContainer);
    vineyardToursService.init();
  }

  @FXML
  void onBackButtonClick() {
    managerContext.getGuiManager().mainController.closePopup();
  }

  private void bindToVineyardToursService() {
    vineyardToursService.getVineyardTours()
        .addListener((ListChangeListener<VineyardTour>) change -> {
          while (change.next()) {
            if (change.wasAdded()) {
              change.getAddedSubList().forEach(vineyardTour -> {
                addRemoveCardsContainer.add(vineyardTour, vineyardTour.nameProperty(),
                    !vineyardToursService.isVineyardInTour(vineyardTour, vineyard),
                    () -> managerContext.getDatabaseManager().getVineyardTourDao()
                        .addVineyard(vineyardTour, vineyard),
                    () -> managerContext.getDatabaseManager().getVineyardTourDao()
                        .removeVineyard(vineyardTour, vineyard));
              });
            }
          }
        });
  }
}
