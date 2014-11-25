package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;

/**
 * Shows the welcome screen
 */
public class WelcomeController extends AbstractController {

    @FXML
    private BorderPane mainView;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Button nextButton;

    @FXML
    private Button cancelButton;

    public WelcomeController() {
        super(null);
    }

    @FXML
    public void initialize() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    public void handleNext(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/connect_internet.fxml"));
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            LogManager.getLogger().debug("Error when displaying connect internet screen", e);
            MainApp.showErrorScreen(e);
        }
    }
}
