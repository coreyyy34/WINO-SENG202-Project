package seng202.team6.managers;

import com.opencsv.exceptions.CsvValidationException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import seng202.team6.model.Filters;
import seng202.team6.model.GeoLocation;
import seng202.team6.model.User;
import seng202.team6.model.Wine;
import seng202.team6.util.EncryptionUtil;
import seng202.team6.util.ProcessCSV;


/**
 * Mediates access to the database
 */
public class DatabaseManager implements AutoCloseable {

  /**
   * Logger for the DatabaseManager class
   */
  private final Logger log = LogManager.getLogger(getClass());

  /**
   * Database connection
   * <p>
   * This is ensured to be always valid
   * </p>
   */
  private Connection connection;

  /**
   * Connects to a db file for management. The path to the file is specified by dbpath
   * <p>
   * This method will fail if application is opened from a directory without appropriate file perms.
   * Check <a href="https://www.sqlite.org/wal.html">...</a> for details on WAL. It is much faster
   * in testing
   * </p>
   *
   * @param databaseFileName name of database file to open
   * @param useWal           whether to use WAL
   * @throws SQLException if failed to initialize
   */
  public DatabaseManager(String databaseFileName, boolean useWal) throws SQLException {

    // Construct a file path for the database
    File dir = new File("sqlDatabase");
    if (!dir.exists()) {
      boolean created = dir.mkdirs();

      if (!created) {
        log.error("Error creating database directory");
        throw new RuntimeException("Failed to create database");
      }
    }

    String dbPath = "jdbc:sqlite:sqlDatabase" + File.separator + databaseFileName;
    this.connection = DriverManager.getConnection(dbPath);
    createWinesTable();
    createUsersTable();
    createGeolocationTable();
    addGeolocations();
    createWineListsTable();

    try (Statement statement = connection.createStatement()) {
      if (useWal) {
        statement.execute("pragma journal_mode=wal");
      }
    }
  }


  /**
   * Creates an in-memory database for testing
   *
   * @throws SQLException if failed to initialize
   */
  public DatabaseManager() throws SQLException {
    this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    createWinesTable();
    createUsersTable();
    createWineListsTable();
    createGeolocationTable();
    addGeolocations();
  }

  /**
   * Creates the WINE table if it does not exist
   *
   * @throws SQLException on sql error
   */
  private void createWinesTable() throws SQLException {
    String create = "create table if not exists WINE (" +
        // There are a lot of duplicates
        // This definition of ID is intended to alias to ROWID.
        "ID INTEGER PRIMARY KEY," +
        "TITLE varchar(64) NOT NULL," +
        "VARIETY varchar(32)," +
        "COUNTRY varchar(32)," +
        "REGION varchar(32)," +
        "WINERY varchar(64)," +
        "COLOR varchar(32)," +
        "VINTAGE int," +
        "DESCRIPTION text," +
        "SCORE_PERCENT int," +
        "ABV float," +
        "PRICE float);";
    try (Statement statement = connection.createStatement()) {
      statement.execute(create);
    }
  }

  /**
   * Sets a given wines attribute
   *
   * @param id        id
   * @param attribute attribute name
   * @param callback  callback to set attribute
   * @throws SQLException if error
   */
  public void setWineAttribute(long id, String attribute, AttributeSetterCallBack callback)
      throws SQLException {
    String updateString = "update WINE set " + attribute + " = ? where ID = ?";
    try (PreparedStatement update = connection.prepareStatement(updateString)) {
      update.setInt(2, (int) id);
      callback.setAttribute(update);
      update.executeUpdate();
    }
  }

  /**
   * Gets a subset of the wines in the database
   * <p>
   * The order of elements should remain stable until a write operation occurs.
   * </p>
   *
   * @param begin beginning element
   * @param end   end element (begin + size)
   * @return subset list of wines
   */
  public ObservableList<Wine> getWinesInRange(int begin, int end) {
    long milliseconds = System.currentTimeMillis();
    ObservableList<Wine> wines = FXCollections.observableArrayList();
    String query =
        "select ID, TITLE, VARIETY, COUNTRY, REGION, WINERY, COLOR, VINTAGE, DESCRIPTION, SCORE_PERCENT, ABV, PRICE, LATITUDE, LONGITUDE from WINE "
            + "left join GEOLOCATION on lower(WINE.REGION) like lower(GEOLOCATION.NAME)"
            + "order by WINE.ID "
            + "limit ? "
            + "offset ?;";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setInt(1, end - begin);
      statement.setInt(2, begin);

      ResultSet set = statement.executeQuery();
      while (set.next()) {
        GeoLocation geoLocation = createGeoLocation(set);
        Wine wine = new Wine(
            set.getLong("ID"),
            this,
            set.getString("TITLE"),
            set.getString("VARIETY"),
            set.getString("COUNTRY"),
            set.getString("REGION"),
            set.getString("WINERY"),
            set.getString("COLOR"),
            set.getInt("VINTAGE"),
            set.getString("DESCRIPTION"),
            set.getInt("SCORE_PERCENT"),
            set.getFloat("ABV"),
            set.getFloat("PRICE"),
            geoLocation
        );
        wines.add(wine);
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    LogManager.getLogger(getClass())
        .info("Time to process getWinesInRange: {}", System.currentTimeMillis() - milliseconds);
    return wines;
  }

  /**
   * Gets a subset of the wines in the database with a Map for filtering
   * <p>
   * The order of elements should remain stable until a write operation occurs.
   * </p>
   *
   * @param begin   beginning element
   * @param end     end element (begin + size)
   * @param filters Map of filter values
   * @return subset list of wines
   */
  public ObservableList<Wine> getWinesInRange(int begin, int end, Filters filters) {
    long milliseconds = System.currentTimeMillis();
    ObservableList<Wine> wines = FXCollections.observableArrayList();
    String query =
        "select ID, TITLE, VARIETY, COUNTRY, REGION, WINERY, COLOR, VINTAGE, DESCRIPTION, SCORE_PERCENT, ABV, PRICE, LATITUDE, LONGITUDE "
            + "from WINE "
            + "left join GEOLOCATION on lower(WINE.REGION) like lower(GEOLOCATION.NAME)"
            + "where WINE.TITLE like ? "
            + "and WINE.COUNTRY like ? "
            + "and WINE.WINERY like ? "
            + "and WINE.COLOR like ? "
            + "and WINE.VINTAGE between ? and ?"
            + "and WINE.SCORE_PERCENT between ? and ? "
            + "and WINE.ABV between ? and ? "
            + "and WINE.PRICE between ? and ? "
            + "order by WINE.ID "
            + "limit ? "
            + "offset ?;";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      int paramIndex = 1;
      statement.setString(paramIndex++,
          filters.getTitle().isEmpty() ? "%" : "%" + filters.getTitle() + "%");
      statement.setString(paramIndex++,
          filters.getCountry().isEmpty() ? "%" : "%" + filters.getCountry());
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
      statement.setInt(paramIndex++, end - begin);
      statement.setInt(paramIndex, begin);

      // Add filtered wines to list
      ResultSet set = statement.executeQuery();
      while (set.next()) {
        GeoLocation geoLocation = createGeoLocation(set);
        Wine wine = new Wine(
            set.getLong("ID"),
            this,
            set.getString("TITLE"),
            set.getString("VARIETY"),
            set.getString("COUNTRY"),
            set.getString("REGION"),
            set.getString("WINERY"),
            set.getString("COLOR"),
            set.getInt("VINTAGE"),
            set.getString("DESCRIPTION"),
            set.getInt("SCORE_PERCENT"),
            set.getFloat("ABV"),
            set.getFloat("PRICE"),
            geoLocation
        );
        wines.add(wine);
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    LogManager.getLogger(getClass()).info("Time to process getWinesInRange with filter: {}",
        System.currentTimeMillis() - milliseconds);
    return wines;
  }

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
   * Gets the number of wine records
   *
   * @return total number of wine records
   * @throws SQLException if a database error occurs
   */
  public int getWinesSize() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      ResultSet set = statement.executeQuery("select count(*) from WINE;");
      set.next();
      return set.getInt(1);
    }
  }

  /**
   * Replaces all wines in the database with a new list
   *
   * @param list list of wines
   * @throws SQLException if a database error occurs
   */
  public void replaceAllWines(List<Wine> list) throws SQLException {
    removeWines();
    addWines(list);
  }

  /**
   * Removes all wines from the database
   *
   * @throws SQLException if a database error occurs
   */
  public void removeWines() throws SQLException {
    String delete = "delete from WINE;";
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(delete);
    }
  }

  /**
   * Adds the wines in the list to the database
   *
   * @param list list of wines
   * @throws SQLException if a database error occurs
   */
  public void addWines(List<Wine> list) throws SQLException {
    long milliseconds = System.currentTimeMillis();
    // null key is auto generated
    String insert = "insert into WINE values(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
      for (Wine wine : list) {
        insertStatement.setString(1, wine.getTitle());
        insertStatement.setString(2, wine.getVariety());
        insertStatement.setString(3, wine.getCountry());
        insertStatement.setString(4, wine.getRegion());
        insertStatement.setString(5, wine.getWinery());
        insertStatement.setString(6, wine.getColor());
        insertStatement.setInt(7, wine.getVintage());
        insertStatement.setString(8, wine.getDescription());
        insertStatement.setInt(9, wine.getScorePercent());
        insertStatement.setFloat(10, wine.getAbv());
        insertStatement.setFloat(11, wine.getPrice());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    }
    LogManager.getLogger(getClass())
        .info("Time to process addWines: {}", System.currentTimeMillis() - milliseconds);
  }

  /**
   * Creates the USER table if it does not exist
   *
   * @throws SQLException on sql error
   */
  private void createUsersTable() throws SQLException {
    String create = "create table if not exists USER (" +
        "USERNAME varchar(64) PRIMARY KEY," +
        "PASSWORD varchar(64) NOT NULL," +
        "ROLE varchar(8) NOT NULL," +
        "SALT varchar(32))";
    try (Statement statement = connection.createStatement()) {
      statement.execute(create);
    }
    createDefaultAdminUser();
  }

  /**
   * Creates a default admin user if it does not exist
   *
   * @throws SQLException if a database error occurs
   */
  private void createDefaultAdminUser() throws SQLException {
    String checkAndInsert = "INSERT INTO USER (username, password, role, salt) " +
        "SELECT ?, ?, ?, ? " +
        "WHERE NOT EXISTS (SELECT 1 FROM USER WHERE username = ?)";
    try (PreparedStatement statement = connection.prepareStatement(checkAndInsert)) {
      String salt = EncryptionUtil.generateSalt();
      String password = EncryptionUtil.hashPassword("admin", salt);
      statement.setString(1, "admin");
      statement.setString(2, password);
      statement.setString(3, "admin");
      statement.setString(4, salt);
      statement.setString(5, "admin");
      statement.executeUpdate();
    }
  }

  /**
   * Retrieves a user from the database by username
   *
   * @param username the username of the user
   * @return the User object if found, null otherwise
   */
  public User getUser(String username) {
    String query = "SELECT * FROM USER WHERE USERNAME = ?";

    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, username);
      ResultSet set = statement.executeQuery();

      if (set.next()) {
        return new User(set.getString("USERNAME"), set.getString("PASSWORD"), set.getString("ROLE"),
            set.getString("SALT"));
      }
    } catch (SQLException e) {
      log.error("Database error occurred: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * Adds a new user to the database
   *
   * @param username the username of the new user
   * @param password the password of the new user
   * @param salt     the salt used for password hashing
   * @return true if the user was successfully added, false otherwise
   */
  public boolean addUser(String username, String password, String salt) {
    String insert = "insert into USER values(?, ?, ?, ?);";
    try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
      insertStatement.setString(1, username);
      insertStatement.setString(2, password);
      insertStatement.setString(3, "user");
      insertStatement.setString(4, salt);
      insertStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      if (!e.getMessage().contains("A PRIMARY KEY constraint failed")) {
        log.error("Database error occurred: {}", e.getMessage(), e);
      }
      return false;
    }
  }

  /**
   * Updates the password for an existing user
   *
   * @param username the username of the user
   * @param password the new hashed password
   * @param salt     the salt used for password hashing
   * @return true if the user was successfully added, false otherwise
   */
  public boolean updatePassword(String username, String password, String salt) {
    String updateQuery = "UPDATE USER SET PASSWORD = ?, SALT = ? WHERE USERNAME = ?";

    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
      updateStatement.setString(1, password);
      updateStatement.setString(2, salt);
      updateStatement.setString(3, username);

      int rowsAffected = updateStatement.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      log.error("Error updating password: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * Deletes all users from the database except the admin user.
   *
   * @return true if all non-admin users were successfully deleted, false otherwise.
   */
  public boolean deleteAllUsers() {
    String deleteQuery = "delete from USER "
        + "WHERE USERNAME != ?;";
    try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
      deleteStatement.setString(1, "admin");
      deleteStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      log.error("Error deleting users: {}", e.getMessage(), e);
      return false;
    }
  }

  private void createGeolocationTable() throws SQLException {
    String create = "create table if not exists GEOLOCATION ("
        + "NAME varchar(64) PRIMARY KEY,"
        + "LATITUDE decimal NOT NULL,"
        + "LONGITUDE decimal NOT NULL);";
    try (Statement statement = connection.createStatement()) {
      statement.execute(create);
    }
  }

  public void addGeolocations() {
    String query = "SELECT 1 FROM GEOLOCATION";
    try (Statement statement = connection.createStatement()) {
      ResultSet set = statement.executeQuery(query);
      if (set.next()) {
        return;
      }
    } catch (SQLException error) {
      log.error("Could not add geolocations to the database", error);
    }

    try {
      query = "INSERT INTO GEOLOCATION values (?, ?, ?);";

      ArrayList<String[]> rows = ProcessCSV.getCSVRows(
          getClass().getResourceAsStream("/nz_geolocations.csv"));

      try (PreparedStatement statement = connection.prepareStatement(query)) {
        for (int i = 1; i < rows.size(); i++) {
          String[] row = rows.get(i);
          String name = row[0];
          double latitude = Double.parseDouble(row[1]);
          double longitude = Double.parseDouble(row[2]);
          int queryIndex = 1;
          statement.setString(queryIndex++, name);
          statement.setDouble(queryIndex++, latitude);
          statement.setDouble(queryIndex++, longitude);
          statement.addBatch();
        }
        statement.executeBatch();
      } catch (SQLException error) {
        log.error("Could not add geolocations to the database", error);
      }
    } catch (CsvValidationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createWineListsTable() throws SQLException {
    String listNameTable = "CREATE TABLE IF NOT EXISTS LIST_NAME (" +
        "ID INTEGER PRIMARY KEY," +
        "USERNAME VARCHAR(32) NOT NULL," +
        "NAME VARCHAR(10) NOT NULL);";
    String listItemsTable = "CREATE TABLE IF NOT EXISTS LIST_ITEMS (" +
        "ID INTEGER PRIMARY KEY," +
        "LIST_ID INT NOT NULL," +
        "WINE_ID INT NOT NULL);";
    try (Statement statement = connection.createStatement()) {
      statement.execute(listNameTable);
    }

    try (Statement statement = connection.createStatement()) {
      statement.execute(listItemsTable);
    }
    createAdminFavouritesList();
  }

  private void createAdminFavouritesList() {
    String checkAndInsert = "INSERT INTO LIST_NAME (ID, USERNAME, NAME) " +
        "SELECT null, 'admin', 'Favourites'" +
        "WHERE NOT EXISTS (SELECT 1 FROM LIST_NAME WHERE username = 'admin')";
    try (Statement statement = connection.createStatement()) {
      statement.execute(checkAndInsert);
    } catch (SQLException error) {
      log.error("Could not add list to the database", error);
    }
  }

  public void createList(String username, String listName) {
    String create = "insert into LIST_NAME (ID, USERNAME, NAME) values (null, ?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(create)) {
      statement.setString(1, username);
      statement.setString(2, listName);
      statement.execute();
    } catch (SQLException error) {
      log.error("Could not add list to the database", error);
    }
  }

  public void deleteList(String username, String listName) {
    String delete = "delete from LIST_NAME where USERNAME = ? and NAME = ?";
    try (PreparedStatement statement = connection.prepareStatement(delete)) {
      statement.setString(1, username);
      statement.setString(2, listName);
      statement.executeUpdate();
    } catch (SQLException error) {
      log.error("Could not delete a list from the database", error);
    }
  }

  public List<String> getUserLists(String username) {
    List<String> listNames = new ArrayList<>();
    String query = "select ID, NAME from LIST_NAME where USERNAME = ?;";
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, username);

      ResultSet set = statement.executeQuery();
      while (set.next()) {
        listNames.add(set.getString("NAME"));
      }
    } catch (SQLException error) {
      log.error("Could not read user lists from the database", error);
    }
    return listNames;
  }

  /**
   * Closes the database connection
   * <p>
   * This method is called automatically when the DatabaseManager is used in a try-with-resources
   * statement
   * </p>
   */
  @Override
  public void close() {
    try {
      connection.close();
      connection = null;

    } catch (SQLException e) {
      log.error("Error closing connection", e);
    }
  }

  /**
   * Callback to set the attribute to update
   */
  public interface AttributeSetterCallBack {

    /**
     * Updates the prepared statement with the value to set
     * <p>
     * Attribute must be index 1 in prepared statement
     * </p>
     */
    void setAttribute(PreparedStatement statement) throws SQLException;
  }
}
