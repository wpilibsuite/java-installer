package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Shows the welcome screen
 */
public class WelcomeController extends AbstractController {

    @FXML
    private ImageView logoImageView;

    public WelcomeController() {
        super(true, Arguments.Controller.WELCOME_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    public void handleNext(ActionEvent event) {
        moveNext(Arguments.Controller.INTERNET_CONTROLLER);
    }
}
