package com.cryptography.frontend.controller;

import com.cryptography.frontend.apiclient.AuthClient;
import com.cryptography.frontend.context.SessionManager;
import com.cryptography.frontend.dto.AuthRequest;
import com.cryptography.frontend.dto.AuthResponse;
import com.cryptography.frontend.stompclient.StompClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import static com.cryptography.frontend.controller.ControllerUtils.*;
import static com.cryptography.frontend.controller.WindowManager.openWindow;

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
                SessionManager.getInstance().setUserId(id);
                SessionManager.getInstance().setToken(response.getToken());

                FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource("/com/cryptography/frontend/chat.fxml"));
                Parent root = loader.load();

                ChatController chatController = loader.getController();
                chatController.init(id, name);

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Чат - пользователь: " + name);
            } catch (Exception e) {
                log.error(e.getMessage());
                showAlert(ERR, "Не удалось выполнить вход: " + e.getMessage());
            }
        });

        registrationPageButton.setOnAction(event -> {
            try {
                openWindow("/com/cryptography/frontend/registration.fxml", "Registration Page", (Stage) registrationPageButton.getScene().getWindow());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }
}
