package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import net.mightypork.rpack.utils.DesktopApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Alerts the user to being done the download. Also opens a window to the JRE download location if the user needs to move
 * computer
 */
public class DownloadedController extends AbstractController {

    private String m_path;
    private final Logger m_logger = LogManager.getLogger();

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
        DesktopApi.open(parent);
    }

    public void handleNext(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/untar.fxml"));
        try {
            Parent root = loader.load();
            UntarController controller = loader.getController();
            controller.initialize(m_path);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not load the untar controller", e);
            MainApp.showErrorScreen(e);
        }
    }
}
