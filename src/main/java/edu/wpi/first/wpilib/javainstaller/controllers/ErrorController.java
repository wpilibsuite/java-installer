package edu.wpi.first.wpilib.javainstaller.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import net.mightypork.rpack.utils.DesktopApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

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
        DesktopApi.open(new File("."));
    }

    @FXML
    private void handleExit(ActionEvent event) {
        m_logger.debug("Exiting from error screen");
        Platform.exit();
    }
}
