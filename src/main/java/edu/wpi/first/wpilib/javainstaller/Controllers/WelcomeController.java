package edu.wpi.first.wpilib.javainstaller.Controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * Created by fred on 11/21/14.
 */
public class WelcomeController {

    @FXML
    private Pane frcImagePane;

    @FXML
    public void initialize() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        ImageView frcImageView = new ImageView(frcImage);
        frcImageView.setPreserveRatio(true);
        frcImageView.setFitWidth(frcImagePane.getWidth() / 2);
        frcImagePane.getChildren().add(frcImageView);
    }
}
