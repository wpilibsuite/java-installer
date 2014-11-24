package edu.wpi.first.wpilib.javainstaller.Controllers;

import com.sun.deploy.panel.JreFindDialog;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.action.Action;

import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Deploys the JRE to the roboRio
 */
public class DeployController {
    private static final String JRE_INSTALL_LOCATION = "/usr/local/frc";
    // The roborio uses anonymous login
    private static final String FTP_USER_NAME = "anonymous";
    private static final String FTP_PASSWORD = "";

    @FXML
    private BorderPane mainView;

    private String m_tarLocation;
    private String m_jreLocation;
    private int m_teamNumber;

    private Thread m_ftpThread;
    private final Logger m_logger = LogManager.getLogger();

    public void initialize(String tarLocation, String jreLocation, int teamNumber) {
        m_tarLocation = tarLocation;
        m_jreLocation = jreLocation;
        m_teamNumber = teamNumber;
        m_ftpThread = new Thread(() -> {
            FTPClient roboRioClient = new FTPClient();
            boolean success = false;
            try {
                roboRioClient.connect(String.format(ConnectRoboRioController.ROBO_RIO_MDNS_FORMAT_STRING, m_teamNumber));
                int reply = roboRioClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    m_logger.warn("FTP error connecting to the roborio, FTP code is " + reply);
                    MainApp.showErrorPopup("Error connecting to the roboRio, FTP code is " + reply, false);
                    handleBack(null);
                }
                roboRioClient.login(FTP_USER_NAME, FTP_PASSWORD);
                File jreFolder = new File(m_jreLocation);
                success = roboRioClient.storeFile(JRE_INSTALL_LOCATION, new FileInputStream(jreFolder));
            } catch (IOException e) {
                m_logger.error("Could not connect the roboRio.", e);
                Platform.runLater(() -> MainApp.showErrorScreen(e));
            }

            if (success) {
                Platform.runLater(() -> {
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("/fxml/success.fxml"));
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Could not load the success screen", e);
                        MainApp.showErrorScreen(e);
                    }
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
            m_logger.error("Could not load the connect roborio controller");
            MainApp.showErrorScreen(e);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }
}
