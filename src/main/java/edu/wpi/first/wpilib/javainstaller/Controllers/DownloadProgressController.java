package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Starts the download and shows the user the download and extract process progress
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

    private URL downloadUrl;
    private Thread downloadThread;
    private final Logger m_logger = LogManager.getLogger();

    public DownloadProgressController() {
        super("/fxml/download.fxml");
    }

    public void initialize(URL downloadLocation) {
        downloadUrl = downloadLocation;
        downloadThread = new Thread(new Downloader());
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    @FXML
    @Override
    public void handleBack(ActionEvent event) {
        downloadThread.interrupt();
        super.handleBack(event);
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
                String fileName = downloadUrl.getPath().substring(downloadUrl.getPath().lastIndexOf("/") + 1);
                File javaFile = new File(currentDirectory.getAbsolutePath() + File.separator + fileName);

                // Set up the streams
                downloadConnection = (HttpURLConnection) downloadUrl.openConnection();
                System.out.println("Url is " + downloadUrl);
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
                    System.out.println("Redirecting to " + redirectUrl);
                    downloadConnection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    downloadConnection.setRequestProperty("Cookie", cookies);
                    downloadConnection.connect();
                    status = downloadConnection.getResponseCode();
                }
                m_logger.debug("Final url is " + downloadConnection.getURL());

                downloadStream = new BufferedInputStream(downloadConnection.getInputStream());
                outputStream = new BufferedOutputStream(new FileOutputStream(javaFile));


                // Loop until the file is downloaded
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int sizeOfChunk;
                int downloaded = 0;
                int rateCount = 0;
                int rateDownload = 0;
                final int fileSize = downloadConnection.getContentLength();
                long startTime = System.currentTimeMillis();
                m_logger.debug("Starting JRE download. File size is " + fileSize + " bytes");
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

                // Move to the new window
                Platform.runLater(() -> {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/downloaded.fxml"));
                    try {
                        Parent root = loader.load();
                        DownloadedController controller = loader.getController();
                        controller.initialize(javaFile.getAbsolutePath());
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Error when loaded downloaded page", e);
                        MainApp.showErrorScreen(e);
                    }
                });
            } catch (IOException e) {
                m_logger.error("Could not download the JRE", e);
                Platform.runLater(() -> MainApp.showErrorScreen(e));
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
            }
        }
    }
}
