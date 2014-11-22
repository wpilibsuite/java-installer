package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Common superclass to handle common functions among all controllers, such as showing the exit popup on cancel
 */
public abstract class AbstractController {

    private BorderPane m_pane;

    protected void initialize(BorderPane pane) {
        m_pane = pane;
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

}
