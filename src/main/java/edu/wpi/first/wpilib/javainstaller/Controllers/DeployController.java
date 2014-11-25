package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
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
    @FXML
    private Label commandLabel;

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
                Platform.runLater(() -> commandLabel.setText("Logged in"));
                File jreFolder = new File(m_jreLocation);
                success = putObject(roboRioClient, jreFolder, JRE_INSTALL_LOCATION);
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
            } else {
                m_logger.warn("Unknown error occurred when putting the JRE: failed to deploy");
                Platform.runLater(() -> MainApp.showErrorScreen(new Exception("Unknown failure when deploying the JRE to the roboRio")));
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
            m_logger.error("Could not load the connect roboRio controller");
            MainApp.showErrorScreen(e);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

    private boolean putObject(FTPClient roboRio, File object, String remoteDirectory) throws IOException {
        if (object.isDirectory()) {
            return putDirectory(roboRio, object, remoteDirectory);
        } else {
            return putFile(roboRio, object, remoteDirectory);
        }
    }

    private boolean putDirectory(FTPClient roboRio, File dir, String remoteDirectory) throws IOException {
        String dirName = dir.getName();
        String newDirectory = remoteDirectory + "/" + dirName;
        m_logger.debug("Creating remote directory " + newDirectory);
        Platform.runLater(() -> commandLabel.setText("Creating directory " + newDirectory));
        boolean success = roboRio.makeDirectory(newDirectory);
        m_logger.debug(success ? "Success" : "Failure");
        for (File file : dir.listFiles()) {
            success = success && putObject(roboRio, file, newDirectory);
        }
        return success;
    }

    private boolean putFile(FTPClient roboRio, File file, String remoteDirectory) throws IOException {
        String newName = remoteDirectory + "/" + file.getName();
        m_logger.debug("Creating remote file " + newName);
        Platform.runLater(() -> commandLabel.setText("Sending " + newName));
        boolean success = roboRio.storeFile(newName, new FileInputStream(file));
        m_logger.debug(success ? "Success" : "Failure");
        return success;
    }
}
