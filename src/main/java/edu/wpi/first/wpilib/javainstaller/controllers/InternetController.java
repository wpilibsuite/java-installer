package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

/**
 * Handles ensuring that the internet is up and running on the system before proceeding
 */
public class InternetController extends AbstractController {

    private final String NO_JRE_STRING = "For this first step, we need to be connected to the internet." +
            " To proceed, click next. If you cannot get internet on this computer, you can run this installer" +
            " on another computer to download the JRE, and then transfer it with a flash drive" +
            " and click Already Downloaded.";
    private final String JRE_FOUND_STRING = "The JRE was found already downloaded. If you want to proceed with" +
            " the found JRE (recommended), click Next. If you would like to redownload the JRE, click Redownload JRE";

    @FXML
    private Button alreadyDownloadedButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label textView;

    private final Logger m_logger = LogManager.getLogger();

    public InternetController() {
        super(true, Arguments.Controller.INTERNET_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        // Set off the check JRE thread
        nextButton.setDisable(true);
        Thread checkJREThread = new Thread(() -> checkExistingJRE(Arguments.JRE_DEFAULT_NAME, Arguments.JRE_CREATOR_DEFAULT_NAME));
        checkJREThread.setDaemon(true);
        checkJREThread.start();
    }

    @FXML
    public void handleInternetCheck(ActionEvent event) {
        // Handle the connection test on a background thread to avoid blocking the ui. If the user clicks next and can't
        // get to the internet, they have the ability to stop the program without waiting for a timeout
        nextButton.setDisable(true);
        nextButton.setText("Checking Internet");
        m_logger.debug("Starting internet check");
        Thread thread = new Thread(this::checkInternet);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleDownloaded(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Oracle JRE");
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Compressed JRE", "*.tar.gz"));
        m_logger.debug("Showing choose JRE picker");
        File jre = chooser.showOpenDialog(mainView.getScene().getWindow());
        if (jre != null) {
            new Thread(() -> {
                if (checkExistingJRE(jre.getAbsolutePath(), jre.getAbsolutePath())) {
                    Platform.runLater(nextButton::fire);
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setHeaderText(null);
                        alert.setTitle("Invalid JRE");
                        alert.setContentText("This JRE does not pass file verification. Please choose another " +
                                "or redownload the JRE. If this is a fully created JRE, it must have" +
                                " an md5 file containing the signature. The file should have the same " +
                                "name, with a .md5 appended to the file extension.");
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    /**
     * Checks to see if either a previous jre or the jre creator have already been downloaded. If they have, it will
     * configure the view to move onto the appropriate step. If they have not, then it will configure the controller
     * to move onto the internet check
     */

    private boolean checkExistingJRE(String jreName, String jreCreatorName) {
        final File jre = new File(jreName);
        final File jreMd5 = new File(jreName + ".md5");
        final File jreCreator = new File(jreCreatorName);

        // First, check to see if the jre was previously created
        if (jre.exists() && jreMd5.exists()) {
            BufferedReader md5Reader = null;
            try {
                md5Reader = new BufferedReader(new FileReader(jreMd5));
                String md5 = md5Reader.readLine();
                if (MainApp.checkFileHash(jre, md5)) {
                    m_logger.debug("Found created JRE, setting next controller to be connect roborio");
                    m_args.setArgument(Arguments.Argument.JRE_TAR, jre.getAbsolutePath());
                    setupNextButton(Arguments.Controller.CONNECT_ROBORIO_CONTROLLER);
                    return true;
                }
            } catch (IOException e) {
                m_logger.warn("Error when attempting to verify the existing JRE", e);
            } finally {
                if (md5Reader != null) {
                    try {
                        md5Reader.close();
                    } catch (IOException e) {
                        m_logger.warn("Error when closing the md5 file stream", e);
                    }
                }
            }
        }

        // If that didn't check out, check for the jre creator
        if (jreCreator.exists() && MainApp.checkJreCreator(jreCreator)) {
            m_logger.debug("Found JRE creator, setting next controller to be untar");
            m_args.setArgument(Arguments.Argument.JRE_CREATOR_TAR, jreCreator.getAbsolutePath());
            setupNextButton(Arguments.Controller.UNTAR_CONTROLLER);
            return true;
        }

        // We didn't find either one, so setup the main view for downloading the JRE
        Platform.runLater(() -> {
            m_logger.debug("No valid JRE found");
            textView.setText(NO_JRE_STRING);
            nextButton.setOnAction(this::handleInternetCheck);
            nextButton.setDisable(false);
        });
        return false;
    }

    /**
     * Sets up the view to show that the JRE or JRE creator were found already downloaded, and to move onto the
     * correct places when the buttons are pressed.
     *
     * @param controller The controller to move onto when the next button is pressed
     */
    private void setupNextButton(Arguments.Controller controller) {
        Platform.runLater(() -> {
            alreadyDownloadedButton.setText("Redownload JRE");
            textView.setText(JRE_FOUND_STRING);
            alreadyDownloadedButton.setOnAction(this::handleInternetCheck);
            nextButton.setOnAction((action) -> moveNext(controller));
            nextButton.setDisable(false);
        });
    }

    /**
     * Checks the internet connection. If the connection is valid, it moves to the next controller. If not, then it sets
     * up the button to retry
     */
    private void checkInternet() {
        try {
            // Test for connection to the oracle site. If no connection, show an error
            HttpURLConnection connection = (HttpURLConnection) DownloadController.JRE_URL.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                m_logger.debug("Internet check successful, moving to download");
                // We have a connection, load the next page
                Platform.runLater(() -> {
                    moveNext(Arguments.Controller.DOWNLOAD_CONTROLLER);
                });
            } else {
                m_logger.debug("Could not connect to Oracle's website");
                Platform.runLater(() -> {
                    try {
                        showErrorPopup("Could not connect to the JRE website at: " +
                                        DownloadController.JRE_URL_STRING +
                                        ", error code is " +
                                        connection.getResponseCode(),
                                false);
                    } catch (IOException e) {
                        m_logger.error("Error when showing the could not connect to oracle popup", e);
                        showErrorScreen(e);
                    }
                    nextButton.setDisable(false);
                    nextButton.setText("Retry >");
                });
            }
        } catch (SocketTimeoutException e) {
            Platform.runLater(() -> {
                m_logger.debug("Timed out when connecting to the Oracle webpage");
                showErrorPopup("Timed out when connecting to the Oracle webpage.", false);
                nextButton.setDisable(false);
                nextButton.setText("Retry >");
            });
        } catch (java.io.IOException e) {
            Platform.runLater(() -> {
                m_logger.debug("Error when attempting to connect to the internet");
                showErrorScreen(e);
            });
        }
    }
}
