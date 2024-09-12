package seng202.team0.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.ObjectUtils.Null;
import seng202.team0.database.User;
import seng202.team0.database.Wine;


/**
 * Mediates access to the database
 *
 */
public class DatabaseManager implements AutoCloseable {

  /**
   * Database connection
   * <p>
   * This is ensured to be always valid
   * </p>
   */
  private Connection connection;

  /**
   * Connects to a db file for management. The path to the file is specified by dbpath
   *
   * @throws SQLException if failed to initialize
   */
  public DatabaseManager() throws SQLException {

    // Construct a file path for the database
    File dir = new File("sqlDatabase");
    if (!dir.exists()) {
      boolean created = dir.mkdirs();

      if (!created) {
        System.err.println("Error creating database directory");
      }

    }

    // Connect to database
    // This is the path to the db file
    //String dbPath = "jdbc:sqlite:sqlDatabase" + File.separator + "SQLDatabase.db";
    //this.connection = DriverManager.getConnection(dbPath);
    this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    createWinesTable();
    createUsersTable();

  }

  /**
   * @throws SQLException on sql error
   */
  private void createWinesTable() throws SQLException {
    if (tableExists("WINE")) {
      return;
    }
    String create = "create table WINE (" +
        // There are a lot of duplicates
        "ID AUTO_INCREMENT PRIMARY KEY," +
        "TITLE varchar(64) NOT NULL," +
        "VARIETY varchar(32)," +
        "COUNTRY varchar(32)," +
        "WINERY varchar(64)," +
        "DESCRIPTION text," +
        "SCORE_PERCENT int," +
        "ABV float," +
        "PRICE float);";
    try (Statement statement = connection.createStatement()) {
      statement.execute(create);
    }
    assert (tableExists("WINE"));
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

    ObservableList<Wine> wines = FXCollections.observableArrayList();
    String query = "select TITLE, VARIETY, COUNTRY, WINERY, DESCRIPTION, SCORE_PERCENT, ABV, PRICE from WINE order by ROWID limit ? offset ?;";
    try (PreparedStatement statement = connection.prepareStatement(query)) {

      statement.setInt(1, end - begin);
      statement.setInt(2, begin);

      ResultSet set = statement.executeQuery();
      while (set.next()) {

        Wine wine = new Wine(
            set.getString("TITLE"),
            set.getString("VARIETY"),
            set.getString("COUNTRY"),
            set.getString("WINERY"),
            set.getString("DESCRIPTION"),
            set.getInt("SCORE_PERCENT"),
            set.getFloat("ABV"),
            set.getFloat("PRICE")
        );
        wines.add(wine);
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return wines;
  }

  /**
   * Gets the number of wine records
   *
   * @return total number of wine records
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
   */
  public void replaceAllWines(List<Wine> list) throws SQLException {
    String delete = "delete from WINE;";
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(delete);
    }

    addWines(list);
  }

  /**
   * Adds the wines in the list to the database
   *
   * @param list list of wines
   */
  public void addWines(List<Wine> list) throws SQLException {
    // null key is auto generated
    String insert = "insert into WINE values(null, ?, ?, ?, ?, ?, ?, ?, ?);";
    try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
      for (Wine wine : list) {
        insertStatement.setString(1, wine.getTitle());
        insertStatement.setString(2, wine.getVariety());
        insertStatement.setString(3, wine.getCountry());
        insertStatement.setString(4, wine.getWinery());
        insertStatement.setString(5, wine.getDescription());
        insertStatement.setInt(6, wine.getScorePercent());
        insertStatement.setFloat(7, wine.getAbv());
        insertStatement.setFloat(8, wine.getPrice());
        insertStatement.executeUpdate();
      }

    }
  }

  /**
   * @throws SQLException on sql error
   */
  private void createUsersTable() throws SQLException {
    if (tableExists("USER")) {
      return;
    }
    String create = "create table USER (" +
        "USERNAME varchar(64) PRIMARY KEY," +
        "PASSWORD varchar(64) NOT NULL," +
        "ROLE varchar(8))";
    try (Statement statement = connection.createStatement()) {
      statement.execute(create);
    }
    assert (tableExists("USER"));
    createDefaultAdminUser();
  }

  private void createDefaultAdminUser() throws SQLException { //TODO remove when db persists
    String insert = "insert into USER (username, password, role) values(?, ?, ?);";
    try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
      insertStatement.setString(1, "admin");
      insertStatement.setString(2, "admin");
      insertStatement.setString(3, "admin");
      insertStatement.executeUpdate();
    }
  }

  public User getUser(String username) {
    User user;
    String query = "SELECT * FROM USER WHERE USERNAME = ?";

    try (PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, username);
      ResultSet set = statement.executeQuery();

      if (set.next()) {
        user = new User(set.getString("USERNAME"),set.getString("PASSWORD"),set.getString("ROLE"));
      } else {
        return null;
      }
    } catch (SQLException e) {
      System.err.println("Database error occurred: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
    return user;
  }


  public boolean addUser(String username, String password) {
    String insert = "insert into USER values(?, ?, ?);";
    try (PreparedStatement insertStatement = connection.prepareStatement(insert)) {
      insertStatement.setString(1, username);
      insertStatement.setString(2, password);
      insertStatement.setString(3, "user");
      insertStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      if (e.getMessage().contains("PRIMARY KEY")) {
        System.out.println("Duplicate username: " + username);
        return false;
      } else {
        System.err.println("Database error occurred: " + e.getMessage());
        e.printStackTrace();
        return false;
      }
    }
  }

  public boolean updatePassword(String username, String password) {
    String updateQuery = "UPDATE USER SET PASSWORD = ? WHERE USERNAME = ?";

    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
      updateStatement.setString(1, password);
      updateStatement.setString(2, username);

      int rowsAffected = updateStatement.executeUpdate();

      return rowsAffected > 0;
    } catch (SQLException e) {
      System.err.println("Error updating password: " + e.getMessage());
      return false;
    }
  }

  /**
   * To disconnect/close database
   */
  @Override
  public void close() {
    try {
      this.connection.close();

      // Reset connection
      connection = null;

    } catch (SQLException e) {
      System.err.println("Error closing connection");
      System.err.println(e.getMessage());
    }
  }

  /**
   * Checks if a table exists
   *
   * @param tableName name of table
   * @return if the table exists
   */
  private boolean tableExists(String tableName) {
    try {

      // Get database meta data
      DatabaseMetaData metaData = connection.getMetaData();

      // Strip tableName whitespace
      tableName = tableName.replaceAll("\\s+", "");

      // Query for table existence
      ResultSet tables = metaData.getTables(null, null, tableName, null);

      if (tables.next()) {
        tables.close();
        return true;
      }

      tables.close();

    } catch (SQLException e) {
      System.err.println(e.getMessage());
    }

    return false;
  }

}
