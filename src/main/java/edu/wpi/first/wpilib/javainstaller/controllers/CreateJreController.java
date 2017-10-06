package edu.wpi.first.wpilib.javainstaller.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Takes the extracted JRE and turns it into a customized JRE for the roboRio
 */
public class CreateJreController extends AbstractController {

    private static String[] JRE_COMPACT_CREATE_COMMAND = {"java",
            "-jar",
            "",
            "--dest", "JRE",
            "--profile", "compact2",
            "--vm", "client",
            "--keep-debug-info",
            "--debug"
    };

    private static String[] JRE_FULL_CREATE_COMMAND = {"java",
            "-jar",
            "",
            "--dest", "JRE",
            "--vm", "client",
            "--keep-debug-info",
            "--debug"
    };

    @FXML
    private Label commandLabel;

    private String m_jreCreatorFolder;
    private Thread m_JRECreateThread;
    private final Logger m_logger = LogManager.getLogger();

    public CreateJreController() {
        super(false, Arguments.Controller.CREATE_JRE_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        m_jreCreatorFolder = m_args.getArgument(Arguments.Argument.JRE_CREATOR_FOLDER);
        m_JRECreateThread = new Thread(this::createJRE);
        m_JRECreateThread.setDaemon(true);
        m_JRECreateThread.start();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        m_JRECreateThread.interrupt();
        super.handleBack(event);
    }

    private void createJRE() {
        String[] JRE_CREATE_COMMAND = m_args.getArgument(Arguments.Argument.USE_COMPACT_JRE).equals("TRUE") ? JRE_COMPACT_CREATE_COMMAND : JRE_FULL_CREATE_COMMAND;
        final String jreCreatorLibLocation = m_jreCreatorFolder + File.separator + "lib" + File.separator + "JRECreate.jar";
        JRE_CREATE_COMMAND[2] = jreCreatorLibLocation;
        m_logger.debug("Staring JRE Creation");
        m_logger.debug("Creator location: " + jreCreatorLibLocation);
        m_logger.debug("Command: " + Arrays.toString(JRE_CREATE_COMMAND));
        m_logger.debug("EJDK_HOME: " + m_jreCreatorFolder);
        Platform.runLater(() -> commandLabel.setText(Arrays.toString(JRE_CREATE_COMMAND)));
        try {
            // If the JRE folder already exists the create process will fail, so delete the folder if it exists
            final String jreLocation = new File(m_jreCreatorFolder).getParent() + File.separator + "JRE";
            File jreFolder = new File(jreLocation);
            if (jreFolder.exists()) {
                deleteFolder(jreFolder);
            }

            // Run the JRE create Process
            Process proc = Runtime.getRuntime().exec(
                    JRE_CREATE_COMMAND,
                    new String[]{
                            "EJDK_HOME=" + m_jreCreatorFolder
                    },
                    new File(m_jreCreatorFolder).getParentFile());
            if (proc.waitFor() != 0) {
                String line = "";
                StringBuilder output = new StringBuilder();
                m_logger.error("JRE Creation failed, starting output");
                m_logger.error("JRE Creation stdOut follows:");
                // Echo the output from stdout and err
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
                m_logger.error(output.toString());
                m_logger.error("JRE Creation stdOut end");
                m_logger.error("JRE Creation stdErr start");
                output = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
                m_logger.error(output.toString());

                Platform.runLater(() -> showErrorScreen(new Exception("JRE Creation failed, exit code " + proc.exitValue())));
            } else {
                m_logger.debug("Successfully Created JRE!");
                Platform.runLater(() -> {
                    m_args.setArgument(Arguments.Argument.JRE_FOLDER, jreLocation);
                    moveNext(Arguments.Controller.TAR_JRE_CONTROLLER);
                });
            }
        } catch (IOException e) {
            m_logger.error("Could not create the custom JRE!", e);
            Platform.runLater(() -> showErrorScreen(e));
        } catch (InterruptedException e) {
            m_logger.debug("Interrupted while creating the JRE");
        }
    }

    /**
     * Recursively deletes a folder and all subfolders
     *
     * @param obj The directory to delete
     */
    private void deleteFolder(File obj) {
        if (obj.isFile()) {
            obj.delete();
        } else {
            for (File file : obj.listFiles()) {
                deleteFolder(file);
            }
            obj.delete();
        }
    }
}
