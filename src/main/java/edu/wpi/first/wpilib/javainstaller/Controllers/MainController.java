package edu.wpi.first.wpilib.javainstaller.Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.io.IOException;

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
        showExitPopup((Stage) mainArea.getScene().getWindow());
    }

    public static void showExitPopup(Stage stage) {
        // The popup will have have two buttons: one to cancel, and one to exit the application
        Popup exitPopup = new Popup();
        BorderPane layout = new BorderPane();
        HBox buttonBox = new HBox(10);
        Button cancelButton = new Button();
        Button exitButton = new Button();
        Label exitLabel = new Label("Are you sure you want to exit? The roboRio will not be set up for Java until this finishes");

        exitPopup.setHideOnEscape(true);
        exitPopup.setAutoHide(true);

        cancelButton.setText("Cancel");
        cancelButton.setOnAction((action) -> exitPopup.hide());
        exitButton.setText("Exit");
        exitButton.setOnAction((ActionEvent action) -> Platform.exit());
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(cancelButton, exitButton);

        exitLabel.setPadding(new Insets(10));

        layout.setBottom(buttonBox);
        layout.setCenter(exitLabel);

        exitPopup.getContent().add(layout);
        exitPopup.show(stage);
    }

}
