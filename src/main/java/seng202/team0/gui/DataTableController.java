package seng202.team0.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import seng202.team0.managers.ManagerContext;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import seng202.team0.util.ProcessCSV;

/**
 * This class handles importing existing data into the managers
 *
 * @author Samuel Beattie
 * @author Angus McDougall
 */
public class DataTableController extends Controller {

  /**
   * HBox that lists columns for mapping
   */
  @FXML
  private HBox columnRemapList;

  /**
   * Import button
   */
  @FXML
  private Button importCSVButton;
  /**
   * Append button
   */
  @FXML
  private Button appendButton;
  /**
   * Replace button
   */
  @FXML
  private Button replaceButton;

  ArrayList<ChoiceBox<String>> columnNames = new ArrayList<>();

  /**
   * Names of columns
   */
  private String[] prettyNames = new String[]{
      "",
      "Title",
      "Variety",
      "Country",
      "Winery",
      "Description",
      "Score",
      "ABV",
      "NZD",
  };

  /**
   * Array of rows of current csv
   * <p>
   *   Might be null
   * </p>
   */
  private ArrayList<String[]> selectedTable;




  /**
   * Constructor
   *
   * @param managerContext manager context
   */
  public DataTableController(ManagerContext managerContext) {
    super(managerContext);
  }

  /**
   * Checks that all remap columns are in a valid state
   * <p>
   *   A state is valid if:
   *   - Only one column of each type is selected
   *   - The title is selected
   * </p>
   * @return if state is valid
   */
  boolean isValidRemapping() {

    // Check for each box if there are any others with same value
    for(int i=0; i < columnNames.size(); i++) {
      String value = columnNames.get(i).getValue();
      if(value == null || Objects.equals(value, ""))
        continue;

      for(int j=0; j < columnNames.size(); j++) {
        if(i != j) {
          if(Objects.equals(value, columnNames.get(j).getValue()))
            return false;
        }
      }
    }
    // Check there is a title box
    boolean containsTitle = columnNames.stream().anyMatch(stringChoiceBox -> Objects.equals(
        stringChoiceBox.getValue(), "Title"));

    return containsTitle;

  }
  boolean validateColumns() {
    if(selectedTable == null)
      return false;
    return true;
  }

  /**
   * updates the state of if this selection of rows to remap is valid
   */
  void updateValidation() {
    // Check all option boxes only correspond to one
    if (!isValidRemapping()) {
      appendButton.setDisable(true);
      replaceButton.setDisable(true);
      return;
    }
    appendButton.setDisable(false);
    replaceButton.setDisable(false);
  }



  /**
   * Makes the option box
   * @param name name to maybe preselect
   * @return option box
   */
  private Node makeOptionBox(String name) {

    ChoiceBox<String> choiceBox = new ChoiceBox<>();
    choiceBox.getItems().addAll(prettyNames);
    choiceBox.setMaxWidth(Double.MAX_VALUE);
    for(String prettyName : prettyNames){
      if(prettyName.compareToIgnoreCase(name) == 0) {
        choiceBox.setValue(prettyName);
        break;
      }
    }

    choiceBox.setOnAction(actionEvent -> updateValidation());

    columnNames.add(choiceBox);
    return choiceBox;
  }


  /**
   * Makes a column for previewing and remapping
   * @param name column name
   * @param values preview values
   * @return column
   */
  public Node makeRemapColumn(String name, String[] values){
    VBox vbox = new VBox();

    vbox.setAlignment(Pos.CENTER_LEFT);
    vbox.getChildren().add(new Label(name));
    vbox.getChildren().add(makeOptionBox(name));
    for(String value : values) {
      vbox.getChildren().add(new Label(value));
    }
    vbox.getChildren().add(new Label("..."));

    return vbox;
  }


  /**
   * Makes a list of columns for remapping
   * @param columnNames names of columns
   * @param rows list of rows
   */
  private void makeColumnRemapList(String[] columnNames, List<String[]> rows){

    columnRemapList.getChildren().clear();
    this.columnNames.clear();
    for(int i=0; i < columnNames.length; i++) {
      // First row
      String[] column = new String[rows.size()];
      for(int j=0; j < rows.size(); j++){
        column[j] = rows.get(j)[i];
      }

      columnRemapList.getChildren().add(makeRemapColumn(columnNames[i], column));
    }
  }

  /**
   * Triggers the extension to import a file when the upload csv button is pressed
   */
  public void importCSVFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open CSV File");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

    Stage stage = (Stage) importCSVButton.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);
    if (selectedFile == null)
      return;
    try {
      // Should be first row on pretty much all files
      ArrayList<String[]> rows = ProcessCSV.getCSVRows(selectedFile);

      String[] columnNames = rows.getFirst();
      makeColumnRemapList(columnNames, rows.subList(1, Math.min(10, rows.size())));
      selectedTable = rows;
    } catch(Exception exception) {
      LogManager.getLogger(getClass())
          .error("Failed to read CSV file: {}", selectedFile.getAbsolutePath(), exception);
    }

    updateValidation();
  }

  /**
   * Called to append the current file to the database
   */
  public void appendCSVFile() {
    // TODO validate + stubs
  }

  /**
   * Called to replace the current database with this file
   */
  public void replaceCSVFile() {

  }
}
