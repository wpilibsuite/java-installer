package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Final controller for a successful deploy!
 */
public class SuccessController {

    @FXML
    private BorderPane mainView;

    @FXML
    private ImageView logoImageView;

    private final Logger m_logger = LogManager.getLogger();

    private void initialize() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    @FXML
    private void handleFinish(ActionEvent event) {
        m_logger.debug("Finished!");
        Platform.exit();
    }

    @FXML
    private void handleRestart(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/intro_screen.fxml"));
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Error when loading intro_screen from restart");
            MainApp.showErrorScreen(e);
        }
    }
}
