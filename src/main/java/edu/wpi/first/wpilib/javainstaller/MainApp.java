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

    public static final URL JRE_URL;
    public static final String JRE_URL_STRING = "http://www.oracle.com/technetwork/java/embedded/embedded-se/downloads/javase-embedded-downloads-2209751.html";

    // Jump through hoops to take care of a possible MalformedURLException when initializing the JRE url
    static {
        URL temp;
        try {
            temp = new URL(JRE_URL_STRING);
        } catch (MalformedURLException e) {
            temp = null;
            e.printStackTrace();
        }
        JRE_URL = temp;

    }

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
        Dialogs.create()
                .title("Error")
                .message(error)
                .showError();
    }
}
