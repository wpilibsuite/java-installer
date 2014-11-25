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
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.io.IOException;

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
    public static void showExitPopup() {
        // TODO: When JDK8u40 is release (estimated March 2015) update this to official APIs
        Action action = Dialogs.create()
                .title("Exit")
                .message("Are you sure you want to quit? The roboRio will not be set up for Java until the installer has completed.")
                .showConfirm();

        if (action == org.controlsfx.dialog.Dialog.ACTION_YES) {
            logger.debug("Exiting installer from popup");
            Platform.exit();
        }
    }

    public static void showErrorPopup(String error) {
        showErrorPopup(error, true);
    }

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
}
