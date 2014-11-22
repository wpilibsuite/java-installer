package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Created by fred on 11/21/14.
 */
public class WelcomeController {

    @FXML
    private VBox mainView;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Button nextButton;

    @FXML
    private Button cancelButton;

    @FXML
    public void initialize() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup((Stage) mainView.getScene().getWindow());
    }

    public void handleNext(ActionEvent event) {

    }
}
