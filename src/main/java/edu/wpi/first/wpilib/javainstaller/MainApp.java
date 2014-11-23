package edu.wpi.first.wpilib.javainstaller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Main application launch point.
 */
public class MainApp extends Application {

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("/fxml/intro_screen.fxml"));

        Scene scene = new Scene(root);

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
            Platform.exit();
        }
    }
}
