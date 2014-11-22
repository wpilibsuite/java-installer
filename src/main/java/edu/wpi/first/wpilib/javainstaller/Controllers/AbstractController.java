package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Common superclass to handle common functions among all controllers, such as showing the exit popup on cancel
 */
public abstract class AbstractController {

    private final String m_previousLocation;
    @FXML
    protected BorderPane mainView;

    /**
     * @param previousLocation The screen to go to when the back button is pressed
     */
    protected AbstractController(String previousLocation) {
        m_previousLocation = previousLocation;
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(m_previousLocation));
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
