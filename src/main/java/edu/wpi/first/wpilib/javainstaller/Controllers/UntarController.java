package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Prompts the user to either move the JRE zip to a computer that has access to the roboRio, or tells them to move
 * to connect to the robot
 */
public class UntarController {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    @FXML
    private BorderPane mainView;

    @FXML
    private Label fileNameLabel;

    private File jreFile;
    private Thread decompressThread;
    private final Logger m_logger = LogManager.getLogger();

    public void initialize(String jrePath) {
        jreFile = new File(jrePath);
        decompressThread = new Thread(() -> {
            // Open the jar file
            TarArchiveInputStream jreTar = null;
            File saveDir = jreFile.getParentFile();
            m_logger.debug("Starting decompress of file " + jrePath + " to " + saveDir.getAbsolutePath());
            try {
                jreTar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(jreFile))));
                TarArchiveEntry entry = jreTar.getNextTarEntry();
                // We save the first directory that we see to extract the inner folder name later.
                String firstDirectory = null;
                while (entry != null) {
                    File destPath = new File(saveDir, entry.getName());
                    final TarArchiveEntry finalEntry = entry;
                    Platform.runLater(() -> fileNameLabel.setText(finalEntry.getName()));
                    m_logger.debug("Untarring " + entry.getName());
                    if (entry.isDirectory()) {
                        // If the entry is a directory, then make the directories in the destination
                        destPath.mkdirs();
                        // If this is the first directory we've made, save the string
                        if (firstDirectory == null) {
                            firstDirectory = entry.getName();
                        }
                    } else {
                        // Otherwise, read in the entry and write it to the disk
                        BufferedOutputStream output = null;
                        try {
                            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                            int readBytes = 0;
                            output = new BufferedOutputStream(new FileOutputStream(destPath));

                            // Write the tar entry to the current file
                            while ((readBytes = jreTar.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                                output.write(buffer, 0, readBytes);
                            }
                        } catch (IOException | IllegalArgumentException e) {
                            m_logger.warn("Couldn't untar entry " + entry.getName(), e);
                        } finally {
                            if (output != null) {
                                output.flush();
                                output.close();
                            }
                        }

                    }
                    entry = jreTar.getNextTarEntry();
                }
                final String finalFirstDirectory = firstDirectory;
                Platform.runLater(() -> {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_jre.fxml"));
                    try {
                        Parent root = loader.load();
                        CreateJreController controller = loader.getController();
                        assert finalFirstDirectory != null;
                        controller.initialize(saveDir.getAbsolutePath() + File.separator + finalFirstDirectory.substring(0, finalFirstDirectory.indexOf("/")),
                                jreFile.getAbsolutePath());
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Could not load create jre window", e);
                        MainApp.showErrorScreen(e);
                    }
                });
            } catch (IOException e) {
                m_logger.error("Exception when untarring the JRE", e);
                Platform.runLater(() -> MainApp.showErrorScreen(e));
            } finally {
                if (jreTar != null) {
                    try {
                        jreTar.close();
                    } catch (IOException e) {
                        m_logger.warn("Exception when closing the tar stream", e);
                    }
                }
            }
        });
        decompressThread.setDaemon(true);
        decompressThread.start();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        decompressThread.interrupt();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/downloaded.fxml"));
        try {
            Parent root = loader.load();
            DownloadedController controller = loader.getController();
            controller.initialize(jreFile.getAbsolutePath());
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not load the downloaded view");
            MainApp.showErrorScreen(e);
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }
}
