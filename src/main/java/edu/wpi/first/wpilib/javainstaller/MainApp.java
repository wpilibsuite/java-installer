package edu.wpi.first.wpilib.javainstaller;

import edu.wpi.first.wpilib.javainstaller.Controllers.ErrorController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Main application launch point.
 */
public class MainApp extends Application {

    // The md5 hash of a fully downloaded jre
    private static final String JRE_HASH = "082F08397B0D3F63844AB472B5111C8C";
    private static final Logger logger = LogManager.getLogger();
    private static Scene _scene;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        logger.trace("Starting application");
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("/fxml/intro_screen.fxml"));

        Scene scene = new Scene(root);
        _scene = scene;

        stage.setTitle("FRC roboRio Java Installer");
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
    @SuppressWarnings("deprecation")
    public static void showExitPopup() {
        // TODO: When JDK8u40 is release (estimated March 2015) update this to official APIs
        Action action = Dialogs.create()
                .title("Exit")
                .message("Are you sure you want to quit? The roboRio will not be set up for Java until the installer has completed.")
                .actions(Dialog.ACTION_YES, Dialog.ACTION_NO)
                .showConfirm();

        if (action == Dialog.ACTION_YES) {
            logger.debug("Exiting installer from popup");
            Platform.exit();
        }
    }

    public static void showErrorPopup(String error) {
        showErrorPopup(error, true);
    }

    @SuppressWarnings("deprecation")
    public static void showErrorPopup(String error, boolean exit) {
        Dialogs.create()
                .title("Error")
                .message(error)
                .showError();
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
            Dialogs.create()
                    .title("Error Displaying Error")
                    .message("Something is really broken. Please copy the contents of the logs folder in " +
                            new File(".").getAbsolutePath() +
                            " to the FIRST discussion board")
                    .showError();
            Platform.exit();
        }
    }

    /**
     * Computes the md5 of a found JRE to ensure that it is correctly downloaded
     *
     * @param jre The jre to check
     * @return True if it passes the MD5 check
     */
    public static boolean checkJre(File jre) {
        logger.debug("Found JRE, checking hash");
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
                logger.debug("Computed hash is " + hashString + ", official hash is " + JRE_HASH);
                return hashString.equalsIgnoreCase(JRE_HASH);
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Could not create md5 hash", e);
            return false;

        }
    }
}
