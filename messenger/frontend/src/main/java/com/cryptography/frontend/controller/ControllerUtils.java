package com.cryptography.frontend.controller;

import javafx.scene.control.Alert;

public class ControllerUtils {

    static final String ERR = "Ошибка";
    static final String SUCCESS = "Успех";

    static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
