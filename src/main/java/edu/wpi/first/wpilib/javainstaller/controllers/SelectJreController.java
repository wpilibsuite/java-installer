package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Created by noahg on 03-Oct-17.
 */
public class SelectJreController extends AbstractController{

    @FXML
    public Button fullJreButton;

    @FXML
    public Button compactJreButton;

    public SelectJreController() {
        super(true, Arguments.Controller.SELECT_JRE_CONTROLLER);
    }

    @Override
    protected void initializeClass() {

    }

    @FXML
    protected void handleFullJre(){
        m_args.setArgument(Arguments.Argument.USE_COMPACT_JRE, "FALSE");
        moveNext(Arguments.Controller.CREATE_JRE_CONTROLLER);
    }

    @FXML
    protected void handleCompactJre(){
        m_args.setArgument(Arguments.Argument.USE_COMPACT_JRE, "TRUE");
        moveNext(Arguments.Controller.CREATE_JRE_CONTROLLER);
    }
}
