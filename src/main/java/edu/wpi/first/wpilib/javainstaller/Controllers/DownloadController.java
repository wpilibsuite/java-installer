package edu.wpi.first.wpilib.javainstaller.Controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Walks the user through downloading the JRE
 */
public class DownloadController extends AbstractController {

    public static final URL JRE_URL;
    public static final String JRE_URL_STRING = "http://www.oracle.com/technetwork/java/embedded/embedded-se/downloads/javaseembedded8u6-2406243.html";
    public static final String JRE_LOGIN_STRING = "login.oracle.com/mysso/signon.jsp";
    public static final String JRE_ACCOUNT_CREATE_PAGE = "profile.oracle.com/myprofile/account/create-account.jspx";

    // Jump through hoops to take care of a possible MalformedURLException when initializing the JRE url
    static {
        URL temp;
        try {
            temp = new URL(JRE_URL_STRING);
        } catch (MalformedURLException e) {
            temp = null;
            LogManager.getLogger(DownloadController.class)
                    .error("Could not parse the JRE url. Something is seriously messed up. URL was " + JRE_URL_STRING, e);
            Platform.runLater(() -> MainApp.showErrorScreen(e));
        }
        JRE_URL = temp;

    }

    private final String[] instructionStrings = new String[]{
            "On this page, first click Accept License Agreement under OTN License Agreement",
            "Next, download the ARMv7 Linux - VFP, SoftFP ABI, Little Endian JRE. This is the second link in the first download box",
            "You need an Oracle account to download the JRE. If you already have one, you can sign in, and the download will start. Otherwise, please create an account.",
            "After you have created an account, you will be brought back to the sign in page. After you sign in, the download will begin.",
            "Having trouble? Restart."
    };

    @FXML
    private Label instructions;
    @FXML
    private Label instructionsBack;
    @FXML
    private Label instructionsNext;

    @FXML
    private WebView browserView;
    private WebEngine browserEngine;

    private boolean creatingAccount = false;
    private boolean signedIn = false;
    private int currentInstruction = 0;
    private final Logger m_logger = LogManager.getLogger();

    public DownloadController() {
        super("/fxml/connect_internet.fxml");
    }

    @FXML
    private void initialize() {
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        browserEngine = browserView.getEngine();

        // Set up the download extension listener
        browserEngine.locationProperty().addListener((value, oldLock, newLoc) -> {

            if (signedIn && newLoc.endsWith("tar.gz")) {
                Platform.runLater(() -> {
                    m_logger.debug("Signed in and have the .tar.gz link.");
                    FXMLLoader loader = new FXMLLoader();
                    try {
                        Parent root = loader.load(getClass().getResource("/fxml/download_progress.fxml").openStream());
                        DownloadProgressController controller = loader.getController();
                        controller.initialize(new URL(newLoc));
                        mainView.getScene().setRoot(root);
                    } catch (IOException e) {
                        m_logger.error("Could not display the downloadprogress page", e);
                        MainApp.showErrorScreen(e);
                    }
                });
            }
        });

        // Set up the listener for switching the help text based on the page
        browserEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            String location = browserEngine.getLocation();
            if (location.equals(JRE_URL_STRING)) {
                m_logger.debug("Loading download page page");
                Platform.runLater(() -> {
                    currentInstruction = 0;
                    setInstructionText();
                });
            } else if (location.contains(JRE_LOGIN_STRING)) {
                signedIn = true;
                m_logger.debug("Loading login page");
                Platform.runLater(() -> {
                    if (!creatingAccount) {
                        currentInstruction = 2;
                        setInstructionText();
                    }
                });
            } else if (location.contains(JRE_ACCOUNT_CREATE_PAGE)) {
                creatingAccount = true;
                m_logger.debug("Loading account creation page");
                Platform.runLater(() -> {
                    currentInstruction = 3;
                    setInstructionText();
                });
            } else {
                m_logger.debug("Loading unknown page");
                Platform.runLater(() -> {
                    currentInstruction = 4;
                    setInstructionText();
                });
            }
        });
        browserEngine.load(JRE_URL_STRING);
    }

    @FXML
    private void handleInstructionNext(Event event) {
        currentInstruction++;
        setInstructionText();
    }

    @FXML
    private void handleInstructionBack(Event event) {
        currentInstruction--;
        setInstructionText();
    }

    private void setInstructionText() {
        currentInstruction = currentInstruction % instructionStrings.length;
        switch (currentInstruction) {
            case 0:
                instructions.setText(instructionStrings[currentInstruction]);
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case 1:
                instructions.setText(instructionStrings[currentInstruction]);
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case 2:
                instructions.setText(instructionStrings[currentInstruction]);
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case 3:
                instructions.setText(instructionStrings[currentInstruction]);
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case 4:
            default:
                instructions.setText(instructionStrings[currentInstruction]);
                instructions.setUnderline(true);
                instructions.setTextFill(Color.BLUE);
                instructions.setOnMouseClicked((mouseEvent) -> {
                    m_logger.debug("Restarting login process");
                    browserEngine.load(JRE_URL_STRING);
                    signedIn = false;
                    creatingAccount = false;
                });
                break;
        }
        instructions.setTooltip(new Tooltip(instructionStrings[currentInstruction]));
    }
}
