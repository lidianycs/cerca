package com.cerca;

import com.cerca.controller.MainController;
import com.cerca.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize MVC Components
        MainView view = new MainView();
        new MainController(view); 
        
    
        var iconStream = getClass().getResourceAsStream("/images/app_icon.png");
       
        
        if (iconStream != null) {
            Image icon = new Image(iconStream);
            stage.getIcons().add(icon);
        } else {
            System.out.println("⚠️ Icon file not found in resources!");
        }

        Scene scene = new Scene(view.getView(), 1100, 650);
        stage.setTitle("Cerca - Citation Extraction & Reference Checking Assistant");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}