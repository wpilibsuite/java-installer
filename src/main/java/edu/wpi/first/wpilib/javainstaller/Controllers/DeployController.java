package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.controlsfx.control.action.Action;

import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.IOException;

/**
 * Deploys the JRE to the roboRio
 */
public class DeployController {
    private static final String JRE_INSTALL_LOCATION = "/usr/local/frc/";

    @FXML
    private BorderPane mainView;

    private String m_tarLocation;
    private String m_jreLocation;
    private int m_teamNumber;

    private Thread m_ftpThread;

    public void initialize(String tarLocation, String jreLocation, int teamNumber) {
        m_tarLocation = tarLocation;
        m_jreLocation = jreLocation;
        m_teamNumber = teamNumber;
        m_ftpThread = new Thread(() -> {
            FTPClient roboRioClient = new FTPClient();
            try {
                roboRioClient.connect(String.format(ConnectRoboRioController.ROBO_RIO_MDNS_FORMAT_STRING, m_teamNumber));
                int reply = roboRioClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    MainApp.showErrorPopup("Error connecting to the roboRio, FTP code is " + reply, false);
                    handleBack(null);
                }

                // Loop through all JRE files and copy them to the roboRio
                File jreFolder = new File(m_jreLocation);
                
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    MainApp.showErrorPopup("Could not connect to the roboRio: " + System.lineSeparator() + e, false);
                    handleBack(null);
                });
            }
        });
        m_ftpThread.setDaemon(true);
        m_ftpThread.start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect_roborio.fxml"));
        try {
            Parent root = loader.load();
            ConnectRoboRioController controller = loader.getController();
            controller.initialize(m_tarLocation, m_jreLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }
}
