package edu.wpi.first.wpilib.javainstaller.Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.dialog.Dialogs;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Controlling for displaying and error to the user and prompting them to submit their logs
 */
public class ErrorController {

    @FXML
    private Label errorText;

    private final Logger m_logger = LogManager.getLogger();

    public void initialize(Exception err) {
        errorText.setText(err.toString());
    }

    @FXML
    private void handleOpenFolder(ActionEvent event) {
        File curDir = new File(".");
        try {
            Desktop.getDesktop().open(curDir);
        } catch (IOException e) {
            m_logger.error("Error when displaying log directory", e);
            Dialogs.create()
                    .title("Open Folder Error")
                    .message(
                            "Could not open explorer in directory. Please copy the logs from " +
                                    curDir.getAbsolutePath() +
                                    " and submit them to the FIRST discussion boards.")
                    .showError();
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        m_logger.debug("Exiting from error screen");
        Platform.exit();
    }
}
