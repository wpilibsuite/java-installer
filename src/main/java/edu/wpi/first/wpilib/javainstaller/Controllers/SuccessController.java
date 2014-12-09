package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Final controller for a successful deploy!
 */
public class SuccessController {

    @FXML
    private BorderPane mainView;

    @FXML
    private ImageView logoImageView;

    @FXML
    private CheckBox cleanupCheckBox;

    private String m_jreFolder;
    private String m_tarLocation;
    private String m_untarredLocation;

    private final Logger m_logger = LogManager.getLogger();

    @FXML
    private void initialize() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    public void initialize(String jreFolder, String tarLocation, String untarredLocation) {
        m_jreFolder = jreFolder;
        m_tarLocation = tarLocation;
        m_untarredLocation = untarredLocation;
    }

    @FXML
    private void handleFinish(ActionEvent event) {
        // Cleanup the installer debris
        if (cleanupCheckBox.isSelected()) {
            cleanupLocalFiles();
        }
        m_logger.debug("Finished!");
        Platform.exit();
    }

    @FXML
    private void handleRestart(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect_roborio.fxml"));
            Parent root = loader.load();
            ConnectRoboRioController controller = loader.getController();
            controller.initialize(m_jreFolder, m_tarLocation, m_untarredLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Error when loading intro_screen from restart");
            MainApp.showErrorScreen(e);
        }
    }

    /**
     * Cleans up the local files used by the installer, except the logs directory
     */
    private void cleanupLocalFiles() {
        m_logger.debug("Cleaning up installer files");
        File roboRioJreFile = new File(DeployController.JRE_TGZ_NAME);
        roboRioJreFile.delete();
        File downloadedJreFile = new File(m_tarLocation);
        downloadedJreFile.delete();
        deleteDirectory(new File(m_untarredLocation));
        deleteDirectory(new File(m_jreFolder));
    }


    private void deleteDirectory(File obj) {
        if (obj.isDirectory()) {
            for (File file : obj.listFiles()) {
                deleteDirectory(file);
            }
        }
        obj.delete();
    }
}
