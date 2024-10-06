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
import javafx.scene.web.WebView;
import seng202.team6.gui.controls.*;
import seng202.team6.gui.controls.card.AddRemoveCard;
import seng202.team6.gui.controls.card.Card;
import seng202.team6.gui.controls.cardcontent.ItineraryItemCardContent;
import seng202.team6.gui.controls.cardcontent.VineyardCardContent;
import seng202.team6.gui.controls.container.CardsContainer;
import seng202.team6.managers.ManagerContext;
import seng202.team6.model.GeoLocation;
import seng202.team6.model.Vineyard;
import seng202.team6.model.VineyardTour;
import seng202.team6.service.TourPlanningService;
import seng202.team6.service.VineyardService;
import seng202.team6.service.VineyardToursService;
import seng202.team6.util.GeolocationResolver;

public class TourPlanningController extends Controller {
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

  private final VineyardToursService vineyardToursService;
  private final VineyardService vineyardService;
  private LeafletOSMController mapController;
  private TourPlanningService currentTourPlanningService;

  private final GeolocationResolver geolocation;

  /**
   * Constructs a new TourPlanningController.
   *
   * @param context The manager context
   */
  public TourPlanningController(ManagerContext context) {
    super(context);
    vineyardToursService = new VineyardToursService(managerContext.authenticationManager,
        managerContext.databaseManager);
    vineyardService = new VineyardService(managerContext.databaseManager);
    geolocation = new GeolocationResolver();
    bindToVineyardToursService();
  }

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

    mapController = new LeafletOSMController(webView.getEngine());
    mapController.initMap();
  }

  @FXML
  public void onCreateTourButtonClick() {
    managerContext.GUIManager.mainController.openVineyardTourPopup(vineyardToursService, null);
  }

  @FXML
  public void onDeleteTourClick() {

  }

  @FXML
  public void onCalculateTourClick() {
    List<Vineyard> vineyards = currentTourPlanningService.getVineyards();
    if (vineyards.size() < 2) {
      // todo - add popup to say not enough vineyards in list
      return;
    }

    List<GeoLocation> vineyardLocations = currentTourPlanningService.getVineyards().stream()
            .peek(vineyard -> mapController.addVineyardMaker(vineyard, false))
            .map(Vineyard::getGeoLocation)
            .toList();
    String geometry = geolocation.resolveRoute(vineyardLocations);
    if (geometry == null) {
      // todo - add popup to say failed to find route
      return;
    }
    mapController.addRoute(geometry);
    tabPane.getSelectionModel().select(viewTourTab);
  }

  public void openVineyardTour(VineyardTour vineyardTour) {
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
    currentTourPlanningService = new TourPlanningService(managerContext.databaseManager, vineyardTour);
    currentTourPlanningService.getVineyards().addListener((ListChangeListener<Vineyard>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(vineyard -> {
            ItineraryItemCardContent itineraryItemCardContent = new ItineraryItemCardContent(vineyard);
            Card card = new Card(itineraryCardsContainer.widthProperty(), new SimpleDoubleProperty());
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
  }
}
