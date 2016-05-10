package edu.wpi.first.wpilib.javainstaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import edu.wpi.first.wpilib.javainstaller.controllers.ErrorController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Main application launch point.
 */
public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger();
    private static Scene _scene;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        logger.trace("Starting application");
        Parent root = ControllerFactory.getInstance().initializeController(Arguments.Controller.WELCOME_CONTROLLER, new Arguments());

        Scene scene = new Scene(root);
        _scene = scene;

        stage.setTitle("FRC roboRIO Java Installer");
        stage.setScene(scene);
        scene.getWindow().setOnCloseRequest((windowEvent) -> {
            showExitPopup();
            windowEvent.consume();
        });
        stage.show();
    }

    /**
     * Shows a popup that exits the program on clicking yes.
     */
    public static void showExitPopup() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setContentText("Are you sure you want to quit? The roboRIO will not be set up for Java until the installer has completed.");
        Optional<ButtonType> action = alert.showAndWait();

        if (!action.isPresent() || action.get().equals(ButtonType.OK)) {
            logger.debug("Exiting installer from popup");
            Platform.exit();
        }
    }

    public static void showErrorPopup(String error) {
        showErrorPopup(error, true);
    }

    @SuppressWarnings("deprecation")
    public static void showErrorPopup(String error, boolean exit) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(error);
        alert.showAndWait();

        if (exit) {
            logger.debug("Exiting application from error popup");
            Platform.exit();
        }
    }

    @SuppressWarnings("deprecation")
    public static void showErrorScreen(Exception e) {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/error.fxml"));
        try {
            Parent root = loader.load();
            ErrorController controller = loader.getController();
            controller.initialize(e);
            _scene.setRoot(root);
        } catch (IOException ex) {
            LogManager.getLogger().error("Unknown error when displaying the logger", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Displaying Error");
            alert.setContentText("Something is really broken. Please copy the contents of the logs folder in " +
                    new File(".").getAbsolutePath() +
                    " to the FIRST discussion board");
            alert.showAndWait();
            Platform.exit();
        }
    }

    /**
     * Computes the md5 of a found JRE to ensure that it is correctly downloaded
     *
     * @param jre The jre to check
     * @return True if it passes the MD5 check
     */
    public static boolean checkJreCreator(File jre) {
        logger.debug("Found JRE, checking hash");
        return checkFileHash(jre, Arguments.JRE_CREATOR_HASH);
    }

    /**
     * Hashes a given file with the md5 algorithm, and compares it to a given hash.
     *
     * @param file    The file to verify
     * @param md5Hash The hash to check
     * @return True if the hash verifies, false if it doesn't or if there is an error hashing the
     * file
     */
    public static boolean checkFileHash(File file, String md5Hash) {
        try {
            String hashString = hashFile(file);
            logger.debug("Computed hash is " + hashString + ", official hash is " + md5Hash);
            return hashString.equalsIgnoreCase(md5Hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Could not create md5 hash", e);
            return false;

        }
    }

    public static String hashFile(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(Paths.get(file.getAbsolutePath()))) {
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
            return sb.toString();
        }
    }
}
