package seng202.team6.gui;

import java.util.List;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import seng202.team6.gui.controls.ButtonsList;
import seng202.team6.gui.controls.card.AddRemoveCard;
import seng202.team6.gui.controls.card.Card;
import seng202.team6.gui.controls.cardcontent.ItineraryItemCardContent;
import seng202.team6.gui.controls.cardcontent.VineyardCardContent;
import seng202.team6.gui.controls.container.CardsContainer;
import seng202.team6.gui.popup.ErrorPopupController;
import seng202.team6.managers.ManagerContext;
import seng202.team6.model.GeoLocation;
import seng202.team6.model.Vineyard;
import seng202.team6.model.VineyardTour;
import seng202.team6.service.TourPlanningService;
import seng202.team6.service.VineyardService;
import seng202.team6.service.VineyardToursService;
import seng202.team6.util.GeolocationResolver;

/**
 * The controller for manging the vineyard tour planning screen.
 */
public class TourPlanningController extends Controller {

  private final VineyardToursService vineyardToursService;
  private final VineyardService vineyardService;
  private final GeolocationResolver geolocationResolver;
  @FXML
  private VBox planTourTabContainer;
  @FXML
  private HBox noTourSelectedContainer;
  @FXML
  private VBox planTourOptionsContainer;
  @FXML
  private Label viewingTourLabel;
  @FXML
  private ScrollPane vineyardToursContainer;
  @FXML
  private ScrollPane vineyardsContainer;
  @FXML
  private ScrollPane itineraryContainer;
  @FXML
  private WebView webView;
  @FXML
  private TabPane tabPane;
  @FXML
  private Tab viewTourTab;
  @FXML
  private Tab planTourTab;
  private ButtonsList<VineyardTour> vineyardTourButtonsList;
  private CardsContainer<Vineyard> vineyardCardsContainer;
  private CardsContainer<Vineyard> itineraryCardsContainer;
  private LeafletOsmController mapController;
  private TourPlanningService currentTourPlanningService;

  /**
   * Constructs a new TourPlanningController.
   *
   * @param context the manager context
   */
  public TourPlanningController(ManagerContext context) {
    super(context);
    vineyardToursService = new VineyardToursService(managerContext.getAuthenticationManager(),
        managerContext.getDatabaseManager());
    vineyardService = new VineyardService(managerContext.getDatabaseManager());
    geolocationResolver = new GeolocationResolver();
    bindToVineyardToursService();
  }

  /**
   * Binds the vineyard tours service to the UI. The bindings ensure changes to the vineyard tours
   * are reflected in the UI. The listeners will graphically display or remove vineyard tour buttons
   * upon change in the vineyard tours service list.
   */
  private void bindToVineyardToursService() {
    ObservableList<VineyardTour> vineyardTours = vineyardToursService.getVineyardTours();
    vineyardTours.addListener((ListChangeListener<VineyardTour>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(vineyardTour ->
              vineyardTourButtonsList.add(vineyardTour, vineyardTour.nameProperty(),
                  () -> {
                    openVineyardTour(vineyardTour);
                    tabPane.getSelectionModel().select(planTourTab);
                  }));
        }
        if (change.wasRemoved()) {
          change.getRemoved().forEach(vineyardTour -> vineyardTourButtonsList.remove(vineyardTour));
        }
      }
    });
  }

  /**
   * Initializes the controller and its components.
   */
  @Override
  public void init() {
    vineyardTourButtonsList = new ButtonsList<>(
        vineyardToursContainer.viewportBoundsProperty(),
        vineyardToursContainer.widthProperty());
    vineyardCardsContainer = new CardsContainer<>(
        vineyardsContainer.viewportBoundsProperty(),
        vineyardsContainer.widthProperty());
    itineraryCardsContainer = new CardsContainer<>(
        itineraryContainer.viewportBoundsProperty(),
        itineraryContainer.widthProperty());
    vineyardToursContainer.setContent(vineyardTourButtonsList);
    vineyardsContainer.setContent(vineyardCardsContainer);
    itineraryContainer.setContent(itineraryCardsContainer);
    vineyardToursService.init();
    vineyardService.init();

    mapController = new LeafletOsmController(webView.getEngine());
    mapController.initMap();

    planTourTabContainer.getChildren().remove(planTourOptionsContainer);
  }

  /**
   * Handles the action when the create tour button is clicked.
   */
  @FXML
  public void onCreateTourButtonClick() {
    managerContext.getGuiManager().openVineyardTourPopup(vineyardToursService, null);
  }

  /**
   * Handles the action when the delete tour button is clicked.
   */
  @FXML
  public void onDeleteTourClick() {

  }

  /**
   * Handles the action when the calculate tour button is clicked.
   */
  @FXML
  public void onCalculateTourClick() {
    List<Vineyard> vineyards = currentTourPlanningService.getVineyards();
    if (vineyards.size() < 2) {
      showNotEnoughVineyardsToCalculateError();
      return;
    }

    mapController.clearWineMarkers();
    List<GeoLocation> vineyardLocations = currentTourPlanningService.getVineyards().stream()
        .peek(vineyard -> mapController.addVineyardMaker(vineyard, false))
        .map(Vineyard::getGeoLocation)
        .toList();
    String geometry = geolocationResolver.resolveRoute(vineyardLocations);
    if (geometry == null) {
      showCalculatingRouteError();
      return;
    }
    mapController.addRoute(geometry);
    tabPane.getSelectionModel().select(viewTourTab);
  }

  /**
   * Opens the specified vineyard tour and updates the UI with the associated vineyards.
   * <p>
   * This method populates the vineyard and itinerary containers with the appropriate cards for the
   * selected vineyard tour, allowing the user to view and manage the vineyards included in the
   * tour.
   * </p>
   *
   * @param vineyardTour The vineyard tour to be opened.
   */
  private void openVineyardTour(VineyardTour vineyardTour) {
    vineyardCardsContainer.removeAll();
    itineraryCardsContainer.removeAll();

    ObservableList<Vineyard> vineyards = vineyardService.get();
    vineyards.forEach(vineyard -> {
      VineyardCardContent vineyardCardContent = new VineyardCardContent(vineyard, 150, 100);
      AddRemoveCard addRemoveCard = new AddRemoveCard(vineyardCardsContainer.widthProperty(),
          new SimpleDoubleProperty(), vineyardCardContent, true,
          !vineyardToursService.isVineyardInTour(vineyardTour, vineyard), false,
          () -> currentTourPlanningService.addVineyard(vineyard),
          () -> currentTourPlanningService.removeVineyard(vineyard),
          "Add winery to tour", "Remove winery from tour");
      vineyardCardsContainer.addCard(vineyard, addRemoveCard);
    });
    currentTourPlanningService = new TourPlanningService(managerContext.getDatabaseManager(),
        vineyardTour);
    currentTourPlanningService.getVineyards().addListener((ListChangeListener<Vineyard>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(vineyard -> {
            ItineraryItemCardContent itineraryItemCardContent = new ItineraryItemCardContent(
                vineyard);
            Card card = new Card(itineraryCardsContainer.widthProperty(),
                new SimpleDoubleProperty());
            card.getChildren().add(itineraryItemCardContent);
            itineraryCardsContainer.addCard(vineyard, card);
          });
        }
        if (change.wasRemoved()) {
          change.getRemoved().forEach(vineyard -> itineraryCardsContainer.remove(vineyard));
        }
      }
    });
    currentTourPlanningService.init();
    viewingTourLabel.setText("Viewing Tour: " + vineyardTour.getName());

    planTourTabContainer.getChildren().remove(noTourSelectedContainer);
    // if a tour was previously open, don't add the tour options container again
    if (!planTourTabContainer.getChildren().contains(planTourOptionsContainer)) {
      planTourTabContainer.getChildren().add(planTourOptionsContainer);
    }
  }

  /**
   * Displays an error popup indicating that there are not enough vineyards in the itinerary to
   * calculate a route.
   */
  private void showNotEnoughVineyardsToCalculateError() {
    ErrorPopupController error = managerContext.getGuiManager().showErrorPopup();
    error.setTitle("Error Calculating Route");
    error.setMessage("Please add 2 or more vineyards to your itinerary to calculate a route.");
    error.addButton("Ok", error::close);
  }

  /**
   * Displays an error popup indicating that there was an issue calculating the route.
   */
  private void showCalculatingRouteError() {
    ErrorPopupController error = managerContext.getGuiManager().showErrorPopup();
    error.setTitle("Error Calculating Route");
    error.setMessage("There was an error calculating a route. Please try again later.");
    error.addButton("Ok", error::close);
  }
}
