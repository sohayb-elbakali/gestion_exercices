package utils;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to manage application icons
 */
public class IconHelper {
    private static final Logger LOGGER = Logger.getLogger(IconHelper.class.getName());
    private static final String ICON_PATH = "/images/education.png";
    
    /**
     * Sets the application icon for a stage
     * 
     * @param stage The stage to set the icon for
     */
    public static void setStageIcon(Stage stage) {
        try {
            Image icon = new Image(IconHelper.class.getResourceAsStream(ICON_PATH));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set application icon", e);
        }
    }
    
    /**
     * Sets the application icon for a dialog
     * 
     * @param alert The alert dialog to set the icon for
     */
    public static void setDialogIcon(Alert alert) {
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            setStageIcon(stage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set dialog icon", e);
        }
    }
    
    /**
     * Sets the application icon for all open windows
     */
    public static void setIconForAllStages() {
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage) {
                setStageIcon((Stage) window);
            }
        }
    }
} 
