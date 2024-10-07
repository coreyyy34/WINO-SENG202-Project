package seng202.team6.gui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import seng202.team6.managers.DatabaseManager;
import seng202.team6.managers.ManagerContext;
import seng202.team6.model.User;

import java.util.Optional;

public class UserManagementController extends Controller {
    @FXML
    private ListView<User> userList;

    @FXML
    private Label userLabel;

    @FXML
    private Button deleteUser;

    private ManagerContext managerContext;

    private DatabaseManager dbman;

    private User workingUser = null;

    public UserManagementController(ManagerContext managerContext, Runnable backAction) {
        super(managerContext);
        this.managerContext = managerContext;
        this.dbman = managerContext.getDatabaseManager();
    }

    /**
     * Initialize the controller, set up the list and event listeners
     */
    @FXML
    private void initialize() {
        resetView();
        userList.setOnMouseClicked(this::selectUser);
    }

    /**
     * Reset FXML component content. Used on account deletion.
     */
    private void resetView() {
        ObservableList<User> users = dbman.getUserDao().getAll();
        userList.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getUsername() == null) {
                    setText(null);
                } else {
                    setText(item.getUsername());
                }
            }
        });
        userList.setItems(users);
        userLabel.setText("No User Selected");
        deleteUser.setDisable(true);
    }

    /**
     * Select a user from the list by double clicking on them.
     * @param event
     */
    @FXML
    private void selectUser(MouseEvent event) {
        //doubleclick
        if (event.getClickCount() == 2) {
            workingUser = userList.getSelectionModel().getSelectedItem();
            userLabel.setText(workingUser.getUsername());
            deleteUser.setDisable(false);
        }
    }





}
