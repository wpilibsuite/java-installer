package edu.wpi.first.wpilib.javainstaller.Controllers;

import com.jcraft.jsch.*;
import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Deploys the JRE to the roboRio
 */
public class DeployController {
    private static final String JRE_INSTALL_LOCATION = "/usr/local/frc";
    // File to save compress the jre to
    private static final String JRE_TGZ_NAME = "JRE.tar.gz";
    private static final String EXTRACT_DIR = "/home/admin/";
    private static final String JRE_TGZ_LOCATION = EXTRACT_DIR + JRE_TGZ_NAME;
    private static final String JRE_EXTRACT_DIR = EXTRACT_DIR + "JRE";
    private static final String SCP_T_COMMAND = "scp -p -t " + JRE_TGZ_LOCATION;
    private static final String EXEC_COMMAND = "exec";
    private static final int SUCCESS = 0;
    private static final int BUFF_SIZE = 1024;
    // The roborio login is admin@roborio-XXXX.local, with a blank password
    private static final String USER_NAME = "admin";

    @FXML
    private BorderPane mainView;
    @FXML
    private Label commandLabel;

    private String m_tarLocation;
    private String m_jreLocation;
    private JSch m_jSch = new JSch();

    private final Logger m_logger = LogManager.getLogger();

    public void initialize(String tarLocation, String jreLocation, int teamNumber) {
        m_tarLocation = tarLocation;
        m_jreLocation = jreLocation;
        Thread ftpThread = new Thread(() -> {
            // Turn the jre into a tar gz for ease of transfer
            Platform.runLater(() -> commandLabel.setText("Tarring created JRE"));
            File tgzFile = new File(JRE_TGZ_NAME);
            createTar(tgzFile);

            JSch.setConfig("StrictHostKeyChecking", "no");
            String roboRioHost = String.format(ConnectRoboRioController.ROBO_RIO_MDNS_FORMAT_STRING, teamNumber);
            try {
                Session roboRioSession = m_jSch.getSession(USER_NAME, roboRioHost);
                roboRioSession.setPassword("");
                roboRioSession.connect();
                scpFile(tgzFile, roboRioSession);
                extractJRE(roboRioSession);

                Platform.runLater(() -> {
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("/fxml/success.fxml"));
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Could not load the success screen", e);
                        MainApp.showErrorScreen(e);
                    }
                });
            } catch (JSchException | IOException e) {
                m_logger.error("Could not connect to the roboRio", e);
                Platform.runLater(() -> MainApp.showErrorPopup("Could not connect to the roboRio.", false));
                Platform.runLater(() -> handleBack(null));
            }
        });
        ftpThread.setDaemon(true);
        ftpThread.start();
    }

    private void createTar(File tgzFile) {
        if (tgzFile.exists()) {
            tgzFile.delete();
            m_logger.debug("Removed the old tgz file");
        }
        try {
            m_logger.debug("Starting zip");
            tgzFile.createNewFile();
            TarArchiveOutputStream tgzStream = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tgzFile))));
            addFileToTarGz(tgzStream, m_jreLocation, "");
            tgzStream.finish();
            tgzStream.close();
        } catch (IOException e) {
            m_logger.error("Could not create the tar gz file. Do we have write permissions to the current working directory?", e);
            MainApp.showErrorScreen(e);
        }
    }

    private void scpFile(File tgzFile, Session roboRioSession) throws JSchException, IOException {
        Platform.runLater(() -> commandLabel.setText("Copying " + tgzFile.getAbsolutePath() + " to the roboRio"));

        // Execute the scp -t /home/admin/JRE.tar.gz command. This opens a channel waiting for the data on the
        // roboRio side. This code is adapted from an official JSch example, found here:
        // http://www.jcraft.com/jsch/examples/ScpTo.java.html
        Channel scpChannel = roboRioSession.openChannel(EXEC_COMMAND);
        OutputStream scpOutput = scpChannel.getOutputStream();
        InputStream scpInput = scpChannel.getInputStream();
        ((ChannelExec) scpChannel).setCommand(SCP_T_COMMAND);
        scpChannel.connect();

        // Create our command for sending the file size
        String command = "C0644 " + tgzFile.length() + " " + JRE_TGZ_NAME + "\n";
        m_logger.debug("Size command is " + command);
        scpOutput.write(command.getBytes());
        scpOutput.flush();
        if (checkAck(scpInput) != SUCCESS) {
            throw new IOException("Unknown error when sending file size");
        }

        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(tgzFile));
        byte[] buffer = new byte[BUFF_SIZE];
        while (true) {
            int len = fis.read(buffer);
            if (len < 0) {
                break;
            }
            scpOutput.write(buffer);
        }
        fis.close();
        // Send the final \0
        scpOutput.write('\0');
        scpOutput.flush();
        if (checkAck(scpInput) != SUCCESS) {
            throw new IOException("Failure when scping the JRE");
        }
        scpOutput.close();
        scpInput.close();
        scpChannel.disconnect();
    }

    private void extractJRE(Session roboRioSession) throws JSchException, IOException {
        // Remove the old JRE, if it exists. If it doesn't, this will fail, and we don't care if this one fails, so just log it
        try {
            m_logger.debug("Attempting to remove old JRE");
            Platform.runLater(() -> commandLabel.setText("Cleaning up existing Java, if it exists"));
            executeCommand(roboRioSession, "rm -r " + JRE_INSTALL_LOCATION + "/JRE", "");
            m_logger.debug("Removed old JRE");
        } catch (JSchException | IOException e) {
            m_logger.debug("No JRE folder removed");
        }

        // Extract the JRE
        m_logger.debug("Extracting the JRE");
        Platform.runLater(() -> commandLabel.setText("Extracting JRE"));
        executeCommand(roboRioSession, "tar -xzf " + JRE_TGZ_LOCATION + " -C " + EXTRACT_DIR, "Error when extracting the tar gz: exit code");
        m_logger.debug("Extracted the JRE");

        // Move the directory to the install dir
        m_logger.debug("Moving the JRE");
        Platform.runLater(() -> commandLabel.setText("Moving JRE to " + JRE_INSTALL_LOCATION));
        executeCommand(roboRioSession, "mv " + JRE_EXTRACT_DIR + " " + JRE_INSTALL_LOCATION, "Error when moving the JRE to the install location");
        m_logger.debug("Moved the JRE");

        // Add permissions to the bin directory
        m_logger.debug("Setting permissions");
        Platform.runLater(() -> commandLabel.setText("Setting permissions"));
        executeCommand(roboRioSession, "chmod +x " + JRE_INSTALL_LOCATION + "/JRE/bin/*", "Error when setting permissions on the executables");
        m_logger.debug("Set permissions");

        // Delete the scp'd tar gz
        m_logger.debug("Cleaning up");
        Platform.runLater(() -> commandLabel.setText("Cleaning up installer files"));
        executeCommand(roboRioSession, "rm " + JRE_TGZ_LOCATION, "Could not clean up. The roboRio MIGHT be ok to use, but you should submit the error logs anyway");
        m_logger.debug("Cleaned up");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect_roborio.fxml"));
        try {
            Parent root = loader.load();
            ConnectRoboRioController controller = loader.getController();
            controller.initialize(m_tarLocation, m_jreLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not load the connect roboRio controller");
            MainApp.showErrorScreen(e);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

    private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);
        m_logger.debug("Processing file " + f.getAbsolutePath() + ", putting tar entry is " + entryName);

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

    private void executeCommand(Session roboRioSession, String command, String errorString) throws JSchException, IOException {
        // Extract the JRE
        m_logger.debug("Running command " + command);
        ChannelExec extractChannel = (ChannelExec) roboRioSession.openChannel(EXEC_COMMAND);
        extractChannel.setCommand(command);
        extractChannel.connect();
        int sleepCount = 0;
        // Sleep for up to 10 seconds while we wait for the command to execute, checking every 100 milliseconds
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                m_logger.warn("Interrupted exception while waiting", e);
            }
        } while (!extractChannel.isClosed() && sleepCount++ < 100);

        int res = extractChannel.getExitStatus();
        if (res != SUCCESS) {
            m_logger.debug("Error with command " + command);
            throw new IOException(errorString + " " + res);
        }
        extractChannel.disconnect();
    }

    /**
     * Checks the return code of an ssh command. Taken from http://www.jcraft.com/jsch/examples/ScpTo.java.html.
     *
     * @param in The input stream of the channel
     * @return The return code
     * @throws IOException If a read error occurs
     */
    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        m_logger.debug("Return code is " + b);
        if (b == 0) return b;
        if (b == -1) return b;
        StringBuilder sb = new StringBuilder();
        int c;
        do {
            c = in.read();
            sb.append((char) c);
        }
        while (c != '\n');
        m_logger.error("Acknowledge error:");
        m_logger.error(sb.toString());
        return b;
    }
}
