package seng202.team6.service;

import java.sql.Date;
import java.sql.SQLException;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seng202.team6.managers.AuthenticationManager;
import seng202.team6.managers.DatabaseManager;
import seng202.team6.model.User;
import seng202.team6.model.Wine;
import seng202.team6.model.WineReview;
import seng202.team6.util.DateFormatter;

/**
 * Wine review service for a given wine.
 */
public class WineReviewsService {

  public static final int MAX_DESCRIPTION_CHARACTERS = 255;
  private final AuthenticationManager authenticationManager;
  private final DatabaseManager databaseManager;
  private final ObservableList<WineReview> wineReviews = FXCollections.observableArrayList();
  private final Wine wine;
  private final Property<WineReview> usersReview = new SimpleObjectProperty<>();

  /**
   * Constructor.
   *
   * @param authenticationManager authentication manager
   * @param databaseManager       database manager
   * @param wine                  wine
   */
  public WineReviewsService(AuthenticationManager authenticationManager,
      DatabaseManager databaseManager, Wine wine) {
    this.authenticationManager = authenticationManager;
    this.databaseManager = databaseManager;
    this.wine = wine;
  }


  /**
   * Default Constructor for testing purposes.
   */
  public WineReviewsService() {
    this.authenticationManager = null;
    this.databaseManager = null;
    this.wine = null;
  }

  /**
   * Initialize the service.
   */
  public void init() {
    String username = authenticationManager.getAuthenticatedUsername();
    try {
      wineReviews.addAll(databaseManager.getWineReviewDao().getAll(wine));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    usersReview.setValue(wineReviews.stream()
        .filter(wineReview -> wineReview.getUsername().equals(username))
        .findFirst()
        .orElse(null));
  }

  /**
   * Adds or update a user review.
   *
   * @param rating      rating
   * @param description description
   */
  public void addOrUpdateUserReview(double rating, String description) throws SQLException {
    User user = authenticationManager.getAuthenticatedUser();
    if (hasUserReviewed()) {
      WineReview usersReview = getUsersReview();
      usersReview.setRating(rating);
      usersReview.setDescription(description);
      calculateAverageReview();
      return;
    }
    Date currentDate = new Date(System.currentTimeMillis());
    WineReview wineReview = databaseManager.getWineReviewDao()
        .add(user, wine, rating, description, currentDate);
    if (wineReview != null) {
      wineReviews.add(wineReview);
      usersReview.setValue(wineReview);
      calculateAverageReview();
    }
  }

  /**
   * Deletes users review.
   */
  public void deleteUsersReview() throws SQLException {
    WineReview wineReview = getUsersReview();
    if (wineReview != null) {
      databaseManager.getWineReviewDao().delete(wineReview);
      usersReview.setValue(null);
      wineReviews.remove(wineReview);
      calculateAverageReview();
    }
  }

  /**
   * Gets the caption for the wine review label given the review info.
   *
   * @param wineReview the review for that review card
   * @return the formatted label text
   */
  public String getCaptionWithDateFormatted(WineReview wineReview) {
    String formattedDate = DateFormatter.DATE_FORMAT.format(wineReview.getDate());
    return "From " + wineReview.getUsername() + " on " + formattedDate;
  }


  /**
   * Gets the StringBinding property for the wine review label given the review info.
   *
   * @param wineReview the review for that review card
   * @return the binding property for that review card
   */
  public StringBinding getCaptionBinding(WineReview wineReview) {
    return Bindings.createStringBinding(
        () ->
            "From " + wineReview.getUsername() + " on " + DateFormatter.DATE_FORMAT.format(
                wineReview.getDate()),
        wineReview.dateProperty()
    );
  }

  /**
   * Checks if this wine has any reviews.
   *
   * @return if this wine has a review
   */
  public boolean hasReviews() {
    return !wineReviews.isEmpty();
  }

  /**
   * Gets the reviews for a wine.
   *
   * @return reviews
   */
  public ObservableList<WineReview> getWineReviews() {
    return wineReviews;
  }

  /**
   * Gets the wine.
   *
   * @return wine
   */
  public Wine getWine() {
    return wine;
  }

  /**
   * Gets the users review property.
   *
   * @return review property
   */
  public Property<WineReview> usersReviewProperty() {
    return usersReview;
  }

  /**
   * Gets the users review.
   *
   * @return users review
   */
  public WineReview getUsersReview() {
    return usersReview.getValue();
  }

  /**
   * Checks if the user has reviewed this wine.
   *
   * @return review
   */
  public boolean hasUserReviewed() {
    return getUsersReview() != null;
  }

  /**
   * Updates the average rating.
   */
  private void calculateAverageReview() {
    double sum = wineReviews.stream()
        .mapToDouble(WineReview::getRating)
        .sum();
    wine.setAverageRating(sum / getWineReviews().size());
  }
}
