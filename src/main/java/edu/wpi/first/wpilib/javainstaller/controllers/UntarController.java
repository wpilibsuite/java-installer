package edu.wpi.first.wpilib.javainstaller.controllers;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Prompts the user to either move the JRE zip to a computer that has access to the roboRio, or
 * tells them to move to connect to the robot
 */
public class UntarController extends AbstractController {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    @FXML
    private Label fileNameLabel;

    private File jreCreatorTar;
    private Thread decompressThread;
    private final Logger m_logger = LogManager.getLogger();

    public UntarController() {
        super(false, Arguments.Controller.UNTAR_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        jreCreatorTar = new File(m_args.getArgument(Arguments.Argument.JRE_CREATOR_TAR));
        decompressThread = new Thread(this::untarJRECreator);
        decompressThread.setDaemon(true);
        decompressThread.start();
    }

    @FXML
    @Override
    public void handleBack(ActionEvent event) {
        decompressThread.interrupt();
        super.handleBack(event);
    }

    @FXML
    @Override
    protected void handleCancel(ActionEvent event) {
        super.handleBack(event);
    }

    private void untarJRECreator() {
        // Open the jar file
        TarArchiveInputStream jreTar = null;
        File saveDir = jreCreatorTar.getParentFile();
        m_logger.debug("Starting decompress of file " + jreCreatorTar.getAbsolutePath() + " to " + saveDir.getAbsolutePath());
        try {
            jreTar = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(jreCreatorTar))));
            TarArchiveEntry entry = jreTar.getNextTarEntry();
            // We save the first directory that we see to extract the inner folder name later.
            String firstDirectory = null;
            while (entry != null) {
                File destPath = new File(saveDir, entry.getName());
                final TarArchiveEntry finalEntry = entry;
                Platform.runLater(() -> fileNameLabel.setText(finalEntry.getName()));
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
            final boolean interrupted = Thread.interrupted();
            Platform.runLater(() -> {
                // Ensure that we weren't interrupted during the untar process
                if (interrupted) {
                    return;
                }

                // Check to make sure the first directory is not null. If it is, there was an error untarring that
                // wasn't caught.
                if (finalFirstDirectory == null) {
                    m_logger.error("An unknown error when untarring has occurred: The first directory is null");
                    showErrorScreen(new IllegalArgumentException("An unknown error has occurred."));
                    return;
                }
                m_args.setArgument(Arguments.Argument.JRE_CREATOR_FOLDER,
                        saveDir.getAbsolutePath() +
                                File.separator +
                                // This is the first directory entry that was untarred to. We find up until the first "/"
                                // argument to get the top directory. Because this is generated by the apache commons
                                // untar library, it is always "/", even on Windows systems
                                finalFirstDirectory.substring(0, finalFirstDirectory.indexOf("/")));
                moveNext(Arguments.Controller.SELECT_JRE_CONTROLLER);
            });
        } catch (IOException e) {
            m_logger.error("Exception when untarring the JRE", e);
            Platform.runLater(() -> showErrorScreen(e));
        } finally {
            if (jreTar != null) {
                try {
                    jreTar.close();
                } catch (IOException e) {
                    m_logger.warn("Exception when closing the tar stream", e);
                }
            }
        }
    }
}
