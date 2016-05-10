package edu.wpi.first.wpilib.javainstaller.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Starts the download and shows the user the download progress
 */
public class DownloadProgressController extends AbstractController {
    @FXML
    private Label percentLabel;
    @FXML
    private Label rateLabel;
    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button nextButton;

    private URL m_downloadUrl;
    private Thread m_downloadThread;
    private final Logger m_logger = LogManager.getLogger();

    public DownloadProgressController() {
        // Do not add to the back stack. The next series of screens are just progress screens as we download and
        // create the JRE, so there's nothing the user can do. The next logical back step for the user is to the
        // browser page
        super(false, Arguments.Controller.DOWNLOAD_PROGRESS_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        String urlString = m_args.getArgument(Arguments.Argument.JRE_CREATOR_URL);
        if (urlString == null) {
            m_logger.error("Received null string for the url! Exiting with error.");
            showErrorScreen(new IllegalArgumentException("Url String was null!"));
            return;
        }

        try {
            m_downloadUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            m_logger.error("Received malformed url " + urlString, e);
            showErrorScreen(e);
            return;
        }

        m_downloadThread = new Thread(new Downloader());
        m_downloadThread.setDaemon(true);
        m_downloadThread.start();
    }

    @FXML
    @Override
    protected void handleBack(ActionEvent event) {
        // We need to override the original handler to ensure that the download thread is interrupted
        m_downloadThread.interrupt();
        super.handleBack(event);
    }

    @FXML
    @Override
    protected void handleCancel(ActionEvent event) {
        // Just like in handleBack, we need to interrupt the download thread
        m_downloadThread.interrupt();
        super.handleCancel(event);
    }

    /**
     * Actually runs the download, and reports progress back to the UI
     */
    private class Downloader implements Runnable {
        private static final int DEFAULT_BUFFER_SIZE = 1024;

        private HttpURLConnection downloadConnection;
        private BufferedInputStream downloadStream;
        private BufferedOutputStream outputStream;

        @Override
        public void run() {
            try {
                // Get the location of the current directory and the file name
                File currentDirectory = new File(".");
                String fileName = m_downloadUrl.getPath().substring(m_downloadUrl.getPath().lastIndexOf("/") + 1);
                File jreCreatorFile = new File(currentDirectory.getAbsolutePath() + File.separator + fileName);

                // Set up the streams
                downloadConnection = (HttpURLConnection) m_downloadUrl.openConnection();
                CookieManager handler = (CookieManager) CookieHandler.getDefault();
                CookieStore store = handler.getCookieStore();
                for (HttpCookie cookie : store.getCookies()) {
                    downloadConnection.addRequestProperty(cookie.getName(), cookie.getValue());
                }
                downloadConnection.setInstanceFollowRedirects(true);
                HttpURLConnection.setFollowRedirects(true);
                downloadConnection.connect();

                // Oracle's CDN will redirect us here. Follow any redirects until we're done
                m_logger.debug("Initial url is " + downloadConnection.getURL());
                int status = downloadConnection.getResponseCode();
                while (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String redirectUrl = downloadConnection.getHeaderField("Location");
                    String cookies = downloadConnection.getHeaderField("Set-Cookie");
                    m_logger.debug("Redirecting to " + redirectUrl);
                    downloadConnection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    downloadConnection.setRequestProperty("Cookie", cookies);
                    downloadConnection.connect();
                    status = downloadConnection.getResponseCode();
                }
                m_logger.debug("Final url is " + downloadConnection.getURL());

                downloadStream = new BufferedInputStream(downloadConnection.getInputStream());
                outputStream = new BufferedOutputStream(new FileOutputStream(jreCreatorFile));

                // Loop until the file is downloaded
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int sizeOfChunk;
                int downloaded = 0;
                int rateCount = 0;
                int rateDownload = 0;
                final int fileSize = downloadConnection.getContentLength();
                long startTime = System.currentTimeMillis();
                m_logger.debug("Starting JRE Creator download. File size is " + fileSize + " bytes");
                while ((sizeOfChunk = downloadStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, sizeOfChunk);
                    long endTime = System.currentTimeMillis();
                    downloaded += sizeOfChunk;
                    rateDownload += sizeOfChunk;
                    final double percent = downloaded / (double) fileSize;
                    final long timeTaken = endTime - startTime;
                    final boolean updateRate = (++rateCount % 1000) == 0;
                    final int finalChunkSize = rateDownload;
                    Platform.runLater(() -> {
                        progressBar.setProgress(percent);
                        percentLabel.setText((int) (percent * 100) + "%");
                        if (updateRate) {
                            rateLabel.setText((int) (finalChunkSize / (double) timeTaken) + " kB/s");
                        }
                    });
                    if (updateRate) {
                        rateDownload = 0;
                        startTime = System.currentTimeMillis();
                    }
                }

                m_logger.debug("Downloaded JRE");
                outputStream.flush();
                outputStream.close();
                m_args.setArgument(Arguments.Argument.JRE_CREATOR_TAR, jreCreatorFile.getAbsolutePath());
            } catch (IOException e) {
                m_logger.error("Could not download the JRE", e);
                Platform.runLater(() -> showErrorScreen(e));
            } finally {
                // Ensure streams are closed
                if (downloadStream != null) {
                    try {
                        downloadStream.close();
                    } catch (IOException e) {
                        m_logger.warn("Error when closing the download stream", e);
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        m_logger.warn("Error when closing the output stream", e);
                    }
                }

                final boolean interrupted = Thread.interrupted();

                // Move to the new window
                Platform.runLater(() -> {
                    if (!interrupted) {
                        moveNext(Arguments.Controller.UNTAR_CONTROLLER);
                    }
                });
            }
        }
    }
}
