package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.controlsfx.dialog.Dialogs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Attempts to connect to the roboRio, and displays an error for the user if it cannot
 */
public class ConnectRoboRioController {

    public static final String ROBO_RIO_MDNS_FORMAT_STRING = "roborio-%d.local";
    private static final String CONNECTION_STRING = "Please ensure that you are connected to the roboRio on this computer, and " +
            "input your team number below. Once you have put in your team number, hit connect.";

    @FXML
    private BorderPane mainView;
    @FXML
    private Button nextButton;
    @FXML
    private TextField teamNumberBox;
    @FXML
    private Label mainLabel;

    private String m_JREFolder;
    private String m_tarLocation;
    private int teamNumber = -1;

    public void initialize(String JREFolder, String tarLocation) {
        m_JREFolder = JREFolder;
        m_tarLocation = tarLocation;

        // Ensure only numbers are added to the team box
        teamNumberBox.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d+")) {
                teamNumber = Integer.parseInt(newValue);
            } else {
                teamNumberBox.setText(oldValue);
            }
        });
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/downloaded.fxml"));
        try {
            Parent root = loader.load();
            DownloadedController controller = loader.getController();
            controller.initialize(m_tarLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleNext(ActionEvent event) {
        // First, check to make sure a number has been entered
        if (teamNumber == -1) {
            Dialogs.create().title("Team Number").message("Please type your team number!").showError();
            return;
        }

        // Handle the connection test on a background thread to avoid blocking the ui. If the user clicks next and can't
        // get to the internet, they have the ability to stop the program without waiting for a timeout
        nextButton.setDisable(true);
        nextButton.setText("Checking Connection");
        String roborioAddress = String.format(ROBO_RIO_MDNS_FORMAT_STRING, teamNumber);
        mainLabel.setText("Connecting to " + roborioAddress);
        new Thread(() -> {
            try {
                // Test for connection to the oracle site. If no connection, show an error
                HttpURLConnection connection = (HttpURLConnection) new URL(roborioAddress).openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // We have a connection, load the next page
                    Platform.runLater(() -> {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deploy.fxml"));
                        Parent root = null;
                        try {
                            root = loader.load();
                            mainView.getScene().setRoot(root);
                            DeployController controller = loader.getController();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        MainApp.showErrorPopup("Could not connect to the roboRio at " + roborioAddress);
                        nextButton.setDisable(false);
                        nextButton.setText("Retry Connect");
                        mainLabel.setText(CONNECTION_STRING);
                    });
                }
            } catch (java.io.IOException e) {
                Platform.runLater(() -> {
                    MainApp.showErrorPopup(
                            "Unknown error when attempting to connect to the roborio: "
                                    + System.lineSeparator()
                                    + e);
                    nextButton.setDisable(false);
                    nextButton.setText("Retry Connect");
                    mainLabel.setText(CONNECTION_STRING);
                });
            }
        }).start();

    }
}
