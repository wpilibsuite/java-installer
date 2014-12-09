package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Handles ensuring that the internet is up and running on the system before proceeding
 */
public class InternetController extends AbstractController {

    // Default name for the downloaded JRE.
    private static final String JRE_DEFAULT_NAME = "ejdk-8u6-fcs-b23-linux-arm-vfp-sflt-12_jun_2014.tar.gz";
    // The md5 hash of a fully downloaded jre
    private static final String JRE_HASH = "082F08397B0D3F63844AB472B5111C8C";

    @FXML
    private BorderPane mainView;

    @FXML
    private Button nextButton;

    @FXML
    private Button alreadyDownloadedButton;

    @FXML
    private Label textView;

    private final Logger m_logger = LogManager.getLogger();

    private boolean jreDetected = false;

    public InternetController() {
        super("/fxml/intro_screen.fxml");
    }

    @FXML
    private void initialize() {
        // Attempt to detect the JRE
        final File jre = new File(JRE_DEFAULT_NAME);
        // If the JRE is already downloaded and fully downloaded
        if (jre.exists() && checkJRE(jre)) {
            jreDetected = true;
            alreadyDownloadedButton.setText("Redownload JRE");
            textView.setText("The JRE was found already downloaded. If you want to proceed with the found JRE (recommended), click Next. If you would like to redownload the JRE, click Redownload JRE");
            alreadyDownloadedButton.setOnAction((action) -> handleInternetCheck());
            nextButton.setOnAction((action) -> sendToUntar(jre));
        }
    }

    @FXML
    public void handleInternetCheck() {
        // Handle the connection test on a background thread to avoid blocking the ui. If the user clicks next and can't
        // get to the internet, they have the ability to stop the program without waiting for a timeout
        nextButton.setDisable(true);
        nextButton.setText("Checking Internet");
        m_logger.debug("Starting internet check");
        Thread thread = new Thread(() -> {
            try {
                // Test for connection to the oracle site. If no connection, show an error
                HttpURLConnection connection = (HttpURLConnection) DownloadController.JRE_URL.openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    m_logger.debug("Internet check successful, moving to download");
                    // We have a connection, load the next page
                    Platform.runLater(() -> {
                        FXMLLoader loader = new FXMLLoader();
                        Parent root = null;
                        try {
                            root = loader.load(getClass().getResource("/fxml/download.fxml"));
                        } catch (IOException e) {
                            m_logger.error("Error when attempting to load the download window.", e);
                            MainApp.showErrorScreen(e);
                        }
                        mainView.getScene().setRoot(root);
                    });
                } else {
                    m_logger.debug("Could not connect to Oracle's website");
                    Platform.runLater(() -> {
                        try {
                            MainApp.showErrorPopup("Could not connect to the JRE website at: " + DownloadController.JRE_URL_STRING + ", error code is " + connection.getResponseCode());
                        } catch (IOException e) {
                            m_logger.error("Error when showing the could not connect to oracle popup", e);
                            MainApp.showErrorScreen(e);
                        }
                        nextButton.setDisable(false);
                        nextButton.setText("Retry >");
                    });
                }
            } catch (SocketTimeoutException e) {
                Platform.runLater(() -> {
                    m_logger.debug("Timed out when connecting to the Oracle webpage");
                    MainApp.showErrorPopup("Timed out when connecting to the Oracle webpage.");
                    nextButton.setDisable(false);
                    nextButton.setText("Retry >");
                });
            } catch (java.io.IOException e) {
                Platform.runLater(() -> {
                    m_logger.debug("Error when attempting to connect to the internet");
                    MainApp.showErrorScreen(e);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleDownloaded(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Oracle JRE");
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Compressed JRE", "*.tar.gz"));
        File jre = chooser.showOpenDialog(mainView.getScene().getWindow());
        if (jre != null) {
            sendToUntar(jre);
        }
    }

    private void sendToUntar(File jre) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/untar.fxml"));
        try {
            Parent root = loader.load();
            UntarController controller = loader.getController();
            controller.initialize(jre.getAbsolutePath());
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not load the untar screen.", e);
            MainApp.showErrorScreen(e);
        }
    }

    /**
     * Computes the md5 of a found JRE to ensure that it is correctly downloaded
     *
     * @param jre The jre to check
     * @return True if it passes the MD5 check
     */
    private boolean checkJRE(File jre) {
        m_logger.debug("Found JRE, checking hash");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get(jre.getAbsolutePath()))) {
                DigestInputStream ds = new DigestInputStream(is, md);
                byte[] input = new byte[1024];

                // Read the stream to the end to get the md5
                while (ds.read(input) != -1) {
                }

                byte[] hash = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02X", b));
                }
                String hashString = sb.toString();
                m_logger.debug("Computed hash is " + hashString + ", official hash is " + JRE_HASH);
                return hashString.equalsIgnoreCase(JRE_HASH);
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            m_logger.warn("Could not create md5 hash", e);
            return false;
        }
    }
}
