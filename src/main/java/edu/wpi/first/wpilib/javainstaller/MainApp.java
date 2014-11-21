package edu.wpi.first.wpilib.javainstaller;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application {

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    private static Stage mainStage;

    public static Stage getMainStage() {
        return mainStage;
    }

    public void start(Stage stage) throws Exception {
        mainStage = stage;

        FXMLLoader loader = new FXMLLoader();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main_view.fxml"));

        Scene scene = new Scene(root, 300, 275);

        stage.setTitle("FRC roboRio Java Installer");
        stage.setScene(scene);
        scene.getWindow().setOnCloseRequest((windowEvent) -> {
            MainController.showExitPopup(stage);
            windowEvent.consume();
        });
        stage.show();
    }
}
