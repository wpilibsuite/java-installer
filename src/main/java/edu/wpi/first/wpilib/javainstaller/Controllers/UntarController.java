package edu.wpi.first.wpilib.javainstaller.Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Prompts the user to either move the JRE zip to a computer that has access to the roboRio, or tells them to move
 * to connect to the robot
 */
public class UntarController extends AbstractController {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    @FXML
    private Label fileNameLabel;

    private File jreFile;
    private Thread decompressThread;

    public UntarController() {
        super("/fxml/downloaded.fxml");
    }

    public void initialize(String jrePath) {
        jreFile = new File(jrePath);
        decompressThread = new Thread(() -> {
            // Open the jar file
            TarArchiveInputStream jreTar = null;
            File saveDir = jreFile.getParentFile();
            try {
                System.out.println("Opening tar");
                jreTar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(jreFile))));
                System.out.println("Tar opened");
                TarArchiveEntry entry = jreTar.getNextTarEntry();
                while (entry != null) {
                    File destPath = new File(saveDir, entry.getName());
                    final TarArchiveEntry finalEntry = entry;
                    Platform.runLater(() -> fileNameLabel.setText(finalEntry.getName()));
                    System.out.println("Untarring " + entry.getName());
                    if (entry.isDirectory()) {
                        // If the entry is a directory, then make the directories in the destination
                        destPath.mkdirs();
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
    @Override
    public void handleBack(ActionEvent event) {
        decompressThread.interrupt();
        super.handleBack(event);
    }
}
