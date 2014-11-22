package edu.wpi.first.wpilib.javainstaller;

import edu.wpi.first.wpilib.javainstaller.Controllers.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

public class MainApp extends Application {

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("/fxml/intro_screen.fxml"));

        Scene scene = new Scene(root, 300, 275);

        stage.setTitle("FRC roboRio Java Installer");
        stage.setScene(scene);
        scene.getWindow().setOnCloseRequest((windowEvent) -> {
            showExitPopup(stage);
            windowEvent.consume();
        });
        stage.show();
    }

    public static void showExitPopup(Stage stage) {
        Action action = Dialogs.create()
                .owner(stage)
                .title("Exit")
                .message("Are you sure you want to quit? The roboRio will not be set up for Java until the installer has completed.")
                .showConfirm();

        if (action == org.controlsfx.dialog.Dialog.ACTION_YES) {
            Platform.exit();
        }
    }
}
