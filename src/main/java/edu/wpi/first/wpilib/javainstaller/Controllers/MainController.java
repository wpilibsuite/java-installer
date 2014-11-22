package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

import java.io.IOException;
import java.rmi.activation.ActivationID;

/**
 * Controller for the main view. This manages the current subview, and handles the three main
 * button press events
 */
public class MainController {

    @FXML
    private BorderPane mainArea;

    @FXML
    private void initialize() {
        FXMLLoader loader = new FXMLLoader();
        try {
            Parent root = loader.load(getClass().getResource("/fxml/intro_screen.fxml"));
            mainArea.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelClick(ActionEvent event) {
        MainApp.showExitPopup((Stage) mainArea.getScene().getWindow());
    }

}
