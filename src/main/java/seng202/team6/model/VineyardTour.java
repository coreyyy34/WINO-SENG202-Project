package seng202.team6.model;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * The WineTour class represents a wine tour which holds who the tour belongs to, the name of the
 * tour, and the island the tour is in.
 */
public class VineyardTour {

  private final ReadOnlyLongProperty id;
  private final StringProperty username;
  private final StringProperty name;

  /**
   * Constructs a WineTour with the specified properties.
   *
   * @param id       the unique identifier for the wine tour
   * @param username the username who created the tour.
   * @param name     the name of the wine tour
   */
  public VineyardTour(long id, String username, String name) {
    this.id = new SimpleLongProperty(id);
    this.username = new SimpleStringProperty(username);
    this.name = new SimpleStringProperty(name);
  }

  /**
   * Returns the ReadOnlyLongProperty representing the unique ID of the wine tour.
   *
   * @return the ReadOnlyLongProperty for the wine tour's ID
   */
  public ReadOnlyLongProperty idProperty() {
    return id;
  }

  /**
   * Returns the unique ID of the wine tour.
   *
   * @return the ID of the wine tour
   */
  public long getId() {
    return id.get();
  }

  /**
   * Returns the StringProperty representing the username who created the tour.
   *
   * @return the StringProperty for the username
   */
  public StringProperty usernameProperty() {
    return username;
  }

  /**
   * Returns the username who created the tour.
   *
   * @return the username of the wine tour
   */
  public String getUsername() {
    return username.get();
  }

  /**
   * Returns the StringProperty representing the name of the wine tour.
   *
   * @return the StringProperty for the tour name
   */
  public StringProperty nameProperty() {
    return name;
  }

  /**
   * Returns the name of the wine tour.
   *
   * @return the name of the wine tour
   */
  public String getName() {
    return name.get();
  }
}
