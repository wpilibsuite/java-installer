package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Takes the extracted JRE and turns it into a customized JRE for the roboRio
 */
public class CreateJreController {

    private static final String JRE_CREATE_COMMAND = "java -jar %s --dest JRE --profile compact2 --vm client --keep-debug-info --debug";

    @FXML
    private BorderPane mainView;
    @FXML
    private Label commandLabel;

    private String m_untarredLocation;
    private String m_tarLocation;
    private Thread m_JRECreateThread;

    public void initialize(String untarredLocation, String tarLocation) {
        m_untarredLocation = untarredLocation;
        m_tarLocation = tarLocation;
        m_JRECreateThread = new Thread(() -> {
            String jreCreateLibLocation = m_untarredLocation + File.separator + "lib" + File.separator + "JRECreate.jar";
            String finalCommand = String.format(JRE_CREATE_COMMAND, jreCreateLibLocation);
            Platform.runLater(() -> commandLabel.setText(finalCommand));
            try {
                // Run the JRE create Process
                Process proc = Runtime.getRuntime().exec(
                        String.format(JRE_CREATE_COMMAND, jreCreateLibLocation),
                        new String[]{
                                "EJDK_HOME=" + m_untarredLocation
                        },
                        new File(m_untarredLocation).getParentFile());
                if (proc.waitFor() != 0) {
                    String line = "";

                    // Echo the output from stdout and err
                    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }

                    Platform.runLater(() -> MainApp.showErrorPopup("Unknown error: JRE Creation failed, exit code " + proc.exitValue()));
                } else {
                    System.out.println("Successfully Created JRE!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        m_JRECreateThread.setDaemon(true);
        m_JRECreateThread.start();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        m_JRECreateThread.interrupt();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/downloaded.fxml"));
        try {
            Parent root = loader.load();
            DownloadedController controller = loader.getController();
            controller.initialize(m_tarLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }
}
