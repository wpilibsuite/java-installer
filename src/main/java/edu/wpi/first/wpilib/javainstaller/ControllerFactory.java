package edu.wpi.first.wpilib.javainstaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import edu.wpi.first.wpilib.javainstaller.controllers.AbstractController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Factory for creating new controllers. This handles the creation and initialization of the
 * controllers, and returns the new root to the caller
 */
public final class ControllerFactory {

    private static final ControllerFactory instance = new ControllerFactory();

    /**
     * Gets the static singleton factory instance. You cannot make a new instance of the factory
     *
     * @return The single factory instance
     */
    public static ControllerFactory getInstance() {
        return instance;
    }

    private final Logger m_logger = LogManager.getLogger();

    /**
     * Private constructor so no one can instantiate it.
     */
    private ControllerFactory() {
    }

    /**
     * Initializes a new controller based on a given string, and initializes the new controller with
     * the given list of intents. This does not add the original controller to the backstack.
     *
     * @param controllerName The controller to initialize
     * @param args           The arguments to give the new controller
     * @return The view controlled by the requested controller
     * @throws java.lang.IllegalArgumentException If the given controller does not exist
     * @throws java.io.IOException                If there is an error loading the given controller
     */
    public Parent initializeController(Arguments.Controller controllerName, Arguments args) throws IllegalArgumentException, IOException {
        m_logger.debug("Loading controller " + controllerName);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(controllerName.getPath()));
        Parent root = loader.load();
        AbstractController controller = loader.getController();
        controller.initialize(args);
        return root;
    }
}
