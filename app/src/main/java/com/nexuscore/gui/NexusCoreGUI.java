package com.nexuscore.gui;

import com.nexuscore.gui.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main GUI application class for Nexus Core
 */
public class NexusCoreGUI extends Application {

    private MainController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            // Get controller reference for later access
            controller = loader.getController();

            // Load CSS
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Configure stage
            primaryStage.setTitle("Nexus Core");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Set up close handler
            primaryStage.setOnCloseRequest(e -> {
                if (controller != null) {
                    controller.cleanupResources();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Static launch method to start the JavaFX application
     * 
     * @param appClass The application class
     * @param args     Command line arguments
     */
    public static void launch(Class<? extends Application> appClass, String[] args) {
        Application.launch(appClass, args);
    }
}