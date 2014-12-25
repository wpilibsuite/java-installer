package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

/**
 * This controller creates a tarball from the created JRE and creates an md5 for it.
 */
public class TarJREController extends AbstractController {

    @FXML
    private Label fileLabel;
    private final Logger m_logger = LogManager.getLogger();
    private String m_jreLocation;
    private Thread m_jreTarThread;

    public TarJREController() {
        super(false, Arguments.Controller.TAR_JRE_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        m_jreLocation = m_args.getArgument(Arguments.Argument.JRE_FOLDER);
        m_jreTarThread = new Thread(this::createTar);
        m_jreTarThread.setDaemon(true);
        m_jreTarThread.start();
    }

    /**
     * Creates the tar file with the created JRE for sending to the roborio
     */
    private void createTar() {
        File tgzFile = new File("." + File.separator + Arguments.JRE_DEFAULT_NAME);
        File tgzMd5File = new File(tgzFile.getPath() + ".md5");
        if (tgzFile.exists()) {
            tgzFile.delete();
            m_logger.debug("Removed the old tgz file");
        }
        if (tgzMd5File.exists()) {
            tgzMd5File.delete();
            m_logger.debug("Removed the old md5 file");
        }

        try {
            m_logger.debug("Starting zip");
            tgzFile.createNewFile();
            TarArchiveOutputStream tgzStream = new TarArchiveOutputStream(new GZIPOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tgzFile))));
            addFileToTarGz(tgzStream, m_jreLocation, "");
            tgzStream.finish();
            tgzStream.close();
            m_logger.debug("Finished zip, starting md5 hash");

            Platform.runLater(() -> fileLabel.setText("Generating md5 hash"));
            String md5Hash = MainApp.hashFile(tgzFile);
            OutputStream md5Out = new BufferedOutputStream(new FileOutputStream(tgzMd5File));
            md5Out.write(md5Hash.getBytes());
            md5Out.flush();
            md5Out.close();
            m_logger.debug("Finished md5 hash, hash is " + md5Hash);
            final boolean interrupted = Thread.interrupted();

            // Show the connect roboRio screen
            Platform.runLater(() -> {
                if (!interrupted) {
                    m_args.setArgument(Arguments.Argument.JRE_TAR, tgzFile.getAbsolutePath());
                    moveNext(Arguments.Controller.CONNECT_ROBORIO_CONTROLLER);
                }
            });
        } catch (IOException | NoSuchAlgorithmException e) {
            m_logger.error("Could not create the tar gz file. Do we have write permissions to the current working directory?", e);
            showErrorScreen(e);
        }
    }

    /**
     * Recursively adds a directory to a .tar.gz. Adapted from
     * http://stackoverflow.com/questions/13461393/compress-directory-to-tar-gz-with-commons-compress
     *
     * @param tOut The .tar.gz to add the directory to
     * @param path The location of the folders and files to add
     * @param base The base path of entry in the .tar.gz
     * @throws IOException Any exceptions thrown during tar creation
     */
    private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);
        Platform.runLater(() -> fileLabel.setText("Processing " + f.getPath()));

        if (f.isFile()) {
            FileInputStream fin = new FileInputStream(f);
            IOUtils.copy(fin, tOut);
            fin.close();
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }
}
