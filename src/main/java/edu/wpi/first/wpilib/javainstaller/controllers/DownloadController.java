package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static edu.wpi.first.wpilib.javainstaller.controllers.DownloadController.HelpStep.*;

/**
 * Walks the user through downloading the JRE
 */
public class DownloadController extends AbstractController {
    private static final String[] instructionStrings = new String[]{
            "On this page, first click Accept License Agreement under OTN License Agreement",
            "Next, download the ARMv7 Linux - VFP, SoftFP ABI, Little Endian JRE. This is the second link in the first download box",
            "You need an Oracle account to download the JRE. If you already have one, you can sign in, and the download will start. Otherwise, please create an account.",
            "After you have created an account, you will be brought back to the sign in page. After you sign in, the download will begin.",
            "Having trouble? Restart."
    };

    enum HelpStep {
        ACCEPT,
        DOWNLOAD,
        SIGN_UP,
        SIGN_IN,
        RESTART;

        private static int modSub(int old, int sub, int mod) {
            int n = (old - sub) % mod;
            if (n < 0) {
                n += mod;
            }
            return n;
        }

        public HelpStep getPrevious() {
            return this.values()[modSub(this.ordinal(), 1, this.values().length)];
        }

        public HelpStep getNext() {
            return this.values()[(this.ordinal() + 1) % this.values().length];
        }

        public String getInstruction() {
            return instructionStrings[ordinal()];
        }
    }

    public static final URL JRE_URL;
    public static final String JRE_URL_STRING = "http://www.oracle.com/technetwork/java/embedded/embedded-se/downloads/javaseembedded8u6-2406243.html";
    public static final String JRE_LOGIN_STRING = "login.oracle.com/mysso/signon.jsp";
    public static final String JRE_ACCOUNT_CREATE_PAGE = "profile.oracle.com/myprofile/account/create-account.jspx";
    public static final String JRE_HFLT_IDENTIFIER = "hflt";

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

    private final String incorrectVersionString = "You appear to be downloading the HardFP JRE, not the SoftFP JRE." +
    " The HardFP JRE does not run correctly on the roboRIO." +
    " Please download the SoftFP version, the second download link on the page.";

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
    private HelpStep currentInstruction = ACCEPT;
    private final Logger m_logger = LogManager.getLogger();

    public DownloadController() {
        super(true, Arguments.Controller.DOWNLOAD_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        browserEngine = browserView.getEngine();

        // Set up the download extension listener
        browserEngine.locationProperty().addListener((value, oldLock, newLoc) -> {
            if (newLoc.contains(JRE_HFLT_IDENTIFIER)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Incorrect JRE");
                alert.setContentText(incorrectVersionString);
                alert.showAndWait().ifPresent(button -> Platform.runLater(this::restartDownload));
            }
            if (signedIn && newLoc.endsWith("tar.gz")) {
                moveToNext(newLoc);
            }
        });

        // Set up the listener for switching the help text based on the page
        browserEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            String location = browserEngine.getLocation();
            if (location.equals(JRE_URL_STRING)) {
                m_logger.debug("Loading download page page");
                Platform.runLater(() -> {
                    currentInstruction = ACCEPT;
                    setInstructionText();
                });
            } else if (location.contains(JRE_LOGIN_STRING)) {
                signedIn = true;
                m_logger.debug("Loading login page");
                Platform.runLater(() -> {
                    if (!creatingAccount) {
                        currentInstruction = SIGN_IN;
                        setInstructionText();
                    }
                });
            } else if (location.contains(JRE_ACCOUNT_CREATE_PAGE)) {
                creatingAccount = true;
                m_logger.debug("Loading account creation page");
                Platform.runLater(() -> {
                    currentInstruction = SIGN_UP;
                    setInstructionText();
                });
            } else {
                m_logger.debug("Loading unknown page");
                Platform.runLater(() -> {
                    currentInstruction = RESTART;
                    setInstructionText();
                });
            }
        });
        browserEngine.load(JRE_URL_STRING);
    }

    private void moveToNext(String url) {
        Platform.runLater(() -> {
            m_logger.debug("Signed in and have the .tar.gz link.");
            m_args.setArgument(Arguments.Argument.JRE_CREATOR_URL, url);
            moveNext(Arguments.Controller.DOWNLOAD_PROGRESS_CONTROLLER);
        });
    }

    @FXML
    private void handleInstructionNext(Event event) {
        currentInstruction = currentInstruction.getNext();
        setInstructionText();
    }

    @FXML
    private void handleInstructionBack(Event event) {
        currentInstruction = currentInstruction.getPrevious();
        setInstructionText();
    }

    private void setInstructionText() {
        switch (currentInstruction) {
            case ACCEPT:
                instructions.setText(currentInstruction.getInstruction());
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case DOWNLOAD:
                instructions.setText(currentInstruction.getInstruction());
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case SIGN_UP:
                instructions.setText(currentInstruction.getInstruction());
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case SIGN_IN:
                instructions.setText(currentInstruction.getInstruction());
                instructions.setUnderline(false);
                instructions.setTextFill(Color.BLACK);
                instructions.setOnMouseClicked(null);
                break;
            case RESTART:
            default:
                instructions.setText(currentInstruction.getInstruction());
                instructions.setUnderline(true);
                instructions.setTextFill(Color.BLUE);
                instructions.setOnMouseClicked((mouseEvent) -> {
                    restartDownload();
                });
                break;
        }
        instructions.setTooltip(new Tooltip(currentInstruction.getInstruction()));
    }

    private void restartDownload() {
        m_logger.debug("Restarting login process");
        browserEngine.load(JRE_URL_STRING);
        signedIn = false;
        creatingAccount = false;
    }
}
