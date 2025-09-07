package com.cryptography.frontend.controller;

import com.cryptography.frontend.apiclient.AuthClient;
import com.cryptography.frontend.dto.AuthRequest;
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

@Slf4j
public class RegisterController {
    @FXML
    TextField userName;

    @FXML
    PasswordField password;

    @FXML
    PasswordField repeatPassword;

    @FXML
    Button registrationButton;

    @FXML
    public void initialize() {
        String username = userName.getText();
        String pass = password.getText();
        String repeatPass = repeatPassword.getText();

        if  (username.isEmpty() || pass.isEmpty() || repeatPass.isEmpty()) {
            showAlert(ERR, "Все поля должны быть заполнены!");
            return;
        }

        if (!pass.equals(repeatPass)) {
            showAlert(ERR, "Пароли не совпадают!");
            return;
        }

        try {
            AuthRequest request = new AuthRequest(username, pass);
            AuthClient.register(request);
            showAlert(SUCCESS, "Регистрация прошла успешно! Теперь войдите в систему.");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cryptography/frontend/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registrationButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login Page");
            stage.show();
        } catch (Exception e) {
            log.error(e.getMessage());
            showAlert(ERR, "Не удалось зарегистрироваться: " + e.getMessage());
        }
    }

}
