package seng202.team6.gui;

import java.util.Date;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.controlsfx.control.Rating;
import seng202.team6.managers.AuthenticationManager;
import seng202.team6.managers.ManagerContext;
import seng202.team6.model.User;
import seng202.team6.model.Wine;
import seng202.team6.model.WineReview;
import seng202.team6.service.WineReviewsService;

public class SocialController extends Controller{

  @FXML
  TableView<Wine> wineTableView;

  @FXML
  TableView<WineReview> reviewTableView;

  /**
   * Constructor
   *
   * @param managerContext manager context
   */
  public SocialController(ManagerContext managerContext) {
    super(managerContext);// TODO allow upvotes/downvotes/user search/review search
  }
  public void init() {
    setupWineTableColumns();
    openWineRange(0, 100);

    setupReviewTableColumns();
    openReviewInRange(0, 100);

    reviewTableView.setOnMouseClicked(this::openReviewOnClick);
    wineTableView.setOnMouseClicked(this::openWineOnClick);
  }

  /**
   * Opens a page of wines from the database according to filters
   *
   * @param begin   first element
   * @param end     last element + 1
   */
  private void openWineRange(int begin, int end) { // TODO, add filtering by num reviews/rating
    // Clear existing data
    wineTableView.getItems().clear();

    ObservableList<Wine> wines;

    // todo - find solution to this
//    wines = managerContext.databaseManager.getWinesInRangeWithReviewInfo(begin, end);
    // Set fetched data to the table
//    wineTableView.setItems(wines);

    }

  /**
   * Opens a page of reviews from the database according to filters
   *
   * @param begin   first element
   * @param end     last element + 1
   */
  private void openReviewInRange(int begin, int end) { // TODO, add filtering
    // Clear existing data
    reviewTableView.getItems().clear();

    ObservableList<WineReview> reviews;

    reviews = managerContext.databaseManager.getWineReviewDAO().getAllInRange(begin, end);

    // Set fetched data to the table
    reviewTableView.setItems(reviews);

  }


  public void setupWineTableColumns() {
    // Clear any existing cols
    wineTableView.getColumns().clear();

    // Create and config cols
    wineTableView.setEditable(true);

    TableColumn<Wine, String> titleColumn = new TableColumn<>("Title");
    TableColumn<Wine, String> regionColumn = new TableColumn<>("Region");
    TableColumn<Wine, String> colorColumn = new TableColumn<>("Color");
    TableColumn<Wine, Integer> scoreColumn = new TableColumn<>("Critic Score");
    TableColumn<Wine, Integer> reviewsColumn = new TableColumn<>("Reviews");
    TableColumn<Wine, Double> ratingsColumn = new TableColumn<>("Average User Rating");

    titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));
    colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
    scoreColumn.setCellValueFactory(new PropertyValueFactory<>("scorePercent"));
    reviewsColumn.setCellValueFactory(cellData -> cellData.getValue().getReviewCount().asObject());
    ratingsColumn.setCellValueFactory(cellData -> cellData.getValue().getRating().asObject());
    ratingsColumn.setCellFactory(column -> new TableCell<Wine, Double>() {
      private final HBox starBox = new HBox(5);

      @Override
      protected void updateItem(Double rating, boolean empty) {
        super.updateItem(rating, empty);
        if (empty || rating == null) {
          setGraphic(null);
        } else {
          starBox.getChildren().clear();
          Rating ratingStars = setRating(rating);
          starBox.getChildren().addFirst(ratingStars);
          setGraphic(starBox);
        }
      }
    });

    wineTableView.getColumns().add(titleColumn);
    wineTableView.getColumns().add(regionColumn);
    wineTableView.getColumns().add(colorColumn);
    wineTableView.getColumns().add(scoreColumn);
    wineTableView.getColumns().add(reviewsColumn);
    wineTableView.getColumns().add(ratingsColumn);

  }

  public void setupReviewTableColumns() {
    // Clear any existing cols
    reviewTableView.getColumns().clear();

    // Create and config cols
    reviewTableView.setEditable(true);

    TableColumn<WineReview, String> titleColumn = new TableColumn<>("Wine Name");
    TableColumn<WineReview, String> usernameColumn = new TableColumn<>("By");
    TableColumn<WineReview, Double> ratingsColumn = new TableColumn<>("Score");
    TableColumn<WineReview, Date> dateColumn = new TableColumn<>("Date");

    titleColumn.setCellValueFactory(cellData -> cellData.getValue().getWineName());
    usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
    ratingsColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
    dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

    ratingsColumn.setCellFactory(column -> new TableCell<WineReview, Double>() {
      private final HBox starBox = new HBox(5);

      @Override
      protected void updateItem(Double rating, boolean empty) {
        super.updateItem(rating, empty);

        if (empty || rating == null) {
          setGraphic(null);
        } else {
          starBox.getChildren().clear();
          Rating ratingStars = setRating(rating);
          starBox.getChildren().addFirst(ratingStars);
          setGraphic(starBox);
        }
      }
    });

    reviewTableView.getColumns().add(titleColumn);
    reviewTableView.getColumns().add(usernameColumn);
    reviewTableView.getColumns().add(ratingsColumn);
    reviewTableView.getColumns().add(dateColumn);
  }

  private Rating setRating(double rating) {
    Rating ratingStars = new Rating();
    ratingStars.setUpdateOnHover(false);
    ratingStars.setMouseTransparent(true);
    ratingStars.setOnMouseClicked(Event::consume);
    ratingStars.setOnMouseDragEntered(Event::consume);
    ratingStars.setPartialRating(true);
    ratingStars.setRating(rating);
    return ratingStars;
  }

  @FXML
  public void openWineOnClick(MouseEvent event) {
    if (event.getClickCount() != 2)
      return;

    Wine selectedWine = wineTableView.getSelectionModel().getSelectedItem();
    if (selectedWine == null)
      return;

    Runnable backAction = () -> managerContext.GUIManager.mainController.openSocialScreen();
    managerContext.GUIManager.mainController.openDetailedWineView(selectedWine, backAction);
  }

  @FXML
  public void openReviewOnClick(MouseEvent event) { //TODO take to review screen (upvote/downvote reviews)
    if (event.getClickCount() != 2)
      return;

    WineReview selectedReview = reviewTableView.getSelectionModel().getSelectedItem();
    if (selectedReview == null)
      return;

    // todo - find solution to this
//    Wine selectedWine = managerContext.databaseManager.getWineWithReviewInfoById(selectedReview.getWineID());
//    User reviewer = managerContext.databaseManager.getUser(selectedReview.getUsername());
//    WineReviewsService wineReviewsService = new WineReviewsService(managerContext.authenticationManager, managerContext.databaseManager, selectedWine);
//    managerContext.GUIManager.mainController.openPopupReviewView(wineReviewsService, reviewer, selectedReview);
  }

}
