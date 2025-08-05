package com.cryptography.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField userId;
//    @FXML private TextField password;

    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> {
            String id = userId.getText();
            if (!id.isEmpty()) {
                openChatWindow(id);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.close();
            }
        });
    }

    private void openChatWindow(String id) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cryptography/frontend/chat.fxml"));
            Parent root = loader.load();

            ChatController chatController = loader.getController();
            chatController.init(id);

            Stage stage = new Stage();
            stage.setTitle("Чат - пользователь: " + id);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
