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
import java.net.InetAddress;

/**
 * Attempts to connect to the roboRio, and displays an error for the user if it cannot
 */
public class ConnectRoboRioController {

    public static final String ROBO_RIO_MDNS_FORMAT_STRING = "roborio-%d.local";
    public static final String ROBO_RIO_USB_IP = "172.22.11.2";
    public static final String ROBO_RIO_IP_FORMAT_STRING = "10.%d.%d.2";
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

    private String m_jreFolder;
    private String m_tarLocation;
    private String m_untarredLocation;
    private int teamNumber = -1;
    private final Logger m_logger = LogManager.getLogger();

    public void initialize(String jreFolder, String tarLocation, String untarredLocation) {
        m_jreFolder = jreFolder;
        m_tarLocation = tarLocation;
        m_untarredLocation = untarredLocation;

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

    @SuppressWarnings("deprecation")
    public void handleNext(ActionEvent event) {
        // First, check to make sure a number has been entered
        if (teamNumber == -1) {
            // TODO: When JDK 8u40 is released, replace this with the official api and remove the suppress warning
            Dialogs.create().title("Team Number").message("Please type your team number!").showError();
            return;
        }

        // Handle the connection test on a background thread to avoid blocking the ui. If the user clicks next and can't
        // get to the internet, they have the ability to stop the program without waiting for a timeout
        nextButton.setDisable(true);
        nextButton.setText("Checking Connection");
        String roboRioMDNS = String.format(ROBO_RIO_MDNS_FORMAT_STRING, teamNumber);
        String roboRioIP = String.format(ROBO_RIO_IP_FORMAT_STRING, teamNumber / 100, teamNumber % 100);
        mainLabel.setText("Connecting to " + roboRioMDNS);
        m_logger.debug("Connecting to " + roboRioMDNS);
        new Thread(() -> {
            // Test for connection to the roborio. If no connection, show an error
            // First check mDNS
            m_logger.debug("Checking for mDNS connection to the roboRio");
            if (checkReachable(roboRioMDNS)) {
                m_logger.debug("Found mDNS connection");
                return;
            }

            // No mDNS, check USB
            m_logger.debug("No mDNS connection found, checking for USB Connection to the roboRio");
            if (checkReachable(ROBO_RIO_USB_IP)) {
                m_logger.debug("Found USB connection to the roboRio");
                return;
            }

            m_logger.debug("No usb connection found, checking for ethernet connection to the roboRio");
            if (checkReachable(roboRioIP)) {
                m_logger.debug("Found ethernet connection to the roboRio");
                return;
            }
            Platform.runLater(() -> {
                MainApp.showErrorPopup("Could not connect to the roboRio at " + roboRioMDNS + " after 5 seconds");
                m_logger.warn("Could not connect to the roboRio at " + roboRioMDNS + " or " + ROBO_RIO_USB_IP + " or " + roboRioIP);
                nextButton.setDisable(false);
                nextButton.setText("Retry Connect");
                mainLabel.setText(CONNECTION_STRING);
            });
        }).start();
    }

    /**
     * Checks to see if the given ip address (or host name) is reachable in 5 seconds. If it is, then the next stage is
     * loaded. If not, then false is returned, and nothing is changed
     *
     * @param ip The ip or hostname to check
     * @return True if we found the roborio, false otherwise
     */
    private boolean checkReachable(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(5000)) {
                Platform.runLater(() -> {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deploy.fxml"));
                    Parent root = null;
                    try {
                        m_logger.debug("Connected to the roboRio at " + ip);
                        root = loader.load();
                        DeployController controller = loader.getController();
                        controller.initialize(m_tarLocation, m_untarredLocation, m_jreFolder, teamNumber, ip);
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Could not load the deploy controller", e);
                        MainApp.showErrorScreen(e);
                    }
                });
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            m_logger.warn("Could not connect to the roboRio at " + ip, e);
            return false;
        }
    }
}
