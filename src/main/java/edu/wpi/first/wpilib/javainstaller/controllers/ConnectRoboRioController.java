package edu.wpi.first.wpilib.javainstaller.controllers;

import net.mightypork.rpack.utils.DesktopApi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Attempts to connect to the roboRio, and displays an error for the user if it cannot
 */
public class ConnectRoboRioController extends AbstractController {

    public static final String ROBO_RIO_MDNS_FORMAT_STRING = "roborio-%d-frc.local";
    public static final String ROBO_RIO_USB_IP = "172.22.11.2";
    public static final String ROBO_RIO_IP_FORMAT_STRING = "10.%d.%d.2";
    private static final String CONNECTION_STRING = "At this time, please connect this computer to the roboRIO, and" +
            " enter your team number below. If this computer cannot connect to the roboRIO, please hit the Open in" +
            " Explorer button below. Copy the JRE.tar.gz file, the JRE.tar.gz.md5 file, and this installer to a " +
            "computer with access to the roboRIO and run it again. If the installer does not automatically find the " +
            "JRE, please hit the Already Downloaded button and browse to its location.";

    @FXML
    private Button nextButton;
    @FXML
    private TextField teamNumberBox;
    @FXML
    private Label mainLabel;

    private String m_jreFolder;
    private int teamNumber = -1;
    private final Logger m_logger = LogManager.getLogger();

    public ConnectRoboRioController() {
        super(true, Arguments.Controller.CONNECT_ROBORIO_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        m_jreFolder = m_args.getArgument(Arguments.Argument.JRE_FOLDER);
        mainLabel.setText(CONNECTION_STRING);
        // Ensure only numbers are added to the team box
        teamNumberBox.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d+")) {
                teamNumber = Integer.parseInt(newValue);
            } else {
                teamNumberBox.setText(oldValue);
            }
        });
    }

    /**
     * Opens the JRE in the native file browser
     *
     * @param event unused
     */
    @FXML
    private void handleOpenDirectory(ActionEvent event) {
        File jreFile = new File(m_jreFolder);
        File parent = jreFile.getParentFile();
        DesktopApi.open(parent);
    }

    @FXML
    private void handleNext(ActionEvent event) {
        // First, check to make sure a number has been entered
        if (teamNumber < 1) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Team Number");
            alert.setContentText("Please type your team number!");
            alert.setHeaderText(null);
            alert.showAndWait();
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
            // Test for connection to the roboRio. If no connection, show an error
            // First check mDNS
            m_logger.debug("Checking for mDNS connection to the roboRIO");
            if (checkReachableAndContinue(roboRioMDNS)) {
                m_logger.debug("Found mDNS connection");
                return;
            }

            // No mDNS, check USB
            m_logger.debug("No mDNS connection found, checking for USB Connection to the roboRIO");
            Platform.runLater(() -> mainLabel.setText("Could not connect with mDNS, trying USB connect at " + ROBO_RIO_USB_IP));
            if (checkReachableAndContinue(ROBO_RIO_USB_IP)) {
                m_logger.debug("Found USB connection to the roboRio");
                return;
            }

            m_logger.debug("No usb connection found, checking for ethernet connection to the roboRIO");
            Platform.runLater(() -> mainLabel.setText("Could not connect with USB, trying legacy ip connect at " + roboRioIP));
            if (checkReachableAndContinue(roboRioIP)) {
                m_logger.debug("Found ethernet connection to the roboRIO");
                return;
            }
            Platform.runLater(() -> {
                showErrorPopup("Could not connect to the roboRIO at " + roboRioMDNS + " after 5 seconds", false);
                m_logger.warn("Could not connect to the roboRIO at " + roboRioMDNS + " or " + ROBO_RIO_USB_IP + " or " + roboRioIP);
                nextButton.setDisable(false);
                nextButton.setText("Retry Connect");
                mainLabel.setText(CONNECTION_STRING);
            });
        }).start();
    }

    /**
     * Checks to see if the given ip address (or host name) is reachable in 5 seconds. If it is,
     * then the next stage is loaded. If not, then false is returned, and nothing is changed
     *
     * @param addr The ip or hostname to check
     * @return True if we found the roborio, false otherwise
     */
    private boolean checkReachableAndContinue(String addr) {
        try {
            InetAddress address = InetAddress.getByName(addr);
            if (address.isReachable(5000)) {
                m_logger.debug(addr + " is reachable");
                m_args.setArgument(Arguments.Argument.IP, addr);
                Platform.runLater(() -> moveNext(Arguments.Controller.DEPLOY_CONTROLLER));
                return true;
            } else {
                m_logger.warn("Connection to " + addr + " timed out.");
                return false;
            }
        } catch (IOException e) {
            m_logger.warn("Could not connect to the roboRio at " + addr, e);
            return false;
        }
    }
}
