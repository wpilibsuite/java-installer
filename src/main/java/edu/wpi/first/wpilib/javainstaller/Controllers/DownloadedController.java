package edu.wpi.first.wpilib.javainstaller.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by Fredric on 11/22/14.
 */
public class DownloadedController extends AbstractController {

    private String m_path;

    public DownloadedController() {
        // The previous to this would be the progress controller, but we don't have a url for it, or cookies, so
        // send them back to the web page
        super("/fxml/download.fxml");
    }

    public void initialize(String path) {
        m_path = path;
    }

    /**
     * Opens the JRE in the native file browser
     *
     * @param event unused
     */
    public void handleOpenDirectory(ActionEvent event) {
        File jreFile = new File(m_path);
        File parent = jreFile.getParentFile();
        try {
            Desktop.getDesktop().open(parent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleNext(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/untar.fxml"));
        try {
            Parent root = loader.load();
            UntarController controller = loader.getController();
            controller.initialize(m_path);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
