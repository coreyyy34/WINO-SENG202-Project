package seng202.team6.gui.controls;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


/**
 * A textbox with an auto-suggest feature based on entered text.
 * <p>
 * Modified from: <a
 * href="https://stackoverflow.com/questions/36861056/javafx-textfield-auto-suggestions">...</a>
 * </p>
 */
public class AutoCompletionTextField extends TextField {

  //Local variables
  //entries to autocomplete
  private final SortedSet<String> entries;
  //popup GUI
  private final ContextMenu entriesPopup;

  private Consumer<String> onSelectionAction;

  /**
   * Constructor for creating an AutoCompletionTextField.
   */
  public AutoCompletionTextField() {
    super();
    this.entries = new TreeSet<>();
    this.entriesPopup = new ContextMenu();

    setListener();
  }

  /**
   * Constructor for creating an AutoCompletionTextField with a predefined text value.
   *
   * @param text The initial text to display in the text field.
   */
  public AutoCompletionTextField(String text) {
    super();
    this.entries = new TreeSet<>();
    this.entriesPopup = new ContextMenu();
    setText(text);

    setListener();
  }


  /**
   * "Suggestion" specific listeners.
   */
  private void setListener() {
    //Add "suggestions" by changing text
    textProperty().addListener((observable, oldValue, newValue) -> {
      String enteredText = getText();
      // Always hide suggestion if nothing has been entered (only "spacebars" are disallowed in
      // TextFieldWithLengthLimit)
      //
      if (enteredText == null || enteredText.isEmpty()) {
        entriesPopup.hide();
      } else {
        //filter all possible suggestions depends on "Text", case insensitive
        List<String> filteredEntries = entries.stream()
            .filter(e -> e.toLowerCase().contains(enteredText.toLowerCase()))
            .collect(Collectors.toList());
        //some suggestions are found
        if (!filteredEntries.isEmpty()) {
          //build popup - list of "CustomMenuItem"
          populatePopup(filteredEntries);
          if (!entriesPopup.isShowing()) { //optional
            entriesPopup.show(AutoCompletionTextField.this, Side.BOTTOM, 0, 0); //position of popup
          }
          //no suggestions -> hide
        } else {
          entriesPopup.hide();
        }
      }
    });

    //Hide always by focus-in (optional) and out
    focusedProperty().addListener((observableValue, oldValue, newValue) -> entriesPopup.hide());
  }


  /**
   * Populate the entry set with the given search results. Display is limited to 10 entries, for
   * performance.
   *
   * @param searchResult The set of matching strings.
   */
  private void populatePopup(List<String> searchResult) {
    //List of "suggestions"
    List<CustomMenuItem> menuItems = new LinkedList<>();
    //List size - 10 or founded suggestions count
    int maxEntries = 10;
    int count = Math.min(searchResult.size(), maxEntries);
    //Build list as set of labels
    for (int i = 0; i < count; i++) {
      final String result = searchResult.get(i);
      //label with graphic (text flow) to highlight founded subtext in suggestions
      Label entryLabel = new Label(result);
      CustomMenuItem item = new CustomMenuItem(entryLabel, true);
      menuItems.add(item);

      // Adjust the label's preferred width to match the TextField
      entryLabel.setPrefWidth(
          getWidth() - 10);  // Adjust to match TextField width, leaving some padding
      entryLabel.setMaxWidth(Double.MAX_VALUE);  // Allow the label to stretch fully

      // Ensure that the text wraps if it's too long
      entryLabel.setWrapText(true);

      //if any suggestion is select set it into text and close popup
      item.setOnAction(actionEvent -> {
        setText(result);
        positionCaret(result.length());
        entriesPopup.hide();
        if (onSelectionAction != null) {
          onSelectionAction.accept(result);
        }
      });
    }

    //"Refresh" context menu
    entriesPopup.getItems().clear();
    entriesPopup.getItems().addAll(menuItems);
  }


  /**
   * Get the existing set of autocomplete entries.
   *
   * @return The existing autocomplete entries.
   */
  public SortedSet<String> getEntries() {
    return entries;
  }

  public ContextMenu getEntriesPopup() {
    return entriesPopup;
  }

  public void setOnSelectionAction(Consumer<String> onSelectionAction) {
    this.onSelectionAction = onSelectionAction;
  }
}
