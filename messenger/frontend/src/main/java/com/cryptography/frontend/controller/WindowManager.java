package com.cryptography.frontend.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WindowManager {

    public static <T> T openWindow(String fxmlPath, String title, Stage currentStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();

        return loader.getController();
    }
}
