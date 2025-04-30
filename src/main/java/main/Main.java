package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.IconHelper;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        // Set the application title
        primaryStage.setTitle("Education Management System");
        
        // Set the application icon using the helper class
        IconHelper.setStageIcon(primaryStage);
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
