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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.dialog.Dialogs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

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
    private final Logger m_logger = LogManager.getLogger();

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
            m_logger.error("Could not load the downloaded controller", e);
            MainApp.showErrorScreen(e);
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
        String roboRioAddress = String.format(ROBO_RIO_MDNS_FORMAT_STRING, teamNumber);
        mainLabel.setText("Connecting to " + roboRioAddress);
        m_logger.debug("Connecting to " + roboRioAddress);
        new Thread(() -> {
            try {
                // Test for connection to the roborio. If no connection, show an error
                InetAddress roboRio = InetAddress.getByName(roboRioAddress);
                if (roboRio.isReachable(5000)) {
                    // We have a connection, load the next page
                    Platform.runLater(() -> {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deploy.fxml"));
                        Parent root = null;
                        try {
                            m_logger.debug("Connected to the roboRio at " + roboRioAddress);
                            root = loader.load();
                            mainView.getScene().setRoot(root);
                            DeployController controller = loader.getController();
                            controller.initialize(m_tarLocation, m_JREFolder, teamNumber);
                            mainView.getScene().setRoot(root);
                        } catch (IOException e) {
                            m_logger.error("Could not load the deploy controller", e);
                            MainApp.showErrorScreen(e);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        MainApp.showErrorPopup("Could not connect to the roboRio at " + roboRioAddress + " after 5 seconds");
                        m_logger.warn("Could not connect to the roboRio at " + roboRioAddress + " after 5 seconds");
                        nextButton.setDisable(false);
                        nextButton.setText("Retry Connect");
                        mainLabel.setText(CONNECTION_STRING);
                    });
                }
            } catch (UnknownHostException e) {
                m_logger.warn("Could not resolve the roboRio");
                Platform.runLater(() -> {
                    Dialogs.create()
                            .title("Connection Error")
                            .message("Could not find the roboRio at " + roboRioAddress + ". Are you on the same network as it?")
                            .showError();
                    nextButton.setDisable(false);
                    nextButton.setText("Retry Connect");
                    mainLabel.setText(CONNECTION_STRING);
                });
            } catch (IOException e) {
                m_logger.warn("Unknown error when attempting to connect to the roboRio", e);
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
