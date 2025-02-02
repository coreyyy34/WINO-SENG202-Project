package seng202.team6.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import seng202.team6.managers.DatabaseManager;
import seng202.team6.model.GeoLocation;
import seng202.team6.model.Wine;
import seng202.team6.model.WineFilters;
import seng202.team6.service.WineDataStatService;
import seng202.team6.util.DatabaseObjectUniquer;
import seng202.team6.util.Timer;

/**
 * Data Access Object (DAO) for handling wine related database operations.
 */
public class WineDao extends Dao {

  /**
   * Cache to store and reuse Wine objects to avoid duplication.
   */
  private final DatabaseObjectUniquer<Wine> wineCache = new DatabaseObjectUniquer<>();

  private final WineDataStatService wineDataStatService;


  /**
   * Constructs a new WineDAO with the given database connection.
   *
   * @param connection The database connection to be used for wine operations.
   */
  public WineDao(Connection connection, WineDataStatService wineDataStatService) {
    super(connection, WineDao.class);
    this.wineDataStatService = wineDataStatService;
  }

  /**
   * Returns the SQL statements required to initialise the WINE table.
   *
   * @return Array of SQL statements for initialising the WINE table
   */
  @Override
  public String[] getInitialiseStatements() {
    return new String[]{
        "CREATE TABLE IF NOT EXISTS WINE ("
            + "ID             INTEGER       PRIMARY KEY,"
            + "TITLE          VARCHAR(64)   NOT NULL,"
            + "VARIETY        VARCHAR(32),"
            + "COUNTRY        VARCHAR(32),"
            + "REGION         VARCHAR(32),"
            + "WINERY         VARCHAR(64),"
            + "COLOR          VARCHAR(32),"
            + "VINTAGE        INTEGER,"
            + "DESCRIPTION    TEXT,"
            + "SCORE_PERCENT  INTEGER,"
            + "ABV            FLOAT,"
            + "PRICE          FLOAT,"
            + "AVERAGE_RATING DOUBLE"
            + ")"
    };
  }

  /**
   * Retrieves the total number of wines in the database.
   *
   * @return The count of wines in the WINE table
   */
  public int getCount() throws SQLException {
    Timer timer = new Timer();
    String sql = "SELECT COUNT(*) FROM WINE";
    try (Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery(sql)) {
        if (resultSet.next()) {
          int count = resultSet.getInt(1);
          log.info("Counted {} wines in {}ms", count, timer.currentOffsetMilliseconds());
          return count;
        }
      }
    }
    return 0;
  }

  /**
   * Retrieves total number of wines after applying filters.
   *
   * @param filters filters to apply to wines before counting
   * @return number of wines after filtering
   */
  public int getCount(WineFilters filters) throws SQLException {
    String sql = "SELECT count(*) from WINE "
        + "where TITLE like ? "
        + "and COUNTRY like ? "
        + "and WINERY like ? "
        + "and COLOR like ? "
        + "and VINTAGE between ? and ? "
        + "and SCORE_PERCENT between ? and ? "
        + "and ABV between ? and ? "
        + "and PRICE between ? and ? "
        + "ORDER BY WINE.ID ;";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int paramIndex = 1;
      statement.setString(paramIndex++,
          filters.getTitle().isEmpty() ? "%" : "%" + filters.getTitle() + "%");
      statement.setString(paramIndex++,
          filters.getCountry().isEmpty() ? "%" : "%" + filters.getCountry() + "%");
      statement.setString(paramIndex++,
          filters.getWinery().isEmpty() ? "%" : "%" + filters.getWinery() + "%");
      statement.setString(paramIndex++,
          filters.getColor().isEmpty() ? "%" : "%" + filters.getColor() + "%");
      statement.setInt(paramIndex++, filters.getMinVintage());
      statement.setInt(paramIndex++, filters.getMaxVintage());
      statement.setDouble(paramIndex++, filters.getMinScore());
      statement.setDouble(paramIndex++, filters.getMaxScore());
      statement.setDouble(paramIndex++, filters.getMinAbv());
      statement.setDouble(paramIndex++, filters.getMaxAbv());
      statement.setDouble(paramIndex++, filters.getMinPrice());
      statement.setDouble(paramIndex, filters.getMaxPrice());

      ResultSet resultSet = statement.executeQuery();
      return resultSet.getInt(1);

    }
  }

  /**
   * Retrieves all wines from the WINE table.
   *
   * @return An ObservableList of all Wine objects in the database
   */
  public ObservableList<Wine> getAll() throws SQLException {
    Timer timer = new Timer();
    String sql = "SELECT WINE.ID as wine_id, WINE.*, GEOLOCATION.LATITUDE, GEOLOCATION.LONGITUDE "
        + "FROM WINE "
        + "LEFT JOIN GEOLOCATION ON LOWER(WINE.REGION) LIKE LOWER(GEOLOCATION.NAME)"
        + "ORDER BY WINE.ID ";
    try (Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery(sql)) {
        ObservableList<Wine> wines = extractAllWinesFromResultSet(resultSet, "wine_id");
        log.info("Successfully retrieved all {} wines in {}ms", wines.size(),
            timer.currentOffsetMilliseconds());
        return wines;
      }
    }
  }

  /**
   * Retrieves a range of wines from the WINE table.
   *
   * @param begin   The start index of the range (inclusive)
   * @param end     The end index of the range (exclusive)
   * @param filters The wine filters to be applied
   * @return An ObservableList of Wine objects within the specified range
   */
  public ObservableList<Wine> getAllInRange(int begin, int end, WineFilters filters)
      throws SQLException {
    Timer timer = new Timer();
    String sql = "SELECT WINE.ID as wine_id, WINE.*, GEOLOCATION.LATITUDE, GEOLOCATION.LONGITUDE "
        + "FROM WINE "
        + "LEFT JOIN GEOLOCATION ON LOWER(WINE.REGION) LIKE LOWER(GEOLOCATION.NAME) "
        + "WHERE WINE.ID > ? "
        + (filters == null ? "" : "AND TITLE LIKE ? "
        + "AND COUNTRY LIKE ? "
        + "AND WINERY LIKE ? "
        + "AND COLOR LIKE ? "
        + "AND VINTAGE BETWEEN ? AND ? "
        + "AND SCORE_PERCENT BETWEEN ? AND ? "
        + "AND ABV BETWEEN ? AND ? "
        + "AND PRICE BETWEEN ? AND ? ")
        + "ORDER BY WINE.ID "
        + "LIMIT ?;";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int paramIndex = 1;
      statement.setInt(paramIndex++, begin);
      if (filters != null) {
        statement.setString(paramIndex++,
            filters.getTitle().isEmpty() ? "%" : "%" + filters.getTitle() + "%");
        statement.setString(paramIndex++,
            filters.getCountry().isEmpty() ? "%" : "%" + filters.getCountry() + "%");
        statement.setString(paramIndex++,
            filters.getWinery().isEmpty() ? "%" : "%" + filters.getWinery() + "%");
        statement.setString(paramIndex++,
            filters.getColor().isEmpty() ? "%" : "%" + filters.getColor() + "%");
        statement.setInt(paramIndex++, filters.getMinVintage());
        statement.setInt(paramIndex++, filters.getMaxVintage());
        statement.setDouble(paramIndex++, filters.getMinScore());
        statement.setDouble(paramIndex++, filters.getMaxScore());
        statement.setDouble(paramIndex++, filters.getMinAbv());
        statement.setDouble(paramIndex++, filters.getMaxAbv());
        statement.setDouble(paramIndex++, filters.getMinPrice());
        statement.setDouble(paramIndex++, filters.getMaxPrice());
      }
      statement.setInt(paramIndex, end - begin);

      try (ResultSet resultSet = statement.executeQuery()) {
        ObservableList<Wine> wines = extractAllWinesFromResultSet(resultSet, "wine_id");
        log.info("Successfully retrieved {} wines in range {}-{} in {}ms", wines.size(),
            begin, end, timer.currentOffsetMilliseconds());
        return wines;
      }
    }
  }

  /**
   * Gets a wine with a specific id.
   *
   * @param id id of wine
   * @return wine of given id or null
   */
  public Wine get(long id) throws SQLException {
    String sql = "SELECT WINE.ID as wine_id, WINE.*, GEOLOCATION.LATITUDE, GEOLOCATION.LONGITUDE "
        + "FROM WINE "
        + "LEFT JOIN GEOLOCATION ON LOWER(WINE.REGION) LIKE LOWER(GEOLOCATION.NAME) "
        + "WHERE ID = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Wine wine = extractWineFromResultSet(resultSet, "wine_id");
          if (wine != null) {
            log.info("Successfully retrieved wine with ID {}", id);
            return wine;
          }
          log.info("Could not retrieve wine with ID {}", id);
        }
      }
    }
    return null;
  }

  /**
   * Retrieves a wine from the database by its exact title.
   *
   * @param title The exact title of the wine to retrieve.
   * @return The Wine object if found, or null if no match is found.
   */
  public Wine getByExactTitle(String title) throws SQLException {
    Timer timer = new Timer();
    String sql = "SELECT WINE.ID as wine_id, WINE.*, GEOLOCATION.LATITUDE, GEOLOCATION.LONGITUDE "
        + "FROM WINE "
        + "LEFT JOIN GEOLOCATION ON LOWER(WINE.REGION) LIKE LOWER(GEOLOCATION.NAME) "
        + "WHERE TITLE = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, title);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          Wine wine = extractWineFromResultSet(resultSet, "wine_id");
          if (wine != null) {
            log.info("Successfully retrieved wine with title '{}' in {}ms", title,
                timer.currentOffsetMilliseconds());
            return wine;
          }
        }
        log.info("Could not retrieve wine with title '{}' in {}ms", title,
            timer.currentOffsetMilliseconds());
      }
    }
    return null;
  }

  /**
   * Replaces all wines in the WINE table by first removing all existing wines and then adding the
   * provided lists of wines.
   *
   * @param wines The list of wines to be added to the table
   */
  public void replaceAll(List<Wine> wines) throws SQLException {
    removeAll();
    addAll(wines);
  }


  /**
   * Adds a list of wines to the database.
   *
   * @param wines list of wines
   * @throws SQLException sql exception
   */
  private void addList(List<Wine> wines) throws SQLException {

    Timer timer = new Timer();
    String sql = "INSERT INTO WINE VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    connection.setAutoCommit(false);

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < wines.size(); i++) {
        if (wines.get(i).getKey() != -1) {
          log.error("Adding wine that is already in the database");
          return;
        }
        setWineParameters(statement, wines.get(i), 1);
        statement.addBatch();
      }
      statement.executeBatch();
      ResultSet keys = statement.getGeneratedKeys();
      int i = 0;
      while (keys.next()) {
        wines.get(i++).setKey(keys.getLong(1));
      }
      connection.commit();
    } finally {
      connection.setAutoCommit(true);
    }
    log.info("Successfully {} wines in {}ms", wines.size(), timer.currentOffsetMilliseconds());
  }

  /**
   * Adds a wine to the database.
   *
   * @param wine wine
   */
  public void add(Wine wine) throws SQLException {
    addList(Collections.singletonList(wine));
    updateUniques(wine); // Check if new wine updates uniques
  }

  /**
   * Adds a list of wines to the WINE table in batch mode to improve performance. The batch is
   * executed every 2048 wines to prevent excessive memory usage.
   *
   * @param wines The list of wines to be added to the table
   */
  public void addAll(List<Wine> wines) throws SQLException {
    for (int i = 0; i < wines.size(); i += 2048) {
      addList(wines.subList(i, Math.min(wines.size(), i + 2048)));
    }

    // Update uniques due to new values
    updateUniques();
  }

  /**
   * Removes all wines from the WINE table.
   */
  public void removeAll() throws SQLException {
    Timer timer = new Timer();
    wineCache.clear();
    String sql = "DELETE FROM WINE";
    try (Statement statement = connection.createStatement()) {
      int rowsAffected = statement.executeUpdate(sql);
      log.info("Successfully removed {} wines in {}ms", rowsAffected,
          timer.currentOffsetMilliseconds());
      wineCache.removeAll();
      updateUniques();
    }
  }

  /**
   * Extracts all wines from the provided ResultSet and stores them in an ObservableList.
   *
   * @param resultSet The ResultSet from which wines are to be extracted
   * @return An ObservableList of Wine objects
   * @throws SQLException If an error occurs while processing the ResultSet
   */
  ObservableList<Wine> extractAllWinesFromResultSet(ResultSet resultSet, String idColumnName)
      throws SQLException {
    ObservableList<Wine> wines = FXCollections.observableArrayList();
    while (resultSet.next()) {
      wines.add(extractWineFromResultSet(resultSet, idColumnName));
    }
    return wines;
  }

  /**
   * Extracts a Wine object from the provided ResultSet. The wine cache is checked before creating a
   * new Wine instance
   *
   * @param resultSet The ResultSet from which wines are to be extracted
   * @return The extracted Wine object
   * @throws SQLException If an error occurs while processing the ResultSet
   */
  Wine extractWineFromResultSet(ResultSet resultSet, String idColumnName) throws SQLException {
    long id = resultSet.getLong(idColumnName);
    Wine cachedWine = wineCache.tryGetObject(id);
    if (cachedWine != null) {
      return cachedWine;
    }

    GeoLocation geoLocation = createGeoLocation(resultSet);
    Wine wine = new Wine(
        id,
        resultSet.getString("TITLE"),
        resultSet.getString("VARIETY"),
        resultSet.getString("COUNTRY"),
        resultSet.getString("REGION"),
        resultSet.getString("WINERY"),
        resultSet.getString("COLOR"),
        resultSet.getInt("VINTAGE"),
        resultSet.getString("DESCRIPTION"),
        resultSet.getInt("SCORE_PERCENT"),
        resultSet.getFloat("ABV"),
        resultSet.getFloat("PRICE"),
        geoLocation,
        resultSet.getDouble("AVERAGE_RATING")
    );
    wineCache.addObject(id, wine);

    bindUpdater(wine);
    return wine;
  }

  /**
   * Extracts the latitude and longitude from the provided ResultSet and creates a new GeoLocation
   * object.
   *
   * @param set The ResultSet from which geolocations are to be extracted
   * @return The extract Geolocation if available, otherwise null if either the latitude or
   *     longitude were null
   * @throws SQLException If an error occurs while processing the ResultSet
   */
  private GeoLocation createGeoLocation(ResultSet set) throws SQLException {
    double latitude = set.getDouble("LATITUDE");
    if (set.wasNull()) {
      return null;
    }
    double longitude = set.getDouble("LONGITUDE");
    if (set.wasNull()) {
      return null;
    }
    return new GeoLocation(latitude, longitude);
  }


  /**
   * Sets the parameters for the PreparedStatement with the Wine objects data.
   *
   * @param statement  The PreparedStatement to set the parameters for
   * @param wine       The wine whose data will be used to set
   * @param startIndex The starting param index
   * @throws SQLException If an error occurs while setting the PreparedStatement's parameters
   */
  private void setWineParameters(PreparedStatement statement, Wine wine, int startIndex)
      throws SQLException {
    statement.setString(startIndex++, wine.getTitle());
    statement.setString(startIndex++, wine.getVariety());
    statement.setString(startIndex++, wine.getCountry());
    statement.setString(startIndex++, wine.getRegion());
    statement.setString(startIndex++, wine.getWinery());
    statement.setString(startIndex++, wine.getColor());
    statement.setInt(startIndex++, wine.getVintage());
    statement.setString(startIndex++, wine.getDescription());
    statement.setInt(startIndex++, wine.getScorePercent());
    statement.setFloat(startIndex++, wine.getAbv());
    statement.setFloat(startIndex++, wine.getPrice());
    statement.setDouble(startIndex, wine.getAverageRating());
  }

  /**
   * Binds listeners to the Wine object to ensure that any changes to the wines properties are
   * automatically reflected in the database.
   *
   * @param wine The Wine object to bind listeners to
   */
  private void bindUpdater(Wine wine) {
    wine.titleProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "TITLE", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.varietyProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "VARIETY", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.countryProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "COUNTRY", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.regionProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "REGION", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.wineryProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "WINERY", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.colorProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "COLOR", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.vintageProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "VINTAGE", update -> {
          update.setInt(1, (int) after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.descriptionProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "DESCRIPTION", update -> {
          update.setString(1, after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.scorePercentProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "SCORE_PERCENT", update -> {
          update.setInt(1, (int) after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.abvProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "ABV", update -> {
          update.setFloat(1, (float) after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.priceProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "PRICE", update -> {
          update.setFloat(1, (float) after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
    wine.averageRatingProperty().addListener((observableValue, before, after) -> {
      try {
        updateAttribute(wine.getKey(), "AVERAGE_RATING", update -> {
          update.setDouble(1, (double) after);
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Updates a specific attribute of the user in the WINE table.
   *
   * @param attributeName   name of attribute
   * @param attributeSetter callback to set attribute
   */
  private void updateAttribute(long id, String attributeName,
      DatabaseManager.AttributeSetter attributeSetter) throws SQLException {
    if (id == -1) {
      log.warn("Skipping attribute update '{}' for wine with ID -1",
          attributeName);
      return;
    }
    Timer timer = new Timer();
    String sql = "UPDATE WINE set " + attributeName + " = ? where ID = ?";
    try (PreparedStatement update = connection.prepareStatement(sql)) {
      attributeSetter.setAttribute(update);
      update.setLong(2, id);

      int rowsAffected = update.executeUpdate();
      if (rowsAffected == 1) {
        log.info("Successfully updated attribute '{}' for wine with ID {} in {}ms",
            attributeName, id, timer.currentOffsetMilliseconds());
      } else {
        log.info("Could not update attribute '{}' for wine with ID {} in {}ms",
            attributeName, id, timer.currentOffsetMilliseconds());
      }
    }
  }

  /**
   * Updates a range of unique values using the wineDataStatService.
   *
   * <p>
   * When the cache is invalidated by write operations to the database this must be called.
   * </p>
   */
  public void updateUniques() throws SQLException {
    wineDataStatService.reset();
    String query = "SELECT title, country, winery, color, vintage, score_percent, abv, price "
        + "FROM wine";
    try (PreparedStatement statement = connection.prepareStatement(query);
        ResultSet set = statement.executeQuery()) {

      // Go through results and add to lists
      while (set.next()) {
        final String title = set.getString("title");
        final String country = set.getString("country");
        final String winery = set.getString("winery");
        final String color = set.getString("color");
        final int vintage = set.getInt("vintage");
        final int score = set.getInt("score_percent");
        final float abv = set.getFloat("abv");
        final float price = set.getFloat("price");

        // Add to sets
        this.wineDataStatService.getUniqueTitles().add(title);
        this.wineDataStatService.getUniqueCountries().add(country);
        this.wineDataStatService.getUniqueWineries().add(winery);
        this.wineDataStatService.getUniqueColors().add(color);

        // Update mins and maxes
        updateMinMax("vintage", vintage);
        updateMinMax("score", score);
        updateMinMax("abv", abv);
        updateMinMax("price", price);
      }
      log.info("Successfully updated unique values wine cache");
    }
  }

  /**
   * Update uniques that handles a single wine instead.
   *
   * @param wine The new wine to check
   */
  public void updateUniques(Wine wine) {

    // Add wine data to sets
    wineDataStatService.getUniqueTitles().add(wine.getTitle());
    wineDataStatService.getUniqueCountries().add(wine.getCountry());
    wineDataStatService.getUniqueWineries().add(wine.getWinery());
    wineDataStatService.getUniqueColors().add(wine.getColor());

    // Check mins and maxes
    updateMinMax("vintage", wine.getVintage());
    updateMinMax("score", wine.getScorePercent());
    updateMinMax("abv", wine.getAbv());
    updateMinMax("price", wine.getPrice());
  }

  /**
   * Helper function for the update uniques, just abstracts the min and max checks.
   * <p>
   * Updates the specified value if the new value is smaller than current min or<br> greater than
   * current max.
   * </p>
   *
   * @param name  name of the variable to update
   * @param value new value
   */
  private void updateMinMax(String name, float value) {
    switch (name) {
      case "vintage":
        if (value > this.wineDataStatService.getMaxVintage()) {
          this.wineDataStatService.setMaxVintage((int) value);

          // In decanter, some vintages are NV which defaults to 0
          // In the 130k dataset, some values don't have vintage that defaults to -1
        } else if (value < this.wineDataStatService.getMinVintage() && value > 0) {
          this.wineDataStatService.setMinVintage((int) value);
        }
        break;
      case "score":
        if (value > this.wineDataStatService.getMaxScore()) {
          this.wineDataStatService.setMaxScore((int) value);
        } else if (value < this.wineDataStatService.getMinScore()) {
          this.wineDataStatService.setMinScore((int) value);
        }
        break;
      case "abv":
        if (value > this.wineDataStatService.getMaxAbv()) {
          this.wineDataStatService.setMaxAbv(value);
        } else if (value < this.wineDataStatService.getMinAbv()) {
          this.wineDataStatService.setMinAbv(value);
        }
        break;
      case "price":
        if (value > this.wineDataStatService.getMaxPrice()) {
          this.wineDataStatService.setMaxPrice(value);
        } else if (value < this.wineDataStatService.getMinPrice()) {
          this.wineDataStatService.setMinPrice(value);
        }
        break;

      default:
        break;
    }
  }

  /**
   * Gets the wineDataStatService.
   *
   * @return wineDataStatService
   */
  public WineDataStatService getWineDataStatService() {
    return wineDataStatService;
  }
}
