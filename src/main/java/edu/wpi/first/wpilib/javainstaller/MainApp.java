package edu.wpi.first.wpilib.javainstaller;

import edu.wpi.first.wpilib.javainstaller.Controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        Parent root = loader.load(getClass().getResource("/fxml/main_view.fxml"));

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
