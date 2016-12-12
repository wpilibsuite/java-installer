package edu.wpi.first.wpilib.javainstaller.controllers;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Deploys the JRE to the roboRio
 */
public class DeployController extends AbstractController {
    private static final String JRE_INSTALL_LOCATION = "/usr/local/frc";
    // File to save compress the jre to
    public static final String JRE_TGZ_NAME = "JRE.tar.gz";
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
    private Label commandLabel;

    private String m_ipAddress;
    private String m_jreTarLocation;
    private JSch m_jSch = new JSch();

    private final Logger m_logger = LogManager.getLogger();

    public DeployController() {
        super(false, Arguments.Controller.DEPLOY_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        m_ipAddress = m_args.getArgument(Arguments.Argument.IP);
        m_jreTarLocation = m_args.getArgument(Arguments.Argument.JRE_TAR);
        Thread sshThread = new Thread(this::transferJRE);
        sshThread.setDaemon(true);
        sshThread.start();
    }

    private void transferJRE() {
        JSch.setConfig("StrictHostKeyChecking", "no");
        try {
            Session roboRioSession = m_jSch.getSession(USER_NAME, m_ipAddress);
            roboRioSession.setPassword("");
            roboRioSession.connect();
            scpFile(new File(m_jreTarLocation), roboRioSession);
            setupJRE(roboRioSession);

            Platform.runLater(() -> moveNext(Arguments.Controller.SUCCESS_CONTROLLER));
        } catch (JSchException | IOException e) {
            m_logger.error("Failure to send JRE to the roboRIO", e);
            showErrorScreen(e);
        }
    }

    /**
     * Scps a file to the given roboRio session.
     *
     * @param tgzFile        The tar file to scp
     * @param roboRioSession The roboRio to scp to
     * @throws JSchException If a ssh error occurs
     * @throws IOException   If an IO error occurs
     */
    private void scpFile(File tgzFile, Session roboRioSession) throws JSchException, IOException {
        Platform.runLater(() -> commandLabel.setText("Copying " + tgzFile.getAbsolutePath() + " to the roboRIO"));

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

    /**
     * Sets up the newly scp'd JRE on the roboRio. It removes any existing JRE, untars the new JRE,
     * moves it to the correct location, sets permissions on the bin folder, and removes the
     * temporary tar file
     *
     * @param roboRioSession The roborio to set up
     * @throws JSchException If a ssh error occurs
     * @throws IOException   If an IO error occurs
     */
    private void setupJRE(Session roboRioSession) throws JSchException, IOException {
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
        executeCommand(roboRioSession, "setcap 'cap_sys_nice=pe' " + JRE_INSTALL_LOCATION + "/JRE/bin/java", "Error setting priority capability");
        executeCommand(roboRioSession, "echo '/usr/local/frc/JRE/lib/arm/jli' > /etc/ld.so.conf.d/java.conf", "Error setting ld.so.conf");
        m_logger.debug("Set permissions");

        // Delete the scp'd tar gz
        m_logger.debug("Cleaning up");
        Platform.runLater(() -> commandLabel.setText("Cleaning up installer files"));
        executeCommand(roboRioSession, "rm " + JRE_TGZ_LOCATION, "Could not clean up. The roboRIO MIGHT be ok to use, but you should submit the error logs anyway");
        executeCommand(roboRioSession, "reboot", "Could not reboot");
        m_logger.debug("Cleaned up");
    }

    /**
     * Convenience method to execute a command on the given remote session, using the given string
     * as part of the error if it fails to run.
     *
     * @param roboRioSession The ssh session of the roboRio
     * @param command        The command to execute
     * @param errorString    The error string to put in the exception if an error occurs. The return
     *                       code will be appended to the end
     * @throws JSchException If an ssh error occurs
     * @throws IOException   Thrown if there is an io error, or if the command fails to run
     */
    private void executeCommand(Session roboRioSession, String command, String errorString) throws JSchException, IOException {
        // Extract the JRE
        m_logger.debug("Running command " + command);
        ChannelExec channel = (ChannelExec) roboRioSession.openChannel(EXEC_COMMAND);
        channel.setCommand(command);
        channel.connect();
        int sleepCount = 0;
        // Sleep for up to 10 seconds while we wait for the command to execute, checking every 100 milliseconds
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                m_logger.warn("Interrupted exception while waiting for command " + command + " to finish", e);
            }
        } while (!channel.isClosed() && sleepCount++ < 100);

        int res = channel.getExitStatus();
        if (res != SUCCESS) {
            m_logger.debug("Error with command " + command);
            throw new IOException(errorString + " " + res);
        }
        channel.disconnect();
    }

    /**
     * Checks the return code of an ssh command. Adapted from http://www.jcraft.com/jsch/examples/ScpTo.java.html.
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
        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            m_logger.error("Acknowledge error:");
            m_logger.error(sb.toString());
        }
        return b;
    }
}
