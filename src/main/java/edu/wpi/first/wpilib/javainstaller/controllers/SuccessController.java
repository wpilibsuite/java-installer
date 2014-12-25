package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
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

import javax.security.auth.login.FailedLoginException;
import java.io.File;
import java.io.IOException;

/**
 * Final controller for a successful deploy!
 */
public class SuccessController extends AbstractController {

    @FXML
    private ImageView logoImageView;

    private final Logger m_logger = LogManager.getLogger();

    public SuccessController() {
        super(false, Arguments.Controller.SUCCESS_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    @FXML
    private void handleFinish(ActionEvent event) {
        cleanupLocalFiles();
        m_logger.debug("Finished!");
        Platform.exit();
    }

    @FXML
    private void handleRestart(ActionEvent event) {
        moveNext(Arguments.Controller.CONNECT_ROBORIO_CONTROLLER);
    }

    /**
     * Cleans up the local files used by the installer, except the logs directory
     */
    private void cleanupLocalFiles() {
        m_logger.debug("Cleaning up installer files");
        try {
            new File(m_args.getArgument(Arguments.Argument.JRE_CREATOR_TAR)).delete();
            deleteDirectory(new File(m_args.getArgument(Arguments.Argument.JRE_CREATOR_FOLDER)));
            deleteDirectory(new File(m_args.getArgument(Arguments.Argument.JRE_FOLDER)));
        } catch (Exception e) {
            m_logger.warn("Error when deleting files", e);
        }
    }


    private void deleteDirectory(File obj) {
        if (!obj.exists()) {
            return;
        }
        if (obj.isDirectory()) {
            for (File file : obj.listFiles()) {
                deleteDirectory(file);
            }
        }
        obj.delete();
    }
}
