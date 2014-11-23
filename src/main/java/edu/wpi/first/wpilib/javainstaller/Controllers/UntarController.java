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

    public void initialize(String jrePath) {
        jreFile = new File(jrePath);
        decompressThread = new Thread(() -> {
            // Open the jar file
            TarArchiveInputStream jreTar = null;
            File saveDir = jreFile.getParentFile();
            try {
                jreTar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(jreFile))));
                TarArchiveEntry entry = jreTar.getNextTarEntry();
                String firstDirectory = null;
                while (entry != null) {
                    File destPath = new File(saveDir, entry.getName());
                    final TarArchiveEntry finalEntry = entry;
                    Platform.runLater(() -> fileNameLabel.setText(finalEntry.getName()));
                    System.out.println("Untarring " + entry.getName());
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

                            while ((readBytes = jreTar.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                                output.write(buffer, 0, readBytes);
                            }
                        } catch (IOException | IllegalArgumentException e) {
                            System.err.println("Couldn't untar entry " + entry.getName());
                            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (jreTar != null) {
                    try {
                        jreTar.close();
                    } catch (IOException e) {
                        e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }
}
