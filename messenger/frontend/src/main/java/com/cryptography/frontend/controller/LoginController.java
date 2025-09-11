package com.cryptography.frontend.controller;

import com.cryptography.frontend.apiclient.AuthClient;
import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.AuthRequest;
import com.cryptography.frontend.dto.AuthResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static com.cryptography.frontend.controller.ControllerUtils.*;

@Slf4j
public class LoginController {
    @FXML private TextField userName;
    @FXML private PasswordField password;

    @FXML private Button loginButton;
    @FXML private Button registrationPageButton;

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> {
            String name = userName.getText();
            String pass = password.getText();

            try {
                AuthResponse response = AuthClient.login(new AuthRequest(name, pass));
                String id = response.getUserId();
                SessionManager.getInstance().setToken(id, response.getToken());
                openChatWindow(id);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.close();
            } catch (Exception e) {
                log.error(e.getMessage());
                showAlert(ERR, "Не удалось выполнить вход: " + e.getMessage());
            }
        });

        registrationPageButton.setOnAction(event -> {
            openRegistrationWindow();
        });
    }

    private void openRegistrationWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cryptography/frontend/registration.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registrationPageButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registration Page");
            stage.show();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
            log.error("Ошибка загрузки чатов: {}", e.getMessage());
        }
    }
}
